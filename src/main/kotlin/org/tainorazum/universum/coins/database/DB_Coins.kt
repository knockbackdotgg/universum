package org.tainorazum.universum.coins.database

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.UUID
import java.util.concurrent.CompletableFuture

object DB_Coins : Table("coins") {

    val uuid = varchar("uuid", 36)
    val amount = integer("amount").default(0)

    override val primaryKey = PrimaryKey(uuid)

    /**
     * Fetches coin count directly from the database asynchronously.
     */
    fun getCoins(playerUuid: UUID): CompletableFuture<Int> {
        return dba {
            transaction {
                selectAll()
                    .where { uuid eq playerUuid.toString() }
                    .map { it[amount] }
                    .firstOrNull() ?: 0
            }
        }
    }

    /**
     * Upserts coin count to the database asynchronously.
     */
    fun setCoins(playerUuid: UUID, newAmount: Int): CompletableFuture<Boolean> {
        return dba {
            transaction {
                upsert {
                    it[uuid] = playerUuid.toString()
                    it[amount] = newAmount.coerceAtLeast(0)
                }
                true
            }
        }
    }

    fun getTopCoins(limit: Int = 10): CompletableFuture<List<DB_Stats.LeaderboardEntry>> {
        return dba {
            transaction {
                selectAll()
                    .orderBy(amount to SortOrder.DESC)
                    .limit(limit)
                    .map { DB_Stats.LeaderboardEntry(it[uuid], it[amount].toDouble()) }
            }
        }
    }

    /**
     * Adds (or subtracts) coins for a player asynchronously.
     * Automatically handles players that do not have a database entry yet.
     */
    fun addCoins(playerUuid: UUID, amountToAdd: Int): CompletableFuture<Int> {
        return dba {
            transaction {
                val currentAmount = selectAll()
                    .where { uuid eq playerUuid.toString() }
                    .map { it[amount] }
                    .firstOrNull() ?: 0

                val newAmount = (currentAmount + amountToAdd).coerceAtLeast(0)

                upsert {
                    it[uuid] = playerUuid.toString()
                    it[amount] = newAmount
                }
                newAmount
            }
        }
    }
}