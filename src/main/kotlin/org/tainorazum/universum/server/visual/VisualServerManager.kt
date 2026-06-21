package org.tainorazum.universum.server.visual

import net.kyori.adventure.text.Component
import org.tainorazum.universum.Universum
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.tainorazum.universum.utility.ComponentSerializer
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

class VisualServerManager {

    fun setMOTD(identifier: String, motd: Component): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_VisualServer.upsert {
                    it[DB_VisualServer.identifier] = identifier
                    it[DB_VisualServer.motd] = ComponentSerializer.serialize(motd)
                }
                true
            }
        }
    }

    fun getMOTD(identifier: String): CompletableFuture<Component?> {
        return dba {
            transaction {
                DB_VisualServer.selectAll()
                    .where { DB_VisualServer.identifier eq identifier }
                    .map { ComponentSerializer.deserialize(it[DB_VisualServer.motd]) }
                    .singleOrNull()
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
                true
            }
        }
    }

    fun getFavicon(identifier: String): CompletableFuture<ByteArray?> {
        return dba {
            transaction {
                DB_VisualServer.selectAll()
                    .where { DB_VisualServer.identifier eq identifier }
                    .map { row -> row[DB_VisualServer.favicon]?.bytes }
                    .singleOrNull()
            }
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