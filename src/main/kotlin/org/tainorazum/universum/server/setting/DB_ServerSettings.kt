package org.tainorazum.universum.server.setting

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.Universum
// transaction is handled by DatabaseUtil.dba()
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DB_ServerSettings : Table("server_settings") {

    val setting = enumerationByName("setting", 32, ServerSetting::class)

    val value = varchar("value", 128)

    override val primaryKey = PrimaryKey(setting)

    // In-memory cache for settings
    private val cache = ConcurrentHashMap<ServerSetting, Any>()

    /**
     * Ensure that every enum entry exists in the database.
     * Note: this should be called inside an Exposed transaction (the caller is responsible for that).
     */
    fun ensureDefaults() {
        for (s in ServerSetting.entries) {
            val exists = DB_ServerSettings.select { setting eq s }.limit(1).any()
            if (!exists) {
                DB_ServerSettings.insert {
                    it[setting] = s
                    it[value] = s.default.toString()
                }
            }
        }
    }

    /**
     * Load all settings into cache from the database. Call once at startup.
     */
    fun loadAllSettingsIntoCache(): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    DB_ServerSettings.selectAll().forEach { row ->
                        val setting = row[DB_ServerSettings.setting]
                        val rawValue = row[DB_ServerSettings.value]
                        val typedValue = parseValue(setting, rawValue)
                        cache[setting] = typedValue
                    }
                }
                true
            } catch (e: Exception) {
                Universum.instance.logger.severe("Failed to load server settings into cache: $e")
                false
            }
        }
    }

    fun set(setting: ServerSetting, value: Any): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_ServerSettings.deleteWhere {
                    DB_ServerSettings.setting eq setting
                }
                try {
                    DB_ServerSettings.insert {
                        it[DB_ServerSettings.setting] = setting
                        it[DB_ServerSettings.value] = value.toString()
                    }

                    // update cache
                    cache[setting] = value

                    true
                } catch (e: Exception) {
                    Universum.instance.logger.severe("Failed to insert setting into database. $e")
                    false
                }
            }
        }
    }

    fun get(setting: ServerSetting): CompletableFuture<Any> {
        // cache hit
        val cached = cache[setting]
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        // cache miss: fetch from DB and cache
        return dba {
            val row = DB_ServerSettings.select { DB_ServerSettings.setting eq setting }
                .limit(1)
                .firstOrNull()

            val raw = row?.get(DB_ServerSettings.value) ?: setting.default.toString()

            try {
                val typedValue = parseValue(setting, raw)
                // update cache
                cache[setting] = typedValue
                typedValue
            } catch (e: Exception) {
                Universum.instance.logger.severe("Returning ServerSetting's default value. $e")
                cache[setting] = setting.default
                setting.default
            }
        }
    }

    /**
     * Invalidate cache for a specific setting or all if setting is null.
     */
    fun invalidateCache(setting: ServerSetting? = null) {
        if (setting == null) {
            cache.clear()
        } else {
            cache.remove(setting)
        }
    }

    /**
     * Parse a string value into the appropriate type based on the setting's default type.
     */
    private fun parseValue(setting: ServerSetting, raw: String): Any {
        return when (setting.default) {
            is Boolean -> raw.toBoolean()
            is Int -> raw.toInt()
            is Long -> raw.toLong()
            is Double -> raw.toDouble()
            is Float -> raw.toFloat()
            is String -> raw
            else -> raw
        }
    }

}