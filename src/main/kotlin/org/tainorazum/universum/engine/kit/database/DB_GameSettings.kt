package org.tainorazum.universum.engine.kit.database


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object DB_GameSettings : Table("kits_settings") {

    val kitId = varchar("kit_id", 100)
    val settingsJson = text("settings_json")

    override val primaryKey = PrimaryKey(kitId)


    fun get(
        kitId: String
    ): CompletableFuture<GameSettings> {

        return dba {
            transaction {
                val json = select(settingsJson)
                    .where { DB_GameSettings.kitId eq kitId }
                    .limit(1)
                    .firstOrNull()
                    ?.get(settingsJson)

                if (json == null) {
                    GameSettings()
                } else {
                    GameSettingsSerializer.deserialize(json)
                }
            }
        }
    }


    fun save(
        kitId: String,
        settings: GameSettings
    ): CompletableFuture<Boolean> {

        return dba {
            transaction {

                val json = GameSettingsSerializer.serialize(settings)

                deleteWhere {
                    DB_GameSettings.kitId eq kitId
                }

                insert {
                    it[DB_GameSettings.kitId] = kitId
                    it[DB_GameSettings.settingsJson] = json
                }

                true
            }
        }
    }


    fun reset(
        kitId: String
    ): CompletableFuture<Boolean> {

        return dba {
            transaction {
                deleteWhere {
                    DB_GameSettings.kitId eq kitId
                } > 0
            }
        }
    }
}