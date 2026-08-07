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
import java.util.concurrent.ConcurrentHashMap

object DB_Cosmetics : Table("cosmetics") {

    val id = integer("id").autoIncrement()
    val type = enumerationByName<CosmeticType>("type", 32)
    val name = varchar("name", 60)
    val cosmetic = blob("cosmetic")

    override val primaryKey = PrimaryKey(id, type, name)

    // Cache: Maps Pair(CosmeticType, Name) -> CompletableFuture<CosmeticData?>
    private val cache = ConcurrentHashMap<Pair<CosmeticType, String>, CompletableFuture<CosmeticData?>>()

    // =========================================================================
    // Cache Management & Lifecycle
    // =========================================================================

    /**
     * Removes a specific cosmetic from local RAM cache.
     */
    fun invalidate(type: CosmeticType, name: String) {
        cache.remove(type to name)
    }

    /**
     * Clears the entire cosmetic definition cache.
     */
    fun invalidateAll() {
        cache.clear()
    }

    /**
     * Optional: Warm up cache on plugin load by loading all cosmetics from DB into RAM.
     */
    fun preloadAll(): CompletableFuture<Unit> {
        return dba {
            transaction {
                DB_Cosmetics.selectAll().forEach { row ->
                    val cosmeticType = row[DB_Cosmetics.type]
                    val cosmeticName = row[DB_Cosmetics.name]
                    val blobValue = row[DB_Cosmetics.cosmetic]

                    val data = CosmeticSerializer.deserialize(cosmeticType, blobValue.bytes)
                    if (data != null) {
                        cache[cosmeticType to cosmeticName] = CompletableFuture.completedFuture(data)
                    }
                }
            }
        }
    }

    // =========================================================================
    // Retrieval Methods (Serves From Cache)
    // =========================================================================

    /**
     * Retrieves cosmetic data from RAM cache if present, otherwise fetches from DB and deserializes.
     */
    fun getCosmetic(type: CosmeticType, name: String): CompletableFuture<CosmeticData?> {
        val key = type to name

        return cache.computeIfAbsent(key) { (t, n) ->
            val future = fetchCosmeticFromDb(t, n)

            // Do not permanently cache null results or failed futures
            future.whenComplete { result, throwable ->
                if (throwable != null || result == null) {
                    cache.remove(key)
                }
            }

            future
        }
    }

    private fun fetchCosmeticFromDb(type: CosmeticType, name: String): CompletableFuture<CosmeticData?> {
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

    // =========================================================================
    // Mutator Methods (Updates Cache On Success)
    // =========================================================================

    /**
     * Saves or updates a cosmetic in the database and updates the RAM cache directly.
     */
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
        }.thenApply { success ->
            if (success) {
                // Immediately update RAM cache with updated object
                cache[type to name] = CompletableFuture.completedFuture(data)
            }
            success
        }
    }
}