package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.listeners.RM_GameRequest
import org.tainorazum.universum.redis.listeners.RM_Online
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool

object RedisSubscriber {

    private var client: RedisClient? = null
    private var sub: StatefulRedisPubSubConnection<String, String>? = null
    private var listener: RedisPubSubListener<String, String>? = null
    private var started = false

    // ConcurrentHashMap to safely handle async registrations
    private val handlers = ConcurrentHashMap<String, (String) -> Unit>()

    fun register(channel: String, handler: (String) -> Unit) {
        handlers[channel] = handler
        // If already started, dynamically subscribe to the new channel
        if (started) {
            sub?.async()?.subscribe(channel)
        }
    }

    fun start(
        plugin: JavaPlugin = Universum.instance,
        uri: String = Universum.instance.config.getString("redis.uri") ?: "redis://localhost:6379"
    ) {
        if (started) return

        client = RedisClient.create(uri)
        sub = client!!.connectPubSub()

        registerListeners()

        val localListener = object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (!plugin.isEnabled) return

                // Process message handling asynchronously off the main thread
                ForkJoinPool.commonPool().execute {
                    handlers[channel]?.invoke(message)
                }
            }

            override fun message(pattern: String?, channel: String?, message: String?) {}
            override fun subscribed(channel: String?, count: Long) {}
            override fun unsubscribed(channel: String?, count: Long) {}
            override fun psubscribed(pattern: String?, count: Long) {}
            override fun punsubscribed(pattern: String?, count: Long) {}
        }

        sub!!.addListener(localListener)
        listener = localListener
        started = true

        if (handlers.isNotEmpty()) {
            sub!!.async().subscribe(*handlers.keys.toTypedArray())
        }
    }

    fun stop() {
        if (!started) return
        started = false
        try { listener?.let { sub?.removeListener(it) } } catch (_: Exception) {}
        try { if (sub?.isOpen == true) sub?.async()?.unsubscribe() } catch (_: Exception) {}
        try { sub?.close(); client?.shutdown() } catch (_: Exception) {}
        listener = null
        sub = null
        client = null
        handlers.clear()
    }

    private fun registerListeners() {
        RM_GameRequest()
        RM_Online()
    }
}