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
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.network.NetworkPlayer
import org.tainorazum.universum.network.NetworkPlayerEvent
import org.tainorazum.universum.punishment.DB_Punishments
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

    val onlinePlayers = ConcurrentHashMap<UUID, NetworkPlayer>()

    init {
        event<PlayerJoinEvent>(priority = EventPriority.LOW) {
            joinMessage(null)
            player.gameMode = GameMode.SPECTATOR

            handleLocalJoin(player, Universum.server.identifier)
        }

        event<PlayerQuitEvent>(priority = EventPriority.LOW) {
            quitMessage(null)
            handleLocalQuit(player, Universum.server.identifier)
        }
    }

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
        }
    }

    fun handleLocalJoin(player: Player, serverName: String) {
        val netPlayer = NetworkPlayer(player.uniqueId, player.name, serverName)
        onlinePlayers[player.uniqueId] = netPlayer

        RedisManager.hset(REDIS_KEY, player.uniqueId.toString(), gson.toJson(netPlayer))

        val payload = gson.toJson(NetworkPlayerEvent(netPlayer, NetworkPlayerEvent.Type.JOIN))
        RedisManager.publish(CHANNEL, payload)

        PlayerUtility.cachePlayer(player.uniqueId, player.name)
        DB_Punishments.getPunishments(player.uniqueId)
        DB_PlayerCosmetics.getPlayerCosmetics(player.uniqueId).thenApply { }
    }

    fun handleLocalQuit(player: Player, serverName: String) {
        // Local quit logic if needed
    }

    fun handleTrueNetworkQuit(uuid: UUID) {
        onlinePlayers.remove(uuid)
        RedisManager.hdel(REDIS_KEY, uuid.toString())

        DB_PlayerSettings.invalidateCache(uuid)
        PlayerUtility.uncachePlayer(uuid)
        DB_Punishments.invalidate(uuid)
        DB_PlayerCosmetics.invalidate(uuid)
    }

    fun handleVelocitySwitch(uuid: UUID, name: String, currentServer: String) {
        val netPlayer = NetworkPlayer(uuid, name, currentServer)
        onlinePlayers[uuid] = netPlayer

        // Update Redis Hash with new server location
        RedisManager.hset(REDIS_KEY, uuid.toString(), gson.toJson(netPlayer))
    }

    fun handleRemoteEvent(event: NetworkPlayerEvent) {
        when (event.type) {
            NetworkPlayerEvent.Type.JOIN -> onlinePlayers[event.player.uuid] = event.player
            NetworkPlayerEvent.Type.QUIT,
            NetworkPlayerEvent.Type.BANNED,
            NetworkPlayerEvent.Type.MUTED,
            NetworkPlayerEvent.Type.KICKED -> handleTrueNetworkQuit(event.player.uuid)
        }
    }

    fun broadcastJoin(name: String) {
        broadcast(Messages.JOIN_MSG().replace("<name>", name))
    }

    fun broadcastLeave(name: String) {
        broadcast(Messages.LEAVE_MSG().replace("<name>", name))
    }

    fun broadcast(message: Component) {
        val jsonComponent = GsonComponentSerializer.gson().serialize(message)
        val chatMessage = ChatMessage(null, jsonComponent, Universum.server)

        RedisManager.publish("chat_message", gson.toJson(chatMessage))
    }
}
