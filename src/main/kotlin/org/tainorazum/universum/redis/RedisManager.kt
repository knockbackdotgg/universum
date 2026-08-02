package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.tainorazum.universum.Universum
import java.util.UUID
import java.util.concurrent.CompletableFuture

object RedisManager {
    private var client: RedisClient? = null
    private var conn: StatefulRedisConnection<String, String>? = null

    @Synchronized
    fun init(uri: String = Universum.instance.config.getString("redis.uri") ?: "redis://localhost:6379"): Boolean {
        if (client != null && conn?.isOpen == true) return true

        client = RedisClient.create(uri)
        conn = client?.connect()
        return conn?.isOpen == true
    }

    fun close() {
        try { conn?.close(); client?.shutdown() } catch (_: Exception) {}
        conn = null
        client = null
    }

    private fun connection(): StatefulRedisConnection<String, String> {
        return conn?.takeIf { it.isOpen } ?: throw IllegalStateException("RedisManager connection is uninitialized or closed.")
    }

    // --- NON-BLOCKING KEY/VALUE ---

    fun set(key: String, value: String, ttlSeconds: Long? = null): CompletableFuture<Boolean> {
        val async = connection().async()
        return if (ttlSeconds != null) {
            async.setex(key, ttlSeconds, value).thenApply { it == "OK" }.toCompletableFuture()
        } else {
            async.set(key, value).thenApply { it == "OK" }.toCompletableFuture()
        }
    }

    fun get(key: String): CompletableFuture<String?> =
        connection().async().get(key).toCompletableFuture()

    fun del(key: String): CompletableFuture<Long> =
        connection().async().del(key).thenApply { it.toLong() }.toCompletableFuture()

    fun publish(channel: String, message: String): CompletableFuture<Long> =
        connection().async().publish(channel, message).thenApply { it.toLong() }.toCompletableFuture()

    // --- REDIS HASHES (Useful for Online Player Tracking) ---

    fun hset(key: String, field: String, value: String): CompletableFuture<Boolean> =
        connection().async().hset(key, field, value).toCompletableFuture()

    fun hget(key: String, field: String): CompletableFuture<String?> =
        connection().async().hget(key, field).toCompletableFuture()

    fun hdel(key: String, field: String): CompletableFuture<Long> =
        connection().async().hdel(key, field).thenApply { it.toLong() }.toCompletableFuture()

    // --- UTILS ---

    fun generateRequestId(): String = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
}