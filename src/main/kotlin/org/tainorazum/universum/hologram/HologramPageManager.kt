package org.tainorazum.universum.hologram

import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.statistics.StatisticType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class HologramStatPage(
    val title: String,
    val statIdentifier: StatisticType,
    val gameType: GameType? = null,
    val isRanked: Boolean = false
)

object HologramRegistry {

    // Registry mapping Hologram ID -> List of Pages
    private val holograms = mapOf(
        "stats1" to listOf(
            HologramStatPage("Top Kills Ranked", StatisticType.KILLS, isRanked = true), // total
            HologramStatPage("Top Deaths Ranked", StatisticType.DEATHS, isRanked = true), // total
            HologramStatPage("Top Wins Ranked", StatisticType.WINS, isRanked = true), // total
            HologramStatPage("Top Losses Ranked", StatisticType.LOSSES, isRanked = true), // total
            HologramStatPage("Top Elo Ranked", StatisticType.ELO, isRanked = true), // total
            HologramStatPage("Top Coins", StatisticType.COINS, isRanked = true), // total
        ),
        "stats2" to listOf(
            HologramStatPage("Top Kills Unranked", StatisticType.KILLS, isRanked = false), // total
            HologramStatPage("Top Deaths Unranked", StatisticType.DEATHS, isRanked = false), // total
            HologramStatPage("Top Wins Unranked", StatisticType.WINS, isRanked = false), // total
            HologramStatPage("Top Losses Unranked", StatisticType.LOSSES, isRanked = false), // total
        )
    )

    fun getPages(holoId: String): List<HologramStatPage>? = holograms[holoId.lowercase()]

    fun getActivePage(holoId: String, pageNumber: Int): HologramStatPage? {
        val pages = getPages(holoId) ?: return null
        val index = (pageNumber - 1).coerceIn(0, pages.size - 1)
        return pages.getOrNull(index)
    }
}

object HologramPageManager {

    private data class PageKey(val playerUuid: UUID, val holoId: String)

    private val playerPages = ConcurrentHashMap<PageKey, Int>()

    fun getPage(playerUuid: UUID, holoId: String): Int {
        val key = PageKey(playerUuid, holoId.lowercase())
        return playerPages.getOrDefault(key, 1)
    }

    fun setPage(playerUuid: UUID, holoId: String, page: Int) {
        val key = PageKey(playerUuid, holoId.lowercase())
        playerPages[key] = page.coerceAtLeast(1)
    }

    fun cycleNext(playerUuid: UUID, holoId: String) {
        val maxPages = HologramRegistry.getPages(holoId)?.size ?: 1
        val current = getPage(playerUuid, holoId)
        val next = if (current >= maxPages) 1 else current + 1
        setPage(playerUuid, holoId, next)
    }

    fun cyclePrev(playerUuid: UUID, holoId: String) {
        val maxPages = HologramRegistry.getPages(holoId)?.size ?: 1
        val current = getPage(playerUuid, holoId)
        val prev = if (current <= 1) maxPages else current - 1
        setPage(playerUuid, holoId, prev)
    }

    fun clearPlayer(playerUuid: UUID) {
        playerPages.keys.removeIf { it.playerUuid == playerUuid }
    }
}