package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.tainorazum.universum.Universum
import java.util.UUID
import java.util.concurrent.CompletableFuture

object RedisManager {
    private lateinit var client: RedisClient
    private lateinit var conn: StatefulRedisConnection<String, String>

    @Synchronized
    fun init(uri: String = Universum.instance.config.getString("redis.uri") ?: "redis://localhost:6379"): Boolean {
        if (this::client.isInitialized && conn.isOpen) return true
        client = RedisClient.create(uri)
        conn = client.connect()
        return conn.isOpen
    }

    fun close() {
        try { conn.close(); client.shutdown() } catch (_: Exception) {}
    }

    private fun ensure() {
        if (!this::client.isInitialized || !conn.isOpen) throw IllegalStateException("RedisManager not initialized.")
    }

    fun set(key: String, value: String, ttlSeconds: Long? = null): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync {
            ensure()
            conn.sync().set(key, value)
            ttlSeconds?.let { conn.sync().expire(key, it) }
            true
        }

    fun get(key: String): CompletableFuture<String?> =
        CompletableFuture.supplyAsync { ensure(); conn.sync().get(key) }

    fun del(key: String): CompletableFuture<Long> =
        CompletableFuture.supplyAsync { ensure(); conn.sync().del(key) }

    fun incr(key: String): CompletableFuture<Long> =
        CompletableFuture.supplyAsync { ensure(); conn.sync().incr(key) }

    fun publish(channel: String, message: String): CompletableFuture<Long> =
        CompletableFuture.supplyAsync { ensure(); conn.sync().publish(channel, message) }

    fun generateRequestId(): String = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
}