package org.tainorazum.universum.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.database.example.EX_PlayerData
import java.util.concurrent.CompletableFuture

class DatabaseManager {

    // Changed to lateinit so your close() check works flawlessly,
    // or kept as a nullable/standard type safely. Let's make it lateinit.
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://localhost:3306/minecraft_db"
            username = "root"
            password = "password"
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
    }

    fun createTables() {
        create(EX_PlayerData)
    }

    fun create(table: Table) {
        CompletableFuture.runAsync {
            transaction {
                SchemaUtils.create(table)
            }
        }
    }

    fun close() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
        }
    }
}