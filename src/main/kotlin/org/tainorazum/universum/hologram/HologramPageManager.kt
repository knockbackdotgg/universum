package org.tainorazum.universum.hologram

import org.tainorazum.universum.engine.game.manager.fields.GameType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class HologramStatPage(
    val title: String,
    val statIdentifier: String, // e.g. "kills"
    val gameType: GameType? = null,
    val isRanked: Boolean = false
)

object HologramRegistry {

    // Registry mapping Hologram ID -> List of Pages
    private val holograms = mapOf(
        "stats1" to listOf(
            HologramStatPage("Top Kills Ranked", "kills", isRanked = true), // total
            HologramStatPage("Top Deaths Ranked", "deaths", isRanked = true), // total
            HologramStatPage("Top Wins Ranked", "wins", isRanked = true), // total
            HologramStatPage("Top Losses Ranked", "losses", isRanked = true), // total
            HologramStatPage("Top Elo Ranked", "elo", isRanked = true), // total
            HologramStatPage("Top Coins", "coins", isRanked = true), // total
        ),
        "stats2" to listOf(
            HologramStatPage("Top Kills Unranked", "kills", isRanked = false), // total
            HologramStatPage("Top Deaths Unranked", "deaths", isRanked = false), // total
            HologramStatPage("Top Wins Unranked", "wins", isRanked = false), // total
            HologramStatPage("Top Losses Unranked", "losses", isRanked = false), // total
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