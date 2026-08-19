package org.tainorazum.universum.punishment.database

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.punishment.manager.Punishment
import org.tainorazum.universum.punishment.manager.PunishmentType
import org.tainorazum.universum.utility.database.DatabaseUtil
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DB_Punishments : Table("punishments") {

    val id = integer("id").autoIncrement()
    val playerUuid = varchar("player_uuid", 36)
    val type = enumerationByName<PunishmentType>("type", 16)
    val reason = text("reason")

    // Nullable long column for Duration (null or -1 for permanent)
    val duration = long("duration").nullable()

    // Long column for Date/Timestamp (Epoch milliseconds)
    val punishedOn = long("punished_on")

    val punisherUuid = varchar("punisher_uuid", 36)
    val archived = bool("archived")

    override val primaryKey = PrimaryKey(id)

    // Local player punishment cache: UUID -> CompletableFuture<List<Punishment>>
    private val cache = ConcurrentHashMap<UUID, CompletableFuture<List<Punishment>>>()

    // =========================================================================
    // Cache Management & Lifecycle
    // =========================================================================

    /**
     * Clears local cache for a specific player UUID.
     */
    fun invalidate(uuid: UUID) {
        cache.remove(uuid)
    }

    /**
     * Clears the entire local punishment cache.
     */
    fun invalidateAll() {
        cache.clear()
    }

    // =========================================================================
    // Data Ingestion & Mutation (Invalidates Cache On Success)
    // =========================================================================

    fun punish(punishment: Punishment): CompletableFuture<Boolean> {
        return DatabaseUtil.dba {
            transaction {
                DB_Punishments.insert {
                    it[playerUuid] = punishment.uuid.toString()
                    it[type] = punishment.type
                    it[reason] = punishment.reason
                    it[duration] = punishment.duration?.toMillis()
                    it[punishedOn] = punishment.punishedOn.time
                    it[punisherUuid] = punishment.punisherUuid.toString()
                    it[archived] = punishment.archived
                }
                true
            }
        }.thenApply { success ->
            if (success) {
                invalidate(punishment.uuid) // Invalidate cache so new punishment appears immediately
            }
            success
        }
    }

    /**
     * @usage Unpunish/archive a player's punishment
     */
    fun unPunish(uuid: UUID, id: Int): CompletableFuture<Boolean> {
        return DatabaseUtil.dba {
            transaction {
                DB_Punishments.update(
                    {
                        playerUuid eq uuid.toString() and (DB_Punishments.id eq id)
                    }
                ) {
                    it[archived] = true
                } > 0
            }
        }.thenApply { updated ->
            if (updated) {
                invalidate(uuid)
            }
            updated
        }
    }

    fun archivePunishment(id: Int): CompletableFuture<Boolean> {
        return DatabaseUtil.dba {
            transaction {
                // Find target UUID first to selectively invalidate their cache
                val targetUuid = DB_Punishments.selectAll()
                    .where { DB_Punishments.id eq id }
                    .singleOrNull()?.get(playerUuid)

                val updatedCount = DB_Punishments.update(
                    { DB_Punishments.id eq id }
                ) {
                    it[archived] = true
                }

                targetUuid to (updatedCount > 0)
            }
        }.thenApply { (uuidStr, success) ->
            if (success && uuidStr != null) {
                invalidate(UUID.fromString(uuidStr))
            }
            success
        }
    }

    fun removePunishment(uuid: UUID, id: Int): CompletableFuture<Boolean> {
        return DatabaseUtil.dba {
            transaction {
                DB_Punishments.deleteWhere {
                    DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.id eq id)
                } > 0
            }
        }.thenApply { deleted ->
            if (deleted) {
                invalidate(uuid)
            }
            deleted
        }
    }

    // =========================================================================
    // Retrieval Methods (Serves From Cache)
    // =========================================================================

    /**
     * Gets all punishments for a player UUID. Serves from local RAM cache if available.
     */
    fun getPunishments(uuid: UUID): CompletableFuture<List<Punishment>> {
        return cache.computeIfAbsent(uuid) { id ->
            val future = fetchPlayerPunishmentsFromDb(id)

            // Remove failed future from cache so retries are allowed
            future.whenComplete { _, throwable ->
                if (throwable != null) {
                    cache.remove(id)
                }
            }

            future
        }.thenCompose { list ->
            // Passively check if any active punishments in cache have expired over time
            var expiredAny = false
            for (p in list) {
                if (!p.archived && isExpired(p)) {
                    expiredAny = true
                    archivePunishment(p.id!!) // Update DB & invalidate cache asynchronously
                }
            }

            if (expiredAny) {
                // Return fresh DB query if expired punishments were auto-archived
                fetchPlayerPunishmentsFromDb(uuid)
            } else {
                CompletableFuture.completedFuture(list)
            }
        }
    }

    fun getPunishment(uuid: UUID, id: Int): CompletableFuture<List<Punishment>> {
        return getPunishments(uuid).thenApply { list ->
            list.filter { it.id == id }
        }
    }

    fun getPunishments(uuid: UUID, type: PunishmentType): CompletableFuture<List<Punishment>> {
        return getPunishments(uuid).thenApply { list ->
            list.filter { it.type == type }
        }
    }

    fun getPunishments(uuid: UUID, punisherUuid: UUID): CompletableFuture<List<Punishment>> {
        return getPunishments(uuid).thenApply { list ->
            list.filter { it.punisherUuid == punisherUuid }
        }
    }

    fun getPunishments(uuid: UUID, archived: Boolean): CompletableFuture<List<Punishment>> {
        return getPunishments(uuid).thenApply { list ->
            list.filter { it.archived == archived }
        }
    }

    /**
     * Bypasses cache to fetch all global punishments across all players.
     */
    fun getPunishments(): CompletableFuture<List<Punishment>> {
        return DatabaseUtil.dba {
            transaction {
                DB_Punishments.selectAll()
                    .map { row -> toPunishment(row) }
            }
        }
    }

    // =========================================================================
    // Helpers & Expiration
    // =========================================================================

    private fun fetchPlayerPunishmentsFromDb(uuid: UUID): CompletableFuture<List<Punishment>> {
        return DatabaseUtil.dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { playerUuid eq uuid.toString() }
                    .map { row -> toPunishment(row) }
            }
        }
    }

    fun toPunishment(row: ResultRow): Punishment {
        val rawMillis = row[duration]

        return Punishment(
            id = row[id],
            uuid = UUID.fromString(row[playerUuid]),
            type = row[type],
            reason = row[reason],
            duration = rawMillis?.let { Duration.ofMillis(it) },
            punishedOn = Date(row[punishedOn]),
            punisherUuid = UUID.fromString(row[punisherUuid]),
            archived = row[archived]
        )
    }

    private fun isExpired(punishment: Punishment): Boolean {
        if (punishment.archived) return false
        val duration = punishment.duration ?: return false
        val expirationInstant = punishment.punishedOn.toInstant().plus(duration)
        return Instant.now().isAfter(expirationInstant)
    }

    /**
     * @usage Checks if a punishment is active, if not it gets archived.
     */
    fun checkActive(punishment: Punishment): CompletableFuture<Boolean> {
        if (punishment.archived) {
            return CompletableFuture.completedFuture(false)
        }
        val duration = punishment.duration
            ?: return CompletableFuture.completedFuture(true)

        val expirationInstant = punishment.punishedOn.toInstant().plus(duration)
        val isStillActive = Instant.now().isBefore(expirationInstant)

        if (!isStillActive) {
            return archivePunishment(punishment.id!!).thenApply { false }
        }

        return CompletableFuture.completedFuture(true)
    }
}