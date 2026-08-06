package org.tainorazum.universum.network.manager

import com.google.gson.Gson
import gg.flyte.twilight.event.event
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.network.NetworkPlayer
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.network.tab.TabManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.setting.DB_PlayerSettings
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkPlayerManager {

    private val gson = Gson()
    private const val REDIS_KEY = "network:players"
    const val CHANNEL = "network_player_events"

    // Fast in-memory cache for rendering Tab without querying Redis on every frame
    val onlinePlayers = ConcurrentHashMap<UUID, NetworkPlayer>()


    init {

        event<PlayerJoinEvent>(priority = EventPriority.LOW) {
            handleLocalJoin(player, Universum.server.identifier)
            joinMessage(null)

            player.gameMode = GameMode.SPECTATOR // security
        }

        event<PlayerQuitEvent>(priority = EventPriority.LOW) {
            handleLocalQuit(player, Universum.server.identifier)
            quitMessage(null)

            TabManager.leave(player) // change after network player leave update
            DB_PlayerSettings.invalidateCache(player.uniqueId) // change after network player leave update
        }

    }

    /**
     * Call this on server startup to pull currently online network players.
     */
    fun loadInitialPlayers() {
        RedisManager.get(REDIS_KEY).thenAccept { _ ->
            // Fetch all entries from the Redis Hash (Requires hgetall in RedisManager)
            // Or query active keys to populate onlinePlayers on boot
        }
    }

    /**
     * Call this inside PlayerJoinEvent on Bukkit.
     */
    fun handleLocalJoin(player: Player, serverName: String) {
        val netPlayer = NetworkPlayer(player.uniqueId, player.name, serverName)
        val payload = gson.toJson(NetworkPlayerEvent(netPlayer, NetworkPlayerEvent.Type.JOIN))

        // 1. Update persistent Redis state
        RedisManager.hset(REDIS_KEY, player.uniqueId.toString(), gson.toJson(netPlayer))

        // 2. Broadcast event to all other servers
        RedisManager.publish(CHANNEL, payload)
    }

    /**
     * Call this inside PlayerQuitEvent on Bukkit.
     */
    fun handleLocalQuit(player: Player, serverName: String) {
        val netPlayer = NetworkPlayer(player.uniqueId, player.name, serverName)
        val payload = gson.toJson(NetworkPlayerEvent(netPlayer, NetworkPlayerEvent.Type.QUIT))

        // 1. Remove from persistent Redis state
        RedisManager.hdel(REDIS_KEY, player.uniqueId.toString())

        // 2. Broadcast event to all other servers
        RedisManager.publish(CHANNEL, payload)
    }

    /**
     * Called when a Pub/Sub message arrives from ANY server.
     */
    fun handleRemoteEvent(event: NetworkPlayerEvent) {
        when (event.type) {
            NetworkPlayerEvent.Type.JOIN -> {
                onlinePlayers[event.player.uuid] = event.player
            }
            NetworkPlayerEvent.Type.QUIT -> {
                onlinePlayers.remove(event.player.uuid)
            }
            NetworkPlayerEvent.Type.BANNED -> {}
            NetworkPlayerEvent.Type.MUTED -> {}
            NetworkPlayerEvent.Type.KICKED -> {}
        }

        // Trigger Tab update across all local online players
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