package org.tainorazum.universum.server.visual

import net.kyori.adventure.text.Component
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.tainorazum.universum.utility.text.ComponentSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

class VisualServerManager {

    // In-memory cache: identifier -> (motd, favicon)
    private data class VisualCache(
        val motd: Component?,
        val favicon: ByteArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VisualCache) return false
            if (motd != other.motd) return false
            if (favicon != null) {
                if (other.favicon == null) return false
                if (!favicon.contentEquals(other.favicon)) return false
            } else if (other.favicon != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = motd?.hashCode() ?: 0
            result = 31 * result + (favicon?.contentHashCode() ?: 0)
            return result
        }
    }

    private val cache = ConcurrentHashMap<String, VisualCache>()

    fun setMOTD(identifier: String, motd: Component): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_VisualServer.upsert {
                    it[DB_VisualServer.identifier] = identifier
                    it[DB_VisualServer.motd] = ComponentSerializer.serialize(motd)
                }

                // invalidate cache for this identifier
                cache.remove(identifier)
                true
            }
        }
    }

    fun getMOTD(identifier: String): CompletableFuture<Component?> {
        // cache hit
        val cached = cache[identifier]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.motd)
        }

        // cache miss: fetch from DB and cache
        return dba {
            transaction {
                val row = DB_VisualServer.selectAll()
                    .where { DB_VisualServer.identifier eq identifier }
                    .singleOrNull()

                val motd = row?.let { ComponentSerializer.deserialize(it[DB_VisualServer.motd]) }
                val favicon = row?.let { it[DB_VisualServer.favicon]?.bytes }

                // populate cache
                cache[identifier] = VisualCache(motd, favicon)

                motd
            }
        }
    }

    fun setFavicon(identifier: String, url: String): CompletableFuture<Boolean> {
        return dba {
            val bytes = fetchUrlBytes(url) ?: throw IllegalArgumentException("Failed to fetch favicon from url: $url")
            val blob = ExposedBlob(bytes)
            transaction {
                DB_VisualServer.upsert {
                    it[DB_VisualServer.identifier] = identifier
                    it[DB_VisualServer.favicon] = blob
                }

                // invalidate cache for this identifier
                cache.remove(identifier)
                true
            }
        }
    }

    fun getFavicon(identifier: String): CompletableFuture<ByteArray?> {
        // cache hit
        val cached = cache[identifier]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.favicon)
        }

        // cache miss: fetch from DB and cache
        return dba {
            transaction {
                val row = DB_VisualServer.selectAll()
                    .where { DB_VisualServer.identifier eq identifier }
                    .singleOrNull()

                val motd = row?.let { ComponentSerializer.deserialize(it[DB_VisualServer.motd]) }
                val favicon = row?.let { it[DB_VisualServer.favicon]?.bytes }

                // populate cache
                cache[identifier] = VisualCache(motd, favicon)

                favicon
            }
        }
    }

    /**
     * Invalidate cache for a specific identifier or all if identifier is null.
     */
    fun invalidateCache(identifier: String? = null) {
        if (identifier == null) {
            cache.clear()
        } else {
            cache.remove(identifier)
        }
    }

    private fun fetchUrlBytes(urlStr: String, maxBytes: Int = 256 * 1024): ByteArray? {
        try {
            val url = URL(urlStr)
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