package org.tainorazum.universum.coins.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
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
}