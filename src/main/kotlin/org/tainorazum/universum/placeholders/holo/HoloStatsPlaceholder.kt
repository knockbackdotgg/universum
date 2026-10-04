package org.tainorazum.universum.placeholders.holo

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.tainorazum.universum.coins.database.DB_Coins
import org.tainorazum.universum.engine.statistics.StatisticType
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.hologram.HologramPageManager
import org.tainorazum.universum.hologram.HologramRegistry
import org.tainorazum.universum.utility.player.PlayerUtility
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HoloStatsPlaceholder : PlaceholderExpansion() {

    override fun getIdentifier() = "holopage"
    override fun getAuthor() = "tainorazum"
    override fun getVersion() = "1.0"

    override fun persist() = true

    private val resolvedNames = ConcurrentHashMap<UUID, String>()

    // Cache structure: Key -> (FetchTimestamp, LeaderboardEntries)
    private val leaderboardCache = ConcurrentHashMap<String, Pair<Long, List<DB_Stats.LeaderboardEntry>>>()
    private val pendingQueries = ConcurrentHashMap.newKeySet<String>()

    private val cacheTtlMs = 60_000L // Refresh cache every 60 seconds

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return ""

        val lowercaseParams = params.lowercase()
        val args = lowercaseParams.split("_")
        if (args.size < 2) return null

        val holoId = args[0]
        val action = args[1]

        if (action == "current") {
            return HologramPageManager.getPage(player.uniqueId, holoId).toString()
        }

        val currentPageNumber = HologramPageManager.getPage(player.uniqueId, holoId)
        val activePageConfig = HologramRegistry.getActivePage(holoId, currentPageNumber) ?: return "Invalid Holo"

        if (action == "title") {
            return activePageConfig.title
        }

        val positionIndex = (action.toIntOrNull() ?: return null) - 1
        val fetchType = args.getOrNull(2) ?: "all"

        // Build a unique key for caching this specific leaderboard
        val cacheKey = "${activePageConfig.statIdentifier}_${activePageConfig.gameType}_${activePageConfig.isRanked}"
        val cached = leaderboardCache[cacheKey]
        val now = System.currentTimeMillis()

        // Trigger a background refresh if cache is missing or expired
        if ((cached == null || now - cached.first > cacheTtlMs) && pendingQueries.add(cacheKey)) {
            val future = if (activePageConfig.statIdentifier == StatisticType.COINS) {
                DB_Coins.getTopCoins(10)
            } else if (activePageConfig.gameType != null) {
                DB_Stats.getTop(
                    id = activePageConfig.statIdentifier,
                    gameType = activePageConfig.gameType,
                    ranked = activePageConfig.isRanked,
                    limit = 10
                )
            } else {
                DB_Stats.getTopGlobal(
                    id = activePageConfig.statIdentifier,
                    ranked = activePageConfig.isRanked,
                    limit = 10
                )
            }

            future.thenAccept { list ->
                leaderboardCache[cacheKey] = Pair(System.currentTimeMillis(), list)
                pendingQueries.remove(cacheKey)
            }.exceptionally {
                pendingQueries.remove(cacheKey)
                null
            }
        }

        // Return cached entries if available
        val topList = cached?.second ?: return "Loading..."
        val entry = topList.getOrNull(positionIndex) ?: return "N/A"

        val resolvedName = resolvePlayerName(entry.uuid)
        val formattedValue = formatValue(entry.value)

        return when (fetchType) {
            "name" -> resolvedName
            "value" -> formattedValue
            else -> "$resolvedName: $formattedValue"
        }
    }

    private fun resolvePlayerName(uuidString: String): String {
        val uuid = try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            return uuidString
        }

        val cachedName = resolvedNames[uuid]
        if (cachedName != null) return cachedName

        val bukkitName = Bukkit.getOfflinePlayer(uuid).name
        if (bukkitName != null) {
            resolvedNames[uuid] = bukkitName
            return bukkitName
        }

        PlayerUtility.getName(uuid).thenAccept { name ->
            if (name != null) {
                resolvedNames[uuid] = name
            }
        }

        return uuidString.take(8)
    }

    private fun formatValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}