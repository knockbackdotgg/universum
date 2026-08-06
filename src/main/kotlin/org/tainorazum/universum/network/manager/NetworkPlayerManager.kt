package org.tainorazum.universum.network.manager

import com.google.gson.Gson
import gg.flyte.twilight.event.event
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.network.NetworkPlayer
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.network.tab.TabManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.setting.DB_PlayerSettings
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkPlayerManager {

    private val gson = Gson()
    private const val REDIS_KEY = "network:players"
    const val CHANNEL = "network_player_events"

    // Fast in-memory cache for rendering Tab
    val onlinePlayers = ConcurrentHashMap<UUID, NetworkPlayer>()

    init {
        event<PlayerJoinEvent>(priority = EventPriority.LOW) {
            joinMessage(null)
            player.gameMode = GameMode.SPECTATOR // security

            handleLocalJoin(player, Universum.server.identifier)
        }

        event<PlayerQuitEvent>(priority = EventPriority.LOW) {
            quitMessage(null)

            TabManager.leave(player)
            handleLocalQuit(player, Universum.server.identifier)
        }
    }

    /**
     * Call this on server boot to sync existing network players from Redis
     */
    fun loadInitialPlayers() {
        RedisManager.hgetall(REDIS_KEY).thenAccept { map ->
            map?.forEach { (uuidStr, json) ->
                try {
                    val netPlayer = gson.fromJson(json, NetworkPlayer::class.java)
                    onlinePlayers[UUID.fromString(uuidStr)] = netPlayer
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            updateTabListForAll()
        }
    }

    /**
     * Handles local Bukkit player joins.
     */
    fun handleLocalJoin(player: Player, serverName: String) {
        val netPlayer = NetworkPlayer(player.uniqueId, player.name, serverName)
        onlinePlayers[player.uniqueId] = netPlayer

        // Update persistent Redis state with updated sub-server
        RedisManager.hset(REDIS_KEY, player.uniqueId.toString(), gson.toJson(netPlayer))

        // Notify network of local server switch / join
        val payload = gson.toJson(NetworkPlayerEvent(netPlayer, NetworkPlayerEvent.Type.JOIN))
        RedisManager.publish(CHANNEL, payload)
    }

    /**
     * Handles local Bukkit player quits.
     */
    fun handleLocalQuit(player: Player, serverName: String) {
        // Local quit ignored; true network disconnect managed via Velocity event
    }

    /**
     * Called when Velocity confirms a player COMPLETELY disconnected from the network.
     */
    fun handleTrueNetworkQuit(uuid: UUID) {
        val removed = onlinePlayers.remove(uuid)

        // Remove from Redis hash
        RedisManager.hdel(REDIS_KEY, uuid.toString())

        // Invalidate player settings cache safely now that they are off the network
        DB_PlayerSettings.invalidateCache(uuid)

        // Remove from tab
        if (removed != null) {
            TabManager.removeRemotePlayer(removed.name)
        }

        // Refresh Tab across network
        updateTabListForAll()
    }

    /**
     * Called when Velocity notifies sub-servers of a server switch or new network join.
     */
    fun handleVelocitySwitch(uuid: UUID, name: String, currentServer: String) {
        val netPlayer = NetworkPlayer(uuid, name, currentServer)
        onlinePlayers[uuid] = netPlayer

        updateTabListForAll()
    }

    /**
     * Called when Pub/Sub messages arrive from other Bukkit instances.
     */
    fun handleRemoteEvent(event: NetworkPlayerEvent) {
        when (event.type) {
            NetworkPlayerEvent.Type.JOIN -> {
                onlinePlayers[event.player.uuid] = event.player
            }
            NetworkPlayerEvent.Type.QUIT -> {}
            NetworkPlayerEvent.Type.BANNED,
            NetworkPlayerEvent.Type.MUTED,
            NetworkPlayerEvent.Type.KICKED -> {}
        }

        updateTabListForAll()
    }

    private fun updateTabListForAll() {
        TabManager.update()
    }

    fun broadcastJoin(name: String) {
        broadcast(Messages.JOIN_MSG().replace("<name>", name))
    }

    fun broadcastLeave(name: String) {
        broadcast(Messages.LEAVE_MSG().replace("<name>", name))
    }

    fun broadcast(message: Component) {
        val jsonComponent = GsonComponentSerializer.gson().serialize(message)
        val chatMessage = ChatMessage(null, jsonComponent)

        RedisManager.publish("chat_message", gson.toJson(chatMessage))
    }
}