package org.tainorazum.universum.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.Universum
import org.tainorazum.universum.coins.database.DB_Coins
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.database.example.EX_PlayerData
import org.tainorazum.universum.engine.arena.database.DB_ArenaLocations
import org.tainorazum.universum.engine.arena.database.DB_Arenas
import org.tainorazum.universum.engine.arena.database.DB_ArenaGametypes
import org.tainorazum.universum.engine.arena.database.DB_ArenaKits
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.friends.database.DB_Friends
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.kiteditor.editor.items.database.DB_KE_Items
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.punishment.database.DB_Punishments
import org.tainorazum.universum.server.server.DB_Server
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.visual.DB_VisualServer
import org.tainorazum.universum.setting.database.DB_PlayerSettings
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

class DatabaseManager {

    // Changed to lateinit so your close() check works flawlessly,
    // or kept as a nullable/standard type safely. Let's make it lateinit.
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = Universum.instance.config.getString("database.url")
            username = Universum.instance.config.getString("database.username")
            password = Universum.instance.config.getString("database.password")
            driverClassName = "com.mysql.cj.jdbc.Driver"

            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 10000
            maxLifetime = 1800000

            initializationFailTimeout = 1
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        createTables()

        DB_Stats.startAutoFlush(30)
    }

    fun create(table: Table) {
        transaction {
            SchemaUtils.create(table)
            // If we just created the server settings table, ensure every enum value exists with its default
            if (table === DB_ServerSettings) {
                DB_ServerSettings.ensureDefaults()
            }
        }
    }

    fun close() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
        }
    }

    fun createTables() {
        create(EX_PlayerData)
        create(DB_Arenas)
        create(DB_ArenaLocations)
        create(DB_Kits)
        create(DB_KE_Items)
        create(DB_ArenaGametypes)
        create(DB_ArenaKits)
        create(DB_Stats)
        create(DB_VisualServer)
        create(DB_Server)
        create(DB_ServerSettings)
        create(DB_Lobby)
        create(DB_Punishments)
        create(DB_PlayerSettings)
        create(DB_PlayerCosmetics)
        create(DB_Cosmetics)
        create(DB_KitEditor)
        create(DB_Friends)
        create(DB_Coins)
    }

}
