package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.tainorazum.universum.Universum
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

object RedisManager {
    private var client: RedisClient? = null
    private var conn: StatefulRedisConnection<String, String>? = null

    /**
     * Checks if the connection is currently open and ready to accept commands.
     */
    fun isConnected(): Boolean {
        return conn?.isOpen == true
    }

    @Synchronized
    fun init(uri: String = Universum.instance.config.getString("redis.uri") ?: "redis://localhost:6379"): Boolean {
        if (isConnected()) return true

        return try {
            client = RedisClient.create(uri)
            conn = client?.connect()
            val connected = conn?.isOpen == true
            if (connected) {
                Universum.instance.logger.info("[RedisManager] Successfully connected to Redis!")
            } else {
                Universum.instance.logger.severe("[RedisManager] Failed to establish Redis connection.")
            }
            connected
        } catch (e: Exception) {
            Universum.instance.logger.log(Level.SEVERE, "[RedisManager] Exception initializing Redis connection:", e)
            close()
            false
        }
    }

    fun close() {
        try {
            conn?.close()
            client?.shutdown()
        } catch (e: Exception) {
            Universum.instance.logger.log(Level.WARNING, "[RedisManager] Error closing Redis connection:", e)
        } finally {
            conn = null
            client = null
        }
    }

    private fun connection(): StatefulRedisConnection<String, String>? {
        val currentConn = conn
        if (currentConn == null || !currentConn.isOpen) {
            Universum.instance.logger.warning("[RedisManager] Attempted operation while connection is uninitialized or closed.")
            return null
        }
        return currentConn
    }

    // --- NON-BLOCKING KEY/VALUE ---

    fun set(key: String, value: String, ttlSeconds: Long? = null): CompletableFuture<Boolean> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(false)
        val async = activeConn.async()

        return if (ttlSeconds != null) {
            async.setex(key, ttlSeconds, value).thenApply { it == "OK" }.toCompletableFuture()
        } else {
            async.set(key, value).thenApply { it == "OK" }.toCompletableFuture()
        }
    }

    fun get(key: String): CompletableFuture<String?> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(null)
        return activeConn.async().get(key).toCompletableFuture()
    }

    fun del(key: String): CompletableFuture<Long> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(0L)
        return activeConn.async().del(key).thenApply { it.toLong() }.toCompletableFuture()
    }

    fun publish(channel: String, message: String): CompletableFuture<Long> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(0L)
        return activeConn.async().publish(channel, message).thenApply { it.toLong() }.toCompletableFuture()
    }

    // --- REDIS HASHES ---

    fun hset(key: String, field: String, value: String): CompletableFuture<Boolean> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(false)
        return activeConn.async().hset(key, field, value).toCompletableFuture()
    }

    fun hget(key: String, field: String): CompletableFuture<String?> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(null)
        return activeConn.async().hget(key, field).toCompletableFuture()
    }

    fun hdel(key: String, field: String): CompletableFuture<Long> {
        val activeConn = connection() ?: return CompletableFuture.completedFuture(0L)
        return activeConn.async().hdel(key, field).thenApply { it.toLong() }.toCompletableFuture()
    }

    // --- UTILS ---

    fun generateRequestId(): String = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
}