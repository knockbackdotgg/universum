package org.tainorazum.universum.server.update

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.json.JSONObject
import org.tainorazum.universum.Universum
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.utility.text.Colours
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class Updater {

    companion object {
        private const val TARGET_FILE = "universum-all.jar"
        private const val USER_AGENT = "PluginUpdater"

        private val GITHUB_REPO by lazy { Universum.instance.config.getString("github.repo") }
        private val GITHUB_TOKEN by lazy { Universum.instance.config.getString("github.token") }
    }

    fun update() {
        if (!Universum.instance.config.getBoolean("github.auto-update")) {
            Bukkit.getLogger().info("Not auto-updating.")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Universum.instance, Runnable {
            Bukkit.getLogger().info("Attempting to auto-update (Async)...")
            try {
                val assets = fetchLatestAssets()
                val needsRestart = downloadAndReplacePlugin(assets)

                if (needsRestart) {
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        NetworkPlayerManager.broadcast(
                            Component.empty()
                                .append(Component.newline().append(Component.text("Server: ${Universum.server.identifier.uppercase().replace('_', ' ')} is updating to the latest version.", Colours.GREEN)))
                                .append(Component.newline())
                        )
                        Bukkit.restart()
                    })
                }
            } catch (e: Exception) {
                Bukkit.getLogger().warning("Failed to update plugin: ${e.message}")
            }
        })
    }

    @Throws(Exception::class)
    private fun fetchLatestAssets(): Map<String, String> {
        val repoUrl = GITHUB_REPO ?: throw IllegalStateException("GitHub repo URL is not configured.")

        val jsonText = executeHttpRequest(repoUrl) { input ->
            input.bufferedReader().use { it.readText() }
        }

        val response = JSONObject(jsonText)
        val assetsArray = response.getJSONArray("assets")

        // Pre-size the map to prevent dynamic array resizes during iteration
        val assetMap = HashMap<String, String>(assetsArray.length())

        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            assetMap[asset.getString("name")] = asset.getString("url")
        }

        return assetMap
    }

    @Throws(Exception::class)
    private fun computeSHA256(filePath: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")

        // Use standard buffer size and direct NIO stream reading
        Files.newInputStream(filePath).use { fis ->
            val buffer = ByteArray(8192) // 8KB buffer for faster hashing
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Throws(Exception::class)
    private fun downloadAndReplacePlugin(assets: Map<String, String>): Boolean {
        val pluginsDir = Universum.instance.dataFolder.parentFile.toPath()
        val pluginPath: Path = pluginsDir.resolve(TARGET_FILE)

        val jarUrl = assets[TARGET_FILE] ?: throw Exception("JAR asset not found")
        val hashUrl = assets["$TARGET_FILE.sha256"] ?: throw Exception("Hash asset not found")

        val remoteHash = fetchRemoteHash(hashUrl)
        val localHash = if (Files.exists(pluginPath)) computeSHA256(pluginPath) else ""

        if (remoteHash.equalsIgnoreCase(localHash)) {
            Bukkit.getLogger().info("Plugin is already up to date (SHA-256 match).")
            return false
        }

        Bukkit.getLogger().info("New version detected. Downloading update...")
        val tempPath: Path = pluginsDir.resolve("$TARGET_FILE.tmp")

        downloadFile(jarUrl, tempPath)

        try {
            Files.move(
                tempPath,
                pluginPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            Files.move(
                tempPath,
                pluginPath,
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        Bukkit.getLogger().info("Update complete. New hash: ${remoteHash.take(7)}")
        return true
    }

    @Throws(Exception::class)
    private fun fetchRemoteHash(urlStr: String): String {
        return executeHttpRequest(urlStr, acceptOctetStream = true) { input ->
            input.bufferedReader().use { it.readText().trim() }
        }
    }

    @Throws(Exception::class)
    private fun downloadFile(urlStr: String, targetPath: Path) {
        executeHttpRequest(urlStr, acceptOctetStream = true) { input ->
            Files.newOutputStream(targetPath).use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Helper method to centralize HTTP connection boilerplate, headers, and error checking.
     */
    @Throws(Exception::class)
    private fun <T> executeHttpRequest(
        urlStr: String,
        acceptOctetStream: Boolean = false,
        block: (InputStream) -> T
    ): T {
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 10000
            setRequestProperty("User-Agent", USER_AGENT)

            GITHUB_TOKEN?.takeIf { it.isNotBlank() }?.let { token ->
                setRequestProperty("Authorization", "token $token")
            }

            if (acceptOctetStream) {
                setRequestProperty("Accept", "application/octet-stream")
            }
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Failed connection to $urlStr: HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use(block)
        } finally {
            connection.disconnect()
        }
    }

    private fun String.equalsIgnoreCase(other: String): Boolean = this.equals(other, ignoreCase = true)
}