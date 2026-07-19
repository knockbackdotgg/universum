package org.tainorazum.universum

import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.time.TimeUnit
import gg.flyte.twilight.twilight
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.game.GameManager
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.lobby.LobbyManager
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_GameRequest
import org.tainorazum.universum.server.server.Server
import org.tainorazum.universum.server.server.ServerManager
import org.tainorazum.universum.server.server.ServerType
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.setting.ServerSetting
import org.tainorazum.universum.server.visual.VisualServerManager
import revxrsal.commands.bukkit.BukkitLamp
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set
        lateinit var server: Server

        var dbm: DatabaseManager? = null
            private set

        // all servers
        lateinit var adbm : ArenaDatabaseManager private set
        lateinit var arenaManager: ArenaManager private set
        lateinit var gameManager: GameManager private set
        lateinit var serverManager: ServerManager private set
        lateinit var visualServerManager: VisualServerManager private set

        // lobby servers
        lateinit var lobbyManager: LobbyManager private set
    }

    override fun onEnable() {

        server.setWhitelist(true)
        Bukkit.getOnlinePlayers().forEach { p ->
            p.kick()
        }

        // server
        instance = this
        saveDefaultConfig()

        // dependencies & utils
        twilight(this)
        val lamp = BukkitLamp.builder(this).build()

        // redis
        DB_ServerSettings.set(ServerSetting.REDIS, config.get("redis.enabled") ?: false)
        DB_ServerSettings.get(ServerSetting.REDIS).thenAccept { enabled ->
            if(enabled.toString().toBoolean()) {
                RedisManager.init()
                RedisSubscriber.start(this)
                RM_GameRequest()
            }
        }

        // database
        try {
            dbm = DatabaseManager()
            dbm?.init()
            logger.info("Successfully connected to the database pool.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize database connection! Disabling plugin...", e)
            server.pluginManager.disablePlugin(this)
            return
        }

        // managers
        lobbyManager = LobbyManager()
        adbm = ArenaDatabaseManager()
        arenaManager = ArenaManager()
        gameManager = GameManager()
        serverManager = ServerManager()
        visualServerManager = VisualServerManager()

        // delay database-dependent initialization by a small amount to ensure tables are created
        delay(100) {
            // load server settings and visual data into cache
            try {
                DB_ServerSettings.loadAllSettingsIntoCache().get()
                logger.info("Successfully loaded server settings into cache.")
            } catch (e: Exception) {
                logger.warning("Failed to load server settings into cache: ${e.message}")
            }

            if(Companion.server.type == ServerType.LOBBY) {
                lobbyManager = LobbyManager()
                // load lobby world cache for fast in-world checks
                if (!DB_Lobby.loadAndCacheBoundary()) {
                    logger.warning("Failed to load lobby world from database; lobby checks will be disabled.")
                }
                // start stats auto-flush
                DB_Stats.startAutoFlush(1) // flush every 1 minute
            }
        }

        // commands
        delay(200) {
            open().thenAccept { success ->
                if(success) {
                    server.setWhitelist(false)
                } else {
                    // log or alert
                }
            }
        }
    }

    override fun onDisable() {

        DB_Stats.shutdownAndFlush()

        dbm?.close()
        if (dbm != null) {
            logger.info("Database pool safely disconnected.")
        }

        RedisManager.close()
        RedisSubscriber.stop()

        delay(3000) {
            server.shutdown() // server can't run without this plugin
        }
    }

    private fun open() : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val arenaManagerFuture = arenaManager.success
        val lobbyManagerFuture = lobbyManager.success

        CompletableFuture.allOf(arenaManagerFuture, lobbyManagerFuture).thenAccept {
            val allSuccess = arenaManagerFuture.join() && lobbyManagerFuture.join()
            future.complete(allSuccess)
        }

        return future
    }

}
