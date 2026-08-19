package org.tainorazum.universum.cosmetic.database

import org.bukkit.Material
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticSerializer
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

data class CosmeticEntry(
    val type: CosmeticType,
    val name: String,
    val material: Material = Material.STONE,
    val description: String? = null,
    val price: Int = 0,
    val data: CosmeticData? = null
)

object DB_Cosmetics : Table("cosmetics") {

    val id = integer("id").autoIncrement()
    val type = enumerationByName<CosmeticType>("type", 32)
    val name = varchar("name", 60)
    val material = enumerationByName<Material>("material", 50).default(Material.STONE)
    val description = varchar("description", 255).nullable()
    val price = integer("price")
    val cosmetic = blob("cosmetic")

    override val primaryKey = PrimaryKey(id, type, name)

    // Cache: Maps Pair(CosmeticType, Name) -> CompletableFuture<CosmeticEntry?>
    private val cache = ConcurrentHashMap<Pair<CosmeticType, String>, CompletableFuture<CosmeticEntry?>>()

    // =========================================================================
    // Cache Management & Lifecycle
    // =========================================================================

    fun invalidate(type: CosmeticType, name: String) {
        cache.remove(type to name)
    }

    fun invalidateAll() {
        cache.clear()
    }

    fun preloadAll(): CompletableFuture<Unit> {
        return dba {
            transaction {
                DB_Cosmetics.selectAll().forEach { row ->
                    val cosmeticType = row[DB_Cosmetics.type]
                    val cosmeticName = row[DB_Cosmetics.name]
                    val mat = row[DB_Cosmetics.material]
                    val desc = row[DB_Cosmetics.description]
                    val cost = row[DB_Cosmetics.price]
                    val blobValue = row[DB_Cosmetics.cosmetic]

                    val data = CosmeticSerializer.deserialize(cosmeticType, blobValue.bytes)
                    val entry = CosmeticEntry(cosmeticType, cosmeticName, mat, desc, cost, data)
                    cache[cosmeticType to cosmeticName] = CompletableFuture.completedFuture(entry)
                }
            }
        }
    }

    // =========================================================================
    // Retrieval Methods
    // =========================================================================

    fun getCosmeticEntry(type: CosmeticType, name: String): CompletableFuture<CosmeticEntry?> {
        val key = type to name

        return cache.computeIfAbsent(key) { (t, n) ->
            val future = fetchCosmeticEntryFromDb(t, n)

            future.whenComplete { result, throwable ->
                if (throwable != null || result == null) {
                    cache.remove(key)
                }
            }

            future
        }
    }

    fun getCosmetic(type: CosmeticType, name: String): CompletableFuture<CosmeticData?> {
        return getCosmeticEntry(type, name).thenApply { it?.data }
    }

    /**
     * Fetches all cosmetics matching a given CosmeticType.
     */
    fun getCosmeticsByType(type: CosmeticType): CompletableFuture<List<CosmeticEntry>> {
        return dba {
            transaction {
                DB_Cosmetics.selectAll()
                    .where { DB_Cosmetics.type eq type }
                    .map { row ->
                        val cosmeticName = row[DB_Cosmetics.name]
                        val mat = row[DB_Cosmetics.material]
                        val desc = row[DB_Cosmetics.description]
                        val cost = row[DB_Cosmetics.price]
                        val blobValue = row[DB_Cosmetics.cosmetic]
                        val data = CosmeticSerializer.deserialize(type, blobValue.bytes)

                        CosmeticEntry(type, cosmeticName, mat, desc, cost, data)
                    }
            }
        }
    }

    private fun fetchCosmeticEntryFromDb(type: CosmeticType, name: String): CompletableFuture<CosmeticEntry?> {
        return dba {
            transaction {
                val row = DB_Cosmetics.selectAll()
                    .where { (DB_Cosmetics.type eq type) and (DB_Cosmetics.name eq name) }
                    .firstOrNull() ?: return@transaction null

                val mat = row[DB_Cosmetics.material]
                val desc = row[DB_Cosmetics.description]
                val cost = row[DB_Cosmetics.price]
                val blobValue = row[DB_Cosmetics.cosmetic]
                val data = CosmeticSerializer.deserialize(type, blobValue.bytes)

                CosmeticEntry(type, name, mat, desc, cost, data)
            }
        }
    }

    // =========================================================================
    // Mutator Methods
    // =========================================================================

    /**
     * Upserts an entire CosmeticEntry into the database and updates RAM cache.
     */
    fun saveCosmetic(entry: CosmeticEntry): CompletableFuture<Boolean> {
        return dba {
            transaction {
                val serializedBytes = entry.data?.let { CosmeticSerializer.serialize(it) } ?: ByteArray(0)

                DB_Cosmetics.upsert(DB_Cosmetics.type, DB_Cosmetics.name) {
                    it[DB_Cosmetics.type] = entry.type
                    it[DB_Cosmetics.name] = entry.name
                    it[DB_Cosmetics.material] = entry.material
                    it[DB_Cosmetics.description] = entry.description
                    it[DB_Cosmetics.price] = entry.price
                    it[DB_Cosmetics.cosmetic] = ExposedBlob(serializedBytes)
                }
                true
            }
        }.thenApply { success ->
            if (success) {
                cache[entry.type to entry.name] = CompletableFuture.completedFuture(entry)
            }
            success
        }
    }

    /**
     * Adjusts only the price of a cosmetic.
     */
    fun updatePrice(type: CosmeticType, name: String, newPrice: Int): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Cosmetics.update({ (DB_Cosmetics.type eq type) and (DB_Cosmetics.name eq name) }) {
                    it[price] = newPrice
                } > 0
            }
        }.thenApply { success ->
            if (success) invalidate(type, name)
            success
        }
    }

    /**
     * Adjusts only the description of a cosmetic.
     */
    fun updateDescription(type: CosmeticType, name: String, newDescription: String?): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Cosmetics.update({ (DB_Cosmetics.type eq type) and (DB_Cosmetics.name eq name) }) {
                    it[description] = newDescription
                } > 0
            }
        }.thenApply { success ->
            if (success) invalidate(type, name)
            success
        }
    }

    /**
     * Adjusts only the material icon of a cosmetic.
     */
    fun updateMaterial(type: CosmeticType, name: String, newMaterial: Material): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Cosmetics.update({ (DB_Cosmetics.type eq type) and (DB_Cosmetics.name eq name) }) {
                    it[material] = newMaterial
                } > 0
            }
        }.thenApply { success ->
            if (success) invalidate(type, name)
            success
        }
    }
}