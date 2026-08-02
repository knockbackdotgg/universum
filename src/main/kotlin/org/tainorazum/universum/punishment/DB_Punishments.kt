package org.tainorazum.universum.punishment

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.time.Duration
import java.util.Date
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.time.Instant

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

    fun punish(punishment: Punishment): CompletableFuture<Boolean> {
        return dba {
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
        }
    }

    /**
     * @usage Unpunish/archive a player's punishment
     */
    fun unPunish(uuid: UUID, id: Int): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Punishments.update(
                    {
                        DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.id eq id)
                    }
                ) {
                    it[archived] = true
                }
                true
            }
        }
    }

    fun archivePunishment(id: Int): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Punishments.update(
                    {
                        DB_Punishments.id eq id
                    }
                ) {
                    it[archived] = true
                }
                true
            }
        }
    }

    fun removePunishment(uuid: UUID, id: Int): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Punishments.deleteWhere {
                    DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.id eq id)
                }
                true
            }
        }
    }

    fun getPunishments(): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun getPunishment(uuid: UUID, id: Int): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.id eq id) }
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun getPunishments(uuid: UUID, type: PunishmentType): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.type eq type) }
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun getPunishments(uuid: UUID, punisherUuid: UUID): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.punisherUuid eq punisherUuid.toString()) }
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun getPunishments(uuid: UUID, archived: Boolean): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { DB_Punishments.playerUuid eq uuid.toString() and (DB_Punishments.archived eq archived) }
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun getPunishments(uuid: UUID): CompletableFuture<List<Punishment>> {
        return dba {
            transaction {
                DB_Punishments.selectAll()
                    .where { DB_Punishments.playerUuid eq uuid.toString() }
                    .map { row ->
                        toPunishment(row)
                    }
            }
        }
    }

    fun toPunishment(row: ResultRow): Punishment {
        val rawMillis = row[DB_Punishments.duration]

        val p = Punishment(
            id = row[DB_Punishments.id],
            uuid = UUID.fromString(row[DB_Punishments.playerUuid]),
            type = row[DB_Punishments.type],
            reason = row[DB_Punishments.reason],
            duration = rawMillis?.let { Duration.ofMillis(it) },
            punishedOn = Date(row[DB_Punishments.punishedOn]),
            punisherUuid = UUID.fromString(row[DB_Punishments.punisherUuid]),
            archived = row[DB_Punishments.archived]
        )
        checkActive(p)
        return p
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
        val isStillActive = java.time.Instant.now().isBefore(expirationInstant)

        if (!isStillActive) {
            return archivePunishment(punishment.id!!).thenApply { false }
        }

        return CompletableFuture.completedFuture(true)
    }

}