package org.tainorazum.universum.placeholders.holo

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
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

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return ""

        val lowercaseParams = params.lowercase()
        val args = lowercaseParams.split("_")
        if (args.size < 2) return null

        val holoId = args[0]
        val action = args[1] // "title", "current", or position number like "1"

        // Handle page number indicator: %holopage_<holoId>_current%
        if (action == "current") {
            return HologramPageManager.getPage(player.uniqueId, holoId).toString()
        }

        // Get current page configuration for this hologram
        val currentPageNumber = HologramPageManager.getPage(player.uniqueId, holoId)
        val activePageConfig = HologramRegistry.getActivePage(holoId, currentPageNumber) ?: return "Invalid Holo"

        // Handle title: %holopage_<holoId>_title%
        if (action == "title") {
            return activePageConfig.title
        }

        // Handle position query: %holopage_<holoId>_<position>_[name/value]%
        val positionIndex = (action.toIntOrNull() ?: return null) - 1
        val fetchType = args.getOrNull(2) ?: "all" // "name", "value", or "all"

        // Pass the page config's ranked property (defaults to false if not specified on the page)
        val isRanked = activePageConfig.isRanked

        val future = if (activePageConfig.gameType != null) {
            DB_Stats.getTop(
                id = activePageConfig.statIdentifier,
                gameType = activePageConfig.gameType,
                ranked = isRanked,
                limit = 10
            )
        } else {
            // GameType is null -> Fetch TOTAL across all gametypes
            DB_Stats.getTopGlobal(
                id = activePageConfig.statIdentifier,
                ranked = isRanked,
                limit = 10
            )
        }

        return if (future.isDone) {
            val topList = future.getNow(emptyList())
            val entry = topList.getOrNull(positionIndex) ?: return "N/A"

            val resolvedName = resolvePlayerName(entry.uuid)
            val formattedValue = formatValue(entry.value)

            when (fetchType) {
                "name" -> resolvedName
                "value" -> formattedValue
                else -> "$resolvedName: $formattedValue"
            }
        } else {
            "Loading..."
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