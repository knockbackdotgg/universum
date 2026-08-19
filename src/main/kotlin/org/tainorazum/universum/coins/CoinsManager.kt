package org.tainorazum.universum.coins

import org.tainorazum.universum.coins.database.DB_Coins
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class CoinsManager {

    private val coinsCache = ConcurrentHashMap<UUID, Int>()

    /**
     * Loads a player's coin balance into local RAM cache (call on join).
     */
    fun loadPlayer(uuid: UUID): CompletableFuture<Int> {
        return DB_Coins.getCoins(uuid).thenApply { fetchedCoins ->
            coinsCache[uuid] = fetchedCoins
            fetchedCoins
        }
    }

    /**
     * Removes player data from RAM cache (call on leave/quit).
     */
    fun unloadPlayer(uuid: UUID) {
        coinsCache.remove(uuid)
    }

    /**
     * Synchronously gets cached coins (defaults to 0 if not pre-cached).
     */
    fun getCoins(uuid: UUID): Int {
        return coinsCache[uuid] ?: 0
    }

    /**
     * Asynchronously gets current coin balance from DB or cache.
     */
    fun getCoinsAsync(uuid: UUID): CompletableFuture<Int> {
        val cached = coinsCache[uuid]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }
        return loadPlayer(uuid)
    }

    /**
     * Sets exact coin count, updates cache, and persists asynchronously.
     */
    fun setCoins(uuid: UUID, amount: Int): CompletableFuture<Boolean> {
        val sanitizedAmount = amount.coerceAtLeast(0)
        coinsCache[uuid] = sanitizedAmount
        return DB_Coins.setCoins(uuid, sanitizedAmount)
    }

    /**
     * Adds coins to balance.
     */
    fun addCoins(uuid: UUID, amount: Int): CompletableFuture<Boolean> {
        if (amount <= 0) return CompletableFuture.completedFuture(false)
        val newTotal = getCoins(uuid) + amount
        return setCoins(uuid, newTotal)
    }

    /**
     * Attempts to deduct coins. Returns false if funds are insufficient.
     */
    fun removeCoins(uuid: UUID, amount: Int): Boolean {
        if (amount <= 0) return false
        val currentCoins = getCoins(uuid)
        if (currentCoins < amount) return false

        setCoins(uuid, currentCoins - amount)
        return true
    }

    /**
     * Checks if player has at least the specified coin amount.
     */
    fun hasCoins(uuid: UUID, amount: Int): Boolean {
        return getCoins(uuid) >= amount
    }
}