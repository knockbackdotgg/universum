package org.tainorazum.universum.network.manager

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.network.NetworkPlayer
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.redis.RedisManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NetworkPlayerManager {

    private val gson = Gson()
    private const val REDIS_KEY = "network:players"
    const val CHANNEL = "network_player_events"

    // Fast in-memory cache for rendering Tab without querying Redis on every frame
    val onlinePlayers = ConcurrentHashMap<UUID, NetworkPlayer>()

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
            NetworkPlayerEvent.Type.JOIN -> onlinePlayers[event.player.uuid] = event.player
            NetworkPlayerEvent.Type.QUIT -> onlinePlayers.remove(event.player.uuid)
        }

        // Trigger Tab list updates on the main Bukkit thread
        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
            updateTabListForAll()
        })
    }

    private fun updateTabListForAll() {
        // Render or update Tab list for online players using onlinePlayers.values
    }
}