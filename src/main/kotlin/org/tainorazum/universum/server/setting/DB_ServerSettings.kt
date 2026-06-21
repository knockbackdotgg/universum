package org.tainorazum.universum.server.setting

import gg.flyte.twilight.data.sql.QueryBuilder.Companion.eq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.Universum
// transaction is handled by DatabaseUtil.dba()
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object DB_ServerSettings : Table("server_settings") {

    val setting = enumerationByName("setting", 32, ServerSetting::class)

    val value = varchar("value", 128)

    override val primaryKey = PrimaryKey(setting)

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
                    true
                } catch (e: Exception) {
                    Universum.instance.logger.severe("Failed to insert setting into database. $e")
                    false
                }
            }
        }
    }

    fun get(setting: ServerSetting): CompletableFuture<Any> {
        return dba {
            val row = DB_ServerSettings.select { DB_ServerSettings.setting eq setting }
                .limit(1)
                .firstOrNull()

            val raw = row?.get(DB_ServerSettings.value) ?: setting.default.toString()

            try {
                when (setting.default) {
                    is Boolean -> raw.toBoolean()
                    is Int -> raw.toInt()
                    is Long -> raw.toLong()
                    is Double -> raw.toDouble()
                    is Float -> raw.toFloat()
                    is String -> raw
                    else -> raw
                }
            } catch (e: Exception) {
                Universum.instance.logger.severe("Returning ServerSetting's default value. $e")
                setting.default
            }
        }
    }

}