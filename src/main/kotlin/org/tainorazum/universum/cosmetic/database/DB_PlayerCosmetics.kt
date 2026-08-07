package org.tainorazum.universum.cosmetic.database

import net.kyori.adventure.text.Component
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DB_PlayerCosmetics : Table("player_cosmetics") {

    val uuid = varchar("uuid", 36)
    val type = enumerationByName<CosmeticType>("type", 32)
    val name = varchar("name", 60)
    val enabled = bool("enabled")

    override val primaryKey = PrimaryKey(uuid, type, name)

    // =========================================================================
    // Cache Model & Storage
    // =========================================================================

    data class CachedPlayerCosmetics(
        // Maps CosmeticType to the currently enabled cosmetic name
        val enabledNames: Map<CosmeticType, String>,
        // Set of (Type, Name) pairs currently marked enabled
        val enabledPairs: Set<Pair<CosmeticType, String>>
    )

    private val cache = ConcurrentHashMap<UUID, CompletableFuture<CachedPlayerCosmetics>>()

    // =========================================================================
    // Cache Management & Lifecycle
    // =========================================================================

    /**
     * Purges local RAM cache for a specific player UUID.
     */
    fun invalidate(uuid: UUID) {
        cache.remove(uuid)
    }

    /**
     * Purges the entire player cosmetics RAM cache.
     */
    fun invalidateAll() {
        cache.clear()
    }

    fun getPlayerCosmetics(uuid: UUID): CompletableFuture<CachedPlayerCosmetics> {
        return cache.computeIfAbsent(uuid) { id ->
            val future = fetchPlayerCosmeticsFromDb(id)

            // Remove from cache if network/DB fetch fails so retries are allowed
            future.whenComplete { _, throwable ->
                if (throwable != null) {
                    cache.remove(id)
                }
            }

            future
        }
    }

    private fun fetchPlayerCosmeticsFromDb(uuid: UUID): CompletableFuture<CachedPlayerCosmetics> {
        return dba {
            transaction {
                val enabledMap = mutableMapOf<CosmeticType, String>()
                val enabledSet = mutableSetOf<Pair<CosmeticType, String>>()

                DB_PlayerCosmetics.selectAll()
                    .where {
                        (DB_PlayerCosmetics.uuid eq uuid.toString()) and
                                (DB_PlayerCosmetics.enabled eq true)
                    }
                    .forEach { row ->
                        val cosmeticType = row[DB_PlayerCosmetics.type]
                        val cosmeticName = row[DB_PlayerCosmetics.name]

                        enabledMap[cosmeticType] = cosmeticName
                        enabledSet.add(cosmeticType to cosmeticName)
                    }

                CachedPlayerCosmetics(enabledMap, enabledSet)
            }
        }
    }

    // =========================================================================
    // Retrieval Methods (Serves From Cache)
    // =========================================================================

    /**
     * Retrieves the active cosmetic payload/object for a player.
     * Uses cached active cosmetic name to avoid unnecessary database reads.
     */
    fun get(uuid: UUID, type: CosmeticType): CompletableFuture<Any?> {
        return getEnabled(uuid, type).thenCompose { enabledName ->
            if (enabledName == null) {
                CompletableFuture.completedFuture(null)
            } else {
                DB_Cosmetics.getCosmetic(type, enabledName).thenApply { data ->
                    when (data) {
                        is CosmeticData.Tag -> {
                            Component.text("[", Colours.DARK_GREY)
                                .append(data.component)
                                .append(Component.text("] ", Colours.DARK_GREY))
                        }

                        is CosmeticData.Pet -> data

                        is CosmeticData.ArmorTrim -> data.material

                        is CosmeticData.ArmorTrimColor -> data.material

                        is CosmeticData.Trail -> data

                        is CosmeticData.Aura -> data

                        null -> {
                            println("[Cosmetics] Enabled cosmetic '$enabledName' not found in database for $uuid.")
                            null
                        }
                    }
                }
            }
        }
    }

    fun isEnabled(uuid: UUID, type: CosmeticType, name: String): CompletableFuture<Boolean> {
        return getPlayerCosmetics(uuid).thenApply { cached ->
            cached.enabledPairs.contains(type to name)
        }
    }

    fun getEnabled(uuid: UUID, type: CosmeticType): CompletableFuture<String?> {
        return getPlayerCosmetics(uuid).thenApply { cached ->
            cached.enabledNames[type]
        }
    }

    // =========================================================================
    // Data Ingestion & Mutators (Invalidates Cache On Success)
    // =========================================================================

    /**
     * Enables a specific cosmetic for a player and disables any other active cosmetic of that type.
     */
    fun enableCosmetic(uuid: UUID, type: CosmeticType, name: String): CompletableFuture<Boolean> {
        return dba {
            transaction {
                // 1. Disable current active cosmetic for this type
                DB_PlayerCosmetics.update(
                    { (DB_PlayerCosmetics.uuid eq uuid.toString()) and (DB_PlayerCosmetics.type eq type) }
                ) {
                    it[enabled] = false
                }

                // 2. Enable target cosmetic
                val updatedRows = DB_PlayerCosmetics.update(
                    {
                        (DB_PlayerCosmetics.uuid eq uuid.toString()) and
                                (DB_PlayerCosmetics.type eq type) and
                                (DB_PlayerCosmetics.name eq name)
                    }
                ) {
                    it[enabled] = true
                }

                // 3. Insert row if it didn't exist before
                if (updatedRows == 0) {
                    DB_PlayerCosmetics.insert {
                        it[DB_PlayerCosmetics.uuid] = uuid.toString()
                        it[DB_PlayerCosmetics.type] = type
                        it[DB_PlayerCosmetics.name] = name
                        it[DB_PlayerCosmetics.enabled] = true
                    }
                }
                true
            }
        }.thenApply { success ->
            if (success) {
                invalidate(uuid) // Purge RAM cache so the change takes effect immediately
            }
            success
        }
    }

    /**
     * Disables any currently enabled cosmetic of a given type for a player.
     */
    fun disableCosmetic(uuid: UUID, type: CosmeticType): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_PlayerCosmetics.update(
                    { (DB_PlayerCosmetics.uuid eq uuid.toString()) and (DB_PlayerCosmetics.type eq type) }
                ) {
                    it[enabled] = false
                } > 0
            }
        }.thenApply { success ->
            if (success) {
                invalidate(uuid) // Purge RAM cache so the change takes effect immediately
            }
            success
        }
    }
}