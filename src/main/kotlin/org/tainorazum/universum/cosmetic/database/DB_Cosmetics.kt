package org.tainorazum.universum.cosmetic.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticSerializer
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object DB_Cosmetics : Table("cosmetics") {

    val id = integer("id").autoIncrement()
    val type = enumerationByName<CosmeticType>("type", 32)
    val name = varchar("name", 60)
    val cosmetic = blob("cosmetic")

    override val primaryKey = PrimaryKey(id, type, name)

    fun getCosmetic(type: CosmeticType, name: String): CompletableFuture<CosmeticData?> {
        return dba {
            transaction {
                val blobValue = DB_Cosmetics.selectAll()
                    .where { (DB_Cosmetics.type eq type) and (DB_Cosmetics.name eq name) }
                    .map { it[DB_Cosmetics.cosmetic] }
                    .firstOrNull() ?: return@transaction null

                CosmeticSerializer.deserialize(type, blobValue.bytes)
            }
        }
    }

    fun saveCosmetic(type: CosmeticType, name: String, data: CosmeticData): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val serializedBytes = CosmeticSerializer.serialize(data)

                DB_Cosmetics.upsert(DB_Cosmetics.type, DB_Cosmetics.name) {
                    it[DB_Cosmetics.type] = type
                    it[DB_Cosmetics.name] = name
                    it[DB_Cosmetics.cosmetic] = ExposedBlob(serializedBytes)
                }
                true
            }
        }
    }
}