package org.tainorazum.universum.cosmetic.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.UUID
import java.util.concurrent.CompletableFuture

object DB_PlayerCosmetics : Table("player_cosmetics") {

    val uuid = varchar("uuid", 36)

    val type = enumerationByName<CosmeticType>("type", 32)
    val name = varchar("name", 60)
    val enabled = bool("enabled")

    override val primaryKey = PrimaryKey(uuid, type, name)

    fun isEnabled(uuid: UUID, type: CosmeticType, name: String): CompletableFuture<Boolean> {
        return dba {
            transaction {
                !DB_PlayerCosmetics.selectAll()
                    .where {
                        (DB_PlayerCosmetics.uuid eq uuid.toString()) and
                                (DB_PlayerCosmetics.type eq type) and
                                (DB_PlayerCosmetics.name eq name) and
                                (DB_PlayerCosmetics.enabled eq true)
                    }
                    .empty()
            }
        }
    }

    fun getEnabled(uuid: UUID, type: CosmeticType): CompletableFuture<String?> {
        return dba {
            transaction {
                DB_PlayerCosmetics.selectAll()
                    .where {
                        (DB_PlayerCosmetics.uuid eq uuid.toString()) and
                                (DB_PlayerCosmetics.type eq type) and
                                (DB_PlayerCosmetics.enabled eq true)
                    }
                    .map { row -> row[DB_PlayerCosmetics.name] }
                    .singleOrNull()
            }
        }
    }

}