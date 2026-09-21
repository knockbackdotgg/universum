package org.tainorazum.universum.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.listeners.friends.RM_FriendAccept
import org.tainorazum.universum.redis.listeners.friends.RM_FriendRemove
import org.tainorazum.universum.redis.listeners.friends.RM_FriendRequest
import org.tainorazum.universum.redis.listeners.network.RM_ChatMessage
import org.tainorazum.universum.redis.listeners.games.RM_GameRequest
import org.tainorazum.universum.redis.listeners.network.RM_Nametag
import org.tainorazum.universum.redis.listeners.network.RM_Online
import org.tainorazum.universum.redis.listeners.network.RM_PrivateMessage
import org.tainorazum.universum.redis.listeners.network.RM_ServerMessage
import org.tainorazum.universum.redis.listeners.velocity.RM_VelocityPlayerDisconnect
import org.tainorazum.universum.redis.listeners.velocity.RM_VelocityPlayerSwitch
import org.tainorazum.universum.server.server.ServerType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.logging.Level

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
        // games
        if(Universum.server.type == ServerType.GAME) {
            Universum.instance.logger.log(Level.INFO, "Registered game request" )
            RM_GameRequest()
        }

        // friends
        RM_FriendAccept()
        RM_FriendRequest()
        RM_FriendRemove()

        // network
        RM_Online()
        RM_ChatMessage()
        RM_ServerMessage()
        RM_PrivateMessage()
        RM_Nametag()

        // velocity
        RM_VelocityPlayerDisconnect()
        RM_VelocityPlayerSwitch()
    }
}