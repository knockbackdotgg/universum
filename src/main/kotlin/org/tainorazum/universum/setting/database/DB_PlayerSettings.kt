package org.tainorazum.universum.setting.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.tainorazum.universum.setting.manager.PlayerSetting
import org.tainorazum.universum.utility.database.DatabaseUtil
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DB_PlayerSettings : Table("player_settings") {

    val uuid = varchar("uuid", 36)
    val setting = enumerationByName<PlayerSetting>("setting", 50)
    val value = text("value")

    override val primaryKey = PrimaryKey(uuid, setting)

    // Thread-safe nested cache: UUID -> Map<PlayerSetting, ValueString>
    private val cachedPlayerSettings = ConcurrentHashMap<UUID, ConcurrentHashMap<PlayerSetting, String>>()

    /**
     * Pre-loads and caches all settings for a player upon joining.
     * Guarantees default values are initialized and stored in the DB if missing.
     */
    fun cacheAllSettings(uuid: UUID): CompletableFuture<Unit> {
        return DatabaseUtil.dba {
            transaction {
                // Fetch all saved settings for the player from the database
                val dbRows = DB_PlayerSettings.selectAll()
                    .where { DB_PlayerSettings.uuid eq uuid.toString() }
                    .associate { row -> row[setting] to row[value] }

                val playerCache = ConcurrentHashMap<PlayerSetting, String>()

                // Ensure every setting enum constant has a cached value
                for (settingEnum in PlayerSetting.entries) {
                    val existingValue = dbRows[settingEnum]

                    if (existingValue != null) {
                        playerCache[settingEnum] = existingValue
                    } else {
                        // Set missing default setting in DB and cache
                        val defaultValue = settingEnum.options.firstOrNull() ?: ""

                        // Fixed: Removed explicit key arguments for MySQL dialect compatibility
                        DB_PlayerSettings.upsert {
                            it[DB_PlayerSettings.uuid] = uuid.toString()
                            it[DB_PlayerSettings.setting] = settingEnum
                            it[DB_PlayerSettings.value] = defaultValue
                        }

                        playerCache[settingEnum] = defaultValue
                    }
                }

                cachedPlayerSettings[uuid] = playerCache
            }
        }
    }

    /**
     * Gets a setting for a player.
     * Instant lookup if pre-cached on join via [cacheAllSettings].
     */
    fun getSetting(uuid: UUID, setting: PlayerSetting): CompletableFuture<String> {
        val playerCache = cachedPlayerSettings[uuid]
        val cachedValue = playerCache?.get(setting)

        if (cachedValue != null) {
            return CompletableFuture.completedFuture(cachedValue)
        }

        // Fallback if player settings weren't pre-cached
        return DatabaseUtil.dba {
            transaction {
                val existingValue = DB_PlayerSettings.selectAll()
                    .where {
                        (DB_PlayerSettings.uuid eq uuid.toString()) and
                                (DB_PlayerSettings.setting eq setting)
                    }
                    .map { row -> row[value] }
                    .singleOrNull()

                val resultValue = if (existingValue != null) {
                    existingValue
                } else {
                    val defaultValue = setting.options.firstOrNull() ?: ""

                    // Fixed: Removed explicit key arguments for MySQL dialect compatibility
                    DB_PlayerSettings.upsert {
                        it[DB_PlayerSettings.uuid] = uuid.toString()
                        it[DB_PlayerSettings.setting] = setting
                        it[DB_PlayerSettings.value] = defaultValue
                    }

                    defaultValue
                }

                cachedPlayerSettings
                    .computeIfAbsent(uuid) { ConcurrentHashMap() }[setting] = resultValue

                resultValue
            }
        }
    }

    /**
     * Saves or updates a setting value for a given player and updates the cache.
     */
    fun setSetting(uuid: UUID, setting: PlayerSetting, value: String): CompletableFuture<Boolean> {
        return DatabaseUtil.dba {
            transaction {
                // Fixed: Removed explicit key arguments for MySQL dialect compatibility
                DB_PlayerSettings.upsert {
                    it[DB_PlayerSettings.uuid] = uuid.toString()
                    it[DB_PlayerSettings.setting] = setting
                    it[DB_PlayerSettings.value] = value
                }

                cachedPlayerSettings
                    .computeIfAbsent(uuid) { ConcurrentHashMap() }[setting] = value

                true
            }
        }
    }

    /**
     * Removes player settings from memory on logout to prevent memory leaks.
     */
    fun invalidateCache(uuid: UUID) {
        cachedPlayerSettings.remove(uuid)
    }
}