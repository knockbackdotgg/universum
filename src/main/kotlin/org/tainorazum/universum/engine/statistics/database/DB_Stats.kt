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

object DB_Stats : Table("statistics") {

    val identifier = varchar("identifier", 100) // statistic identifier
    val gametype = varchar("gametype", 100)

    val uuid = varchar("uuid", 36)

    val value = double("value").default(0.0)

    override val primaryKey = PrimaryKey(identifier, gametype, uuid)

    fun get(id: String, gameType: GameType, playerUuid: String): CompletableFuture<Double> {
        return dba {
            transaction {
                DB_Stats.selectAll()
                    .where {
                        (DB_Stats.identifier eq id) and
                            (DB_Stats.gametype eq gameType.name) and
                            (DB_Stats.uuid eq playerUuid)
                    }
                    .limit(1)
                    .map { it[DB_Stats.value] }
                    .singleOrNull() ?: 0.0
            }
        }
    }

    fun set(id: String, gameType: GameType, playerUuid: String, value: Double): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Stats.deleteWhere {
                    (identifier eq id) and
                        (gametype eq gameType.name) and
                        (uuid eq playerUuid)
                }
                DB_Stats.insert {
                    it[DB_Stats.identifier] = id
                    it[DB_Stats.gametype] = gameType.name
                    it[DB_Stats.uuid] = playerUuid
                    it[DB_Stats.value] = value
                }
                true
            }
        }
    }

    fun add(
        id: String,
        gameType: GameType,
        playerUuid: String,
        delta: Double
    ): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val updated = DB_Stats.update({
                    (DB_Stats.identifier eq id) and
                        (DB_Stats.gametype eq gameType.name) and
                        (DB_Stats.uuid eq playerUuid)
                }) {
                    it[DB_Stats.value] = DB_Stats.value + delta
                }

                if (updated == 0) {
                    DB_Stats.insert {
                        it[DB_Stats.identifier] = id
                        it[DB_Stats.gametype] = gameType.name
                        it[DB_Stats.uuid] = playerUuid
                        it[DB_Stats.value] = delta
                    }
                }

                true
            }
        }
    }

}
