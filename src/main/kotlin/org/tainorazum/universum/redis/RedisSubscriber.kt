package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.Universum

object RedisSubscriber {

    private var client: RedisClient? = null
    private var sub: StatefulRedisPubSubConnection<String, String>? = null
    private var listener: RedisPubSubListener<String, String>? = null
    private var started = false
    private val handlers = mutableMapOf<String, (String) -> Unit>()

    fun register(channel: String, handler: (String) -> Unit) { handlers[channel] = handler }

    fun start(plugin: JavaPlugin = Universum.instance, uri: String = Universum.instance.config.getString("redis.uri") ?: "redis://localhost:6379") {
        if (started) return
        client = RedisClient.create(uri)
        sub = client!!.connectPubSub()

        val localListener = object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (!plugin.isEnabled) return
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!plugin.isEnabled) return@Runnable
                    handlers[channel]?.invoke(message)
                })
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

        if (handlers.isNotEmpty()) sub!!.sync().subscribe(*handlers.keys.toTypedArray())
    }

    fun subscribe(vararg channels: String) {
        channels.forEach { if (!handlers.containsKey(it)) handlers[it] = { _ -> } }
        if (started) sub?.sync()?.subscribe(*channels)
    }

    fun stop() {
        if (!started) return
        started = false
        try { listener?.let { sub?.removeListener(it) } } catch (_: Exception) {}
        try { if (sub?.isOpen == true) sub?.sync()?.unsubscribe() } catch (_: Exception) {}
        try { sub?.close(); client?.shutdown() } catch (_: Exception) {}
        listener = null
        sub = null
        client = null
        handlers.clear()
    }
}
