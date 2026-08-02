package org.tainorazum.universum.server.visual

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.setting.ServerSetting
import org.tainorazum.universum.utility.text.ComponentSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

class VisualServerManager {

    // Separate atomic caches prevent partial miss/invalidation conflicts
    private val motdCache = ConcurrentHashMap<String, Component>()
    private val faviconCache = ConcurrentHashMap<String, ByteArray>()

    /**
     * Parses raw text from tools like RGB Birdflop into a Component.
     */
    fun parseMOTDString(input: String): Component {
        var formatted = input.replace(Regex("&#([A-Fa-f0-9]{6})")) { match ->
            "<#${match.groupValues[1]}>"
        }

        formatted = formatted.replace(Regex("&([0-9a-fk-orA-FK-OR])")) { match ->
            when (val code = match.groupValues[1].lowercase()) {
                "0" -> "<black>"
                "1" -> "<dark_blue>"
                "2" -> "<dark_green>"
                "3" -> "<dark_aqua>"
                "4" -> "<dark_red>"
                "5" -> "<dark_purple>"
                "6" -> "<gold>"
                "7" -> "<gray>"
                "8" -> "<dark_gray>"
                "9" -> "<blue>"
                "a" -> "<green>"
                "b" -> "<aqua>"
                "c" -> "<red>"
                "d" -> "<light_purple>"
                "e" -> "<yellow>"
                "f" -> "<white>"
                "k" -> "<obfuscated>"
                "l" -> "<bold>"
                "m" -> "<strikethrough>"
                "n" -> "<underlined>"
                "o" -> "<italic>"
                "r" -> "<reset>"
                else -> match.value
            }
        }

        return try {
            MiniMessage.miniMessage().deserialize(formatted)
        } catch (_: Exception) {
            LegacyComponentSerializer.legacySection().deserialize(input)
        }
    }

    /**
     * Overloaded setMOTD that accepts a raw String (useful directly from commands).
     */
    fun setMOTD(identifier: String, rawMotd: String): CompletableFuture<Boolean> {
        val component = parseMOTDString(rawMotd)
        return setMOTD(identifier, component)
    }

    fun setCurrentIdentifier(identifier: String): CompletableFuture<Boolean> {
        return DB_ServerSettings.set(ServerSetting.CURRENT_MOTD, identifier)
    }

    /**
     * Updates/Inserts the MOTD asynchronously into DB and updates in-memory cache.
     */
    fun setMOTD(identifier: String, motd: Component): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val serialized = ComponentSerializer.serialize(motd)
            transaction {
                DB_VisualServer.upsert {
                    it[DB_VisualServer.identifier] = identifier
                    it[DB_VisualServer.motd] = serialized
                }
            }
            // Update cache immediately on save
            motdCache[identifier] = motd
            true
        }
    }

    /**
     * Retrieves the MOTD asynchronously (Non-blocking cache check -> DB fallback).
     */
    fun getMOTD(identifier: String): CompletableFuture<Component?> {
        val cached = motdCache[identifier]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        return CompletableFuture.supplyAsync {
            transaction {
                val row = DB_VisualServer
                    .slice(DB_VisualServer.motd)
                    .select { DB_VisualServer.identifier eq identifier }
                    .singleOrNull()

                val motd = row?.get(DB_VisualServer.motd)?.let { ComponentSerializer.deserialize(it) }
                if (motd != null) {
                    motdCache[identifier] = motd
                }
                motd
            }
        }
    }

    /**
     * Downloads image, validates dimensions, saves to DB, and updates cache asynchronously.
     */
    fun setFavicon(identifier: String, url: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val bytes = fetchUrlBytes(url)
                ?: throw IllegalArgumentException("Failed to fetch favicon from URL: $url")

            if (!isValidFavicon(bytes)) {
                throw IllegalArgumentException("Favicon must be a valid 64x64 PNG image!")
            }

            val blob = ExposedBlob(bytes)
            transaction {
                DB_VisualServer.upsert {
                    it[DB_VisualServer.identifier] = identifier
                    it[DB_VisualServer.favicon] = blob
                }
            }

            // Update cache immediately on save
            faviconCache[identifier] = bytes
            true
        }
    }

    /**
     * Retrieves the Favicon bytes asynchronously (Non-blocking cache check -> DB fallback).
     */
    fun getFavicon(identifier: String): CompletableFuture<ByteArray?> {
        val cached = faviconCache[identifier]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        return CompletableFuture.supplyAsync {
            transaction {
                val row = DB_VisualServer
                    .slice(DB_VisualServer.favicon)
                    .select { DB_VisualServer.identifier eq identifier }
                    .singleOrNull()

                val bytes = row?.get(DB_VisualServer.favicon)?.bytes
                if (bytes != null) {
                    faviconCache[identifier] = bytes
                }
                bytes
            }
        }
    }

    /**
     * Invalidate cache for a specific identifier or all if identifier is null.
     */
    fun invalidateCache(identifier: String? = null) {
        if (identifier == null) {
            motdCache.clear()
            faviconCache.clear()
        } else {
            motdCache.remove(identifier)
            faviconCache.remove(identifier)
        }
    }

    private fun isValidFavicon(bytes: ByteArray): Boolean {
        return try {
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return false
            image.width == 64 && image.height == 64
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchUrlBytes(urlStr: String, maxBytes: Int = 256 * 1024): ByteArray? {
        try {
            val url = URI(urlStr).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Universum/1.0")
            }
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) return null
            val contentLength = conn.contentLengthLong
            if (contentLength > maxBytes) return null

            conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var read: Int
                var total = 0
                while (input.read(buffer).also { read = it } != -1) {
                    total += read
                    if (total > maxBytes) return null
                    out.write(buffer, 0, read)
                }
                return out.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}