package org.tainorazum.universum.network.nametag

import com.google.gson.Gson
import gg.flyte.twilight.event.event
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.RedisManager // Replace with your Redis class
import org.tainorazum.universum.redis.listeners.RM_Nametag
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.emptyMap

data class NametagPayload(
    val uuid: String,
    val name: String,
    val weight: Int,
    val prefixRaw: String
)

object NametagManager {

    private val gson = Gson()
    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    private const val MAX_WEIGHT = 999
    private const val TEAM_NAME_PREFIX = "tag_"
    private const val REDIS_HASH_KEY = "universum:nametag_cache"

    private val networkCache = ConcurrentHashMap<UUID, NametagPayload>()

    fun init() {
        registerLuckPermsHook()

        event<PlayerJoinEvent> {
            val player = this.player
            Bukkit.getScheduler().runTaskLater(Universum.instance, Runnable {
                if (!player.isOnline) return@Runnable

                syncAllNetworkTeamsToPlayer(player)
                updateAndBroadcastPlayer(player)
            }, 5L)
        }

        event<PlayerQuitEvent> {
            val uuid = player.uniqueId
            RedisManager.hdel(REDIS_HASH_KEY, uuid.toString())
            RedisManager.publish("nametag", "REMOVE:$uuid")
        }

        // Catch online players during reloads
        for (player in Bukkit.getOnlinePlayers()) {
            syncAllNetworkTeamsToPlayer(player)
            updateAndBroadcastPlayer(player)
        }
    }

    private fun registerLuckPermsHook() {
        try {
            Universum.luckperms.eventBus.subscribe(Universum.instance, UserDataRecalculateEvent::class.java) { event ->
                val player = Bukkit.getPlayer(event.user.uniqueId) ?: return@subscribe
                updateAndBroadcastPlayer(player)
            }
        } catch (e: Exception) {
            Universum.instance.logger.warning("LuckPerms API hook failed inside NametagManager.")
        }
    }

    fun updateAndBroadcastPlayer(player: Player) {
        val lpUser = Universum.luckperms.userManager.getUser(player.uniqueId) ?: return
        val prefixRaw = lpUser.cachedData.metaData.prefix ?: ""
        val weight = getHighestGroupWeight(lpUser)

        val payload = NametagPayload(
            uuid = player.uniqueId.toString(),
            name = player.name,
            weight = weight,
            prefixRaw = prefixRaw
        )

        if (networkCache[player.uniqueId] == payload) return
        networkCache[player.uniqueId] = payload

        applyNametagToLocalScoreboards(payload)

        val json = gson.toJson(payload)
        RedisManager.hset(REDIS_HASH_KEY, payload.uuid, json)
        RedisManager.publish("nametag", json)
    }

    fun handleNetworkUpdate(jsonPayload: String) {
        val payload = gson.fromJson(jsonPayload, NametagPayload::class.java) ?: return
        val uuid = UUID.fromString(payload.uuid)

        networkCache[uuid] = payload

        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
            applyNametagToLocalScoreboards(payload)
        })
    }

    fun handleNetworkRemove(uuidString: String) {
        val uuid = UUID.fromString(uuidString)
        val removedPayload = networkCache.remove(uuid) ?: return

        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
            removeNametagFromLocalScoreboards(removedPayload.name)
        })
    }

    private fun syncAllNetworkTeamsToPlayer(player: Player) {
        RedisManager.hgetall(REDIS_HASH_KEY).thenAccept { entries ->
            if (entries.isNullOrEmpty()) return@thenAccept

            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                if (!player.isOnline) return@Runnable

                for (json in entries.values) {
                    val payload = gson.fromJson(json, NametagPayload::class.java) ?: continue
                    val uuid = UUID.fromString(payload.uuid)

                    networkCache[uuid] = payload
                    applyNametagToLocalScoreboards(payload)
                }
            })
        }
    }

    private fun getHighestGroupWeight(user: User): Int {
        val metaWeight = user.cachedData.metaData.weight
        if (metaWeight != 0) return metaWeight

        return user.getNodes(NodeType.INHERITANCE)
            .mapNotNull { node -> Universum.luckperms.groupManager.getGroup(node.groupName) }
            .maxOfOrNull { group -> group.weight.orElse(0) } ?: 0
    }

    private fun applyNametagToLocalScoreboards(payload: NametagPayload) {
        val sortOrder = (MAX_WEIGHT - payload.weight).coerceIn(0, MAX_WEIGHT).toString().padStart(3, '0')
        val teamName = "$TEAM_NAME_PREFIX${sortOrder}_${payload.name}"

        val prefixComponent = if (payload.prefixRaw.isNotBlank()) legacySerializer.deserialize(payload.prefixRaw) else Component.empty()
        val hasText = plainSerializer.serialize(prefixComponent).isNotBlank()
        val finalPrefix = if (hasText) prefixComponent.append(Component.text(" ")) else prefixComponent

        for (viewer in Bukkit.getOnlinePlayers()) {
            val scoreboard = viewer.scoreboard

            scoreboard.getEntryTeam(payload.name)?.let { oldTeam ->
                if (oldTeam.name != teamName) {
                    oldTeam.removeEntry(payload.name)
                    if (oldTeam.entries.isEmpty()) oldTeam.unregister()
                }
            }

            val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)

            if (team.prefix() != finalPrefix) {
                team.prefix(finalPrefix)
            }

            team.color(NamedTextColor.GRAY)

            if (!team.hasEntry(payload.name)) {
                team.addEntry(payload.name)
            }
        }
    }

    private fun removeNametagFromLocalScoreboards(playerName: String) {
        for (viewer in Bukkit.getOnlinePlayers()) {
            val scoreboard = viewer.scoreboard
            scoreboard.getEntryTeam(playerName)?.let { team ->
                team.removeEntry(playerName)
                if (team.entries.isEmpty()) team.unregister()
            }
        }
    }
}