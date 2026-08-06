package org.tainorazum.universum.placeholders.stats

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.tainorazum.universum.engine.game.gametypes.GameType
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.utility.player.PlayerUtility
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StatsPlaceholder : PlaceholderExpansion() {

    override fun getIdentifier() = "stats"
    override fun getAuthor() = "tainorazum"
    override fun getVersion() = "1.0"

    override fun persist() = true

    // Local cache for resolved UUID -> Name lookups to avoid redundant calls
    private val resolvedNames = ConcurrentHashMap<UUID, String>()

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val lowercaseParams = params.lowercase()

        // ============================================================
        // 1. LEADERBOARD TOP PLACEHOLDERS (%stats_top_...)
        // Formats:
        // %stats_top_<stat>_<gameType>_<ranked/unranked>_<position>_[name/value]%
        // Example: %stats_top_kills_ffa_ranked_1_name%
        // ============================================================
        if (lowercaseParams.startsWith("top_")) {
            val args = lowercaseParams.removePrefix("top_").split("_")
            if (args.size < 4) return null

            val statIdentifier = args[0] // e.g., "kills", "deaths", "elo"
            val rawGameType = args[1]    // e.g., "ffa", "boxing", "bridge"
            val isRanked = parseRanked(args[2]) // Parse "ranked"/"unranked" or "true"/"false"
            val positionIndex = (args[3].toIntOrNull() ?: 1) - 1 // Convert 1-indexed to 0-indexed

            val gameType = GameType.entries.firstOrNull {
                it.name.equals(rawGameType, ignoreCase = true)
            } ?: return null

            val fetchType = args.getOrNull(4) ?: "all" // "name", "value", or "all"

            val future = DB_Stats.getTop(
                id = statIdentifier,
                gameType = gameType,
                ranked = isRanked,
                limit = 10
            )

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

        // ============================================================
        // 2. INDIVIDUAL PLAYER STAT PLACEHOLDERS (%stats_...)
        // Format: %stats_<stat>_<gameType>_<ranked/unranked>%
        // Example: %stats_kills_ffa_ranked%
        // ============================================================
        if (player == null) return ""

        val args = lowercaseParams.split("_")
        if (args.size < 3) return null

        val statIdentifier = args[0] // e.g., "kills"
        val rawGameType = args[1]    // e.g., "boxing"
        val isRanked = parseRanked(args[2])

        val gameType = GameType.entries.firstOrNull {
            it.name.equals(rawGameType, ignoreCase = true)
        } ?: return null

        val future = DB_Stats.get(
            id = statIdentifier,
            gameType = gameType,
            playerUuid = player.uniqueId.toString(),
            ranked = isRanked
        )

        return if (future.isDone) {
            val statValue = future.getNow(0.0) ?: 0.0
            formatValue(statValue)
        } else {
            "Loading..."
        }
    }

    /**
     * Helper to parse ranked state from text parameters
     */
    private fun parseRanked(input: String): Boolean {
        return input.equals("ranked", ignoreCase = true) || input.equals("true", ignoreCase = true)
    }

    /**
     * Resolves UUID to Name using PlayerUtility.getName(uuid).
     */
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

    /**
     * Formats whole numbers as integers (e.g., 10.0 -> 10) and decimals nicely.
     */
    private fun formatValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}