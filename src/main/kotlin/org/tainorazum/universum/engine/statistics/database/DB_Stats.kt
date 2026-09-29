package org.tainorazum.universum.engine.statistics.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object DB_Stats : Table("statistics") {

    val identifier = varchar("identifier", 100) // Clean statistic identifier (e.g. "kills", "deaths")
    val gametype = enumerationByName<GameType>("gametype", 100)
    val ranked = bool("ranked").default(false)

    val uuid = varchar("uuid", 36)

    val value = double("value").default(0.0)

    override val primaryKey = PrimaryKey(identifier, gametype, ranked, uuid)

    // Cache: keep in-memory values and accumulate local deltas.
    private data class StatsKey(val id: String, val gametype: GameType, val ranked: Boolean, val uuid: String)

    private val cache = ConcurrentHashMap<StatsKey, Double>()
    private val pendingDeltas = mutableMapOf<StatsKey, Double>()
    private val pendingLock = Any()

    // Cache structure for Leaderboards
    private data class LeaderboardKey(val id: String, val gametype: GameType, val ranked: Boolean)
    data class LeaderboardEntry(val uuid: String, val value: Double)

    private data class CachedLeaderboard(
        val entries: List<LeaderboardEntry>,
        val timestamp: Long
    )

    // Cache map for Top Leaderboards
    private val leaderboardCache = ConcurrentHashMap<LeaderboardKey, CachedLeaderboard>()
    private val leaderboardFetchLocks = ConcurrentHashMap<LeaderboardKey, Any>()
    private val globalLeaderboardCache = ConcurrentHashMap<Pair<String, Boolean>, CachedLeaderboard>()
    private val globalLeaderboardFetchLocks = ConcurrentHashMap<Pair<String, Boolean>, Any>()

    // Scheduler to flush periodically.
    private var scheduler: ScheduledExecutorService? = null

    /**
     * Read-through cache get. If not cached, reads from DB and caches result.
     */
    fun get(
        id: String,
        gameType: GameType,
        playerUuid: String,
        ranked: Boolean = false
    ): CompletableFuture<Double> {
        val key = StatsKey(id, gameType, ranked, playerUuid)
        val cached = cache[key]
        if (cached != null) return CompletableFuture.completedFuture(cached)

        return dba {
            transaction {
                val valueFromDb = DB_Stats.selectAll()
                    .where {
                        (DB_Stats.identifier eq id) and
                                (DB_Stats.gametype eq gameType) and
                                (DB_Stats.ranked eq ranked) and
                                (DB_Stats.uuid eq playerUuid)
                    }
                    .limit(1)
                    .map { it[DB_Stats.value] }
                    .singleOrNull() ?: 0.0

                val pending = synchronized(pendingLock) { pendingDeltas[key] ?: 0.0 }
                val combined = valueFromDb + pending
                cache[key] = combined
                combined
            }
        }
    }

    /**
     * Set a statistic to an absolute value.
     */
    fun set(
        id: String,
        gameType: GameType,
        playerUuid: String,
        value: Double,
        ranked: Boolean = false
    ): CompletableFuture<Boolean> {
        val key = StatsKey(id, gameType, ranked, playerUuid)

        val current = cache[key]
        if (current != null) {
            val delta = value - current
            cache[key] = value
            synchronized(pendingLock) {
                pendingDeltas.merge(key, delta) { a, b -> a + b }
            }
            return CompletableFuture.completedFuture(true)
        }

        return dba {
            transaction {
                val dbVal = DB_Stats.selectAll()
                    .where {
                        (DB_Stats.identifier eq id) and
                                (DB_Stats.gametype eq gameType) and
                                (DB_Stats.ranked eq ranked) and
                                (DB_Stats.uuid eq playerUuid)
                    }
                    .limit(1)
                    .map { it[DB_Stats.value] }
                    .singleOrNull() ?: 0.0

                val delta = value - dbVal
                cache[key] = value
                synchronized(pendingLock) {
                    pendingDeltas.merge(key, delta) { a, b -> a + b }
                }

                true
            }
        }
    }

    /**
     * Additive update: fast local update and record delta for periodic flush.
     */
    fun add(
        id: String,
        gameType: GameType,
        playerUuid: String,
        delta: Double,
        ranked: Boolean = false
    ): CompletableFuture<Boolean> {
        val key = StatsKey(id, gameType, ranked, playerUuid)

        cache.compute(key) { _, old -> (old ?: 0.0) + delta }

        synchronized(pendingLock) {
            pendingDeltas.merge(key, delta) { a, b -> a + b }
        }

        return CompletableFuture.completedFuture(true)
    }

    /**
     * Gets the top players for a specific statistic and game type.
     */
    fun getTop(
        id: String,
        gameType: GameType,
        ranked: Boolean = false,
        limit: Int = 10,
        cacheDurationMinutes: Long = 5L
    ): CompletableFuture<List<LeaderboardEntry>> {
        val key = LeaderboardKey(id, gameType, ranked)
        val now = System.currentTimeMillis()
        val cacheTtlMillis = TimeUnit.MINUTES.toMillis(cacheDurationMinutes)

        val cached = leaderboardCache[key]

        if (cached != null && (now - cached.timestamp) < cacheTtlMillis) {
            return CompletableFuture.completedFuture(cached.entries.take(limit))
        }

        return dba {
            val lock = leaderboardFetchLocks.computeIfAbsent(key) { Any() }
            synchronized(lock) {
                val rechecked = leaderboardCache[key]
                if (rechecked != null && (System.currentTimeMillis() - rechecked.timestamp) < cacheTtlMillis) {
                    return@dba rechecked.entries.take(limit)
                }

                try {
                    val dbResults = transaction {
                        DB_Stats.selectAll()
                            .where {
                                (DB_Stats.identifier eq id) and
                                        (DB_Stats.gametype eq gameType) and
                                        (DB_Stats.ranked eq ranked)
                            }
                            .orderBy(DB_Stats.value to org.jetbrains.exposed.sql.SortOrder.DESC)
                            .limit(limit * 2)
                            .map {
                                LeaderboardEntry(
                                    uuid = it[DB_Stats.uuid],
                                    value = it[DB_Stats.value]
                                )
                            }
                    }

                    val mergedResults = mutableMapOf<String, Double>()
                    dbResults.forEach { mergedResults[it.uuid] = it.value }

                    synchronized(pendingLock) {
                        pendingDeltas.forEach { (deltaKey, deltaValue) ->
                            if (deltaKey.id == id && deltaKey.gametype == gameType && deltaKey.ranked == ranked) {
                                mergedResults.compute(deltaKey.uuid) { _, current ->
                                    (current ?: 0.0) + deltaValue
                                }
                            }
                        }
                    }

                    val finalTopList = mergedResults.map { LeaderboardEntry(it.key, it.value) }
                        .sortedByDescending { it.value }
                        .take(limit)

                    leaderboardCache[key] = CachedLeaderboard(finalTopList, System.currentTimeMillis())

                    finalTopList.forEach { entry ->
                        cache[StatsKey(id, gameType, ranked, entry.uuid)] = entry.value
                    }

                    finalTopList
                } finally {
                    leaderboardFetchLocks.remove(key)
                }
            }
        }
    }

    /**
     * Gets the top global players across all game types for a specific ranked state.
     */
    fun getTopGlobal(
        id: String,
        ranked: Boolean = false,
        limit: Int = 10,
        cacheDurationMinutes: Long = 5L
    ): CompletableFuture<List<LeaderboardEntry>> {
        val now = System.currentTimeMillis()
        val cacheTtlMillis = TimeUnit.MINUTES.toMillis(cacheDurationMinutes)
        val cacheKey = Pair(id, ranked)

        val cached = globalLeaderboardCache[cacheKey]
        if (cached != null && (now - cached.timestamp) < cacheTtlMillis) {
            return CompletableFuture.completedFuture(cached.entries.take(limit))
        }

        return dba {
            val lock = globalLeaderboardFetchLocks.computeIfAbsent(cacheKey) { Any() }
            synchronized(lock) {
                val rechecked = globalLeaderboardCache[cacheKey]
                if (rechecked != null && (System.currentTimeMillis() - rechecked.timestamp) < cacheTtlMillis) {
                    return@dba rechecked.entries.take(limit)
                }

                try {
                    val dbResults = transaction {
                        val sumColumn = DB_Stats.value.sum()
                        DB_Stats
                            .select(DB_Stats.uuid, sumColumn)
                            .where {
                                (DB_Stats.identifier eq id) and
                                        (DB_Stats.ranked eq ranked)
                            }
                            .groupBy(DB_Stats.uuid)
                            .orderBy(sumColumn to org.jetbrains.exposed.sql.SortOrder.DESC)
                            .limit(limit * 2)
                            .map {
                                LeaderboardEntry(
                                    uuid = it[DB_Stats.uuid],
                                    value = it[sumColumn] ?: 0.0
                                )
                            }
                    }

                    val mergedResults = mutableMapOf<String, Double>()
                    dbResults.forEach { mergedResults[it.uuid] = it.value }

                    synchronized(pendingLock) {
                        pendingDeltas.forEach { (deltaKey, deltaValue) ->
                            if (deltaKey.id == id && deltaKey.ranked == ranked) {
                                mergedResults.compute(deltaKey.uuid) { _, current ->
                                    (current ?: 0.0) + deltaValue
                                }
                            }
                        }
                    }

                    val finalTopList = mergedResults.map { LeaderboardEntry(it.key, it.value) }
                        .sortedByDescending { it.value }
                        .take(limit)

                    globalLeaderboardCache[cacheKey] = CachedLeaderboard(finalTopList, System.currentTimeMillis())
                    finalTopList
                } finally {
                    globalLeaderboardFetchLocks.remove(cacheKey)
                }
            }
        }
    }

    /**
     * Flush pending deltas to DB.
     */
    fun flushNow(): CompletableFuture<Boolean> {
        val toFlush: Map<StatsKey, Double>
        synchronized(pendingLock) {
            if (pendingDeltas.isEmpty()) return CompletableFuture.completedFuture(true)
            toFlush = HashMap(pendingDeltas)
            pendingDeltas.clear()
        }

        return dba {
            try {
                transaction {
                    toFlush.forEach { (key, delta) ->
                        val updated = DB_Stats.update({
                            (DB_Stats.identifier eq key.id) and
                                    (DB_Stats.gametype eq key.gametype) and
                                    (DB_Stats.ranked eq key.ranked) and
                                    (DB_Stats.uuid eq key.uuid)
                        }) {
                            it[DB_Stats.value] = DB_Stats.value + delta
                        }

                        if (updated == 0) {
                            DB_Stats.insert {
                                it[DB_Stats.identifier] = key.id
                                it[DB_Stats.gametype] = key.gametype
                                it[DB_Stats.ranked] = key.ranked
                                it[DB_Stats.uuid] = key.uuid
                                it[DB_Stats.value] = delta
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                synchronized(pendingLock) {
                    toFlush.forEach { (k, v) -> pendingDeltas.merge(k, v) { a, b -> a + b } }
                }
                false
            }
        }
    }

    fun startAutoFlush(intervalSeconds: Long = 60L) {
        synchronized(this) {
            if (scheduler != null) return
            val sched = Executors.newSingleThreadScheduledExecutor()
            sched.scheduleAtFixedRate({
                try {
                    flushNow().get()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
            scheduler = sched
        }
    }

    fun shutdownAndFlush() {
        synchronized(this) {
            scheduler?.shutdown()
            scheduler = null
        }
        try {
            flushNow().get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}