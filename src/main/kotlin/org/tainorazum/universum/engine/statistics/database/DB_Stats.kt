package org.tainorazum.universum.engine.statistics.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.engine.game.gametypes.GameType
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object DB_Stats : Table("statistics") {

    val identifier = varchar("identifier", 100) // statistic identifier
    val gametype = varchar("gametype", 100)

    val uuid = varchar("uuid", 36)

    val value = double("value").default(0.0)

    override val primaryKey = PrimaryKey(identifier, gametype, uuid)

    // Cache: keep in-memory values and accumulate local deltas.
    // We apply deltas to the DB periodically to avoid contention across servers.
    private data class StatsKey(val id: String, val gametype: String, val uuid: String)

    private val cache = ConcurrentHashMap<StatsKey, Double>()
    private val pendingDeltas = mutableMapOf<StatsKey, Double>()
    private val pendingLock = Any()

    // Scheduler to flush periodically. Not started automatically; call startAutoFlush() from plugin onEnable.
    private var scheduler: ScheduledExecutorService? = null

    /**
     * Read-through cache get. If not cached, reads from DB and caches result.
     */
    fun get(id: String, gameType: GameType, playerUuid: String): CompletableFuture<Double> {
        val key = StatsKey(id, gameType.name, playerUuid)
        val cached = cache[key]
        if (cached != null) return CompletableFuture.completedFuture(cached)

        return dba {
            transaction {
                val valueFromDb = DB_Stats.selectAll()
                    .where {
                        (DB_Stats.identifier eq id) and
                            (DB_Stats.gametype eq gameType.name) and
                            (DB_Stats.uuid eq playerUuid)
                    }
                    .limit(1)
                    .map { it[DB_Stats.value] }
                    .singleOrNull() ?: 0.0

                // include any local pending delta so reads see up-to-date local state
                val pending = synchronized(pendingLock) { pendingDeltas[key] ?: 0.0 }
                val combined = valueFromDb + pending
                cache[key] = combined
                combined
            }
        }
    }

    /**
     * Set a statistic to an absolute value. Internally recorded as a delta so multiple servers can
     * safely apply changes without overwriting other servers' updates.
     */
    fun set(id: String, gameType: GameType, playerUuid: String, value: Double): CompletableFuture<Boolean> {
        val key = StatsKey(id, gameType.name, playerUuid)

        val current = cache[key]
        if (current != null) {
            val delta = value - current
            cache[key] = value
            synchronized(pendingLock) {
                pendingDeltas.merge(key, delta) { a, b -> a + b }
            }
            return CompletableFuture.completedFuture(true)
        }

        // cache miss: load DB value first, then compute delta
        return dba {
            transaction {
                val dbVal = DB_Stats.selectAll()
                    .where {
                        (DB_Stats.identifier eq id) and
                            (DB_Stats.gametype eq gameType.name) and
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
    fun add(id: String, gameType: GameType, playerUuid: String, delta: Double): CompletableFuture<Boolean> {
        val key = StatsKey(id, gameType.name, playerUuid)

        cache.compute(key) { _, old -> (old ?: 0.0) + delta }

        synchronized(pendingLock) {
            pendingDeltas.merge(key, delta) { a, b -> a + b }
        }

        return CompletableFuture.completedFuture(true)
    }

    /**
     * Flush pending deltas to DB. Applies deltas using UPDATE ... SET value = value + delta;
     * inserts rows if they don't exist. Returns future that completes when flush finishes.
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
                                (DB_Stats.uuid eq key.uuid)
                        }) {
                            it[DB_Stats.value] = DB_Stats.value + delta
                        }

                        if (updated == 0) {
                            DB_Stats.insert {
                                it[DB_Stats.identifier] = key.id
                                it[DB_Stats.gametype] = key.gametype
                                it[DB_Stats.uuid] = key.uuid
                                it[DB_Stats.value] = delta
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // on failure, merge toFlush back into pendingDeltas so we don't lose changes
                synchronized(pendingLock) {
                    toFlush.forEach { (k, v) -> pendingDeltas.merge(k, v) { a, b -> a + b } }
                }
                false
            }
        }
    }

    /**
     * Start automatic periodic flush. Call from plugin onEnable.
     */
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

    /**
     * Stop auto flush and flush pending changes synchronously. Call from plugin onDisable.
     */
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
