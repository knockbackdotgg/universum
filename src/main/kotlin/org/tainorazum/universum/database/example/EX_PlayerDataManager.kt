package org.tainorazum.universum.database.example

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.util.concurrent.CompletableFuture

class EX_PlayerDataManager {

    fun setPlayerCoins(uuid: UUID, newCoins: Int): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            transaction {
                EX_PlayerData.upsert {
                    it[EX_PlayerData.uuid] = uuid.toString()
                    it[EX_PlayerData.coins] = newCoins
                }
            }
        }
    }

    fun getPlayerCoins(uuid: UUID): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            transaction {
                EX_PlayerData.selectAll()
                    .where { EX_PlayerData.uuid eq uuid.toString() }
                    .map { it[EX_PlayerData.coins] }
                    .singleOrNull() ?: 0
            }
        }
    }

}