package org.tainorazum.universum

import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.twilight
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.command.universum.UniversumCommand
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.game.GameManager
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.lobby.LobbyManager
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_GameRequest
import org.tainorazum.universum.server.Server
import org.tainorazum.universum.server.ServerManager
import org.tainorazum.universum.server.ServerType
import org.tainorazum.universum.server.npc.NpcManager
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.visual.VisualServerManager
import revxrsal.commands.bukkit.BukkitLamp
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set
        lateinit var server: Server

        var dbm: DatabaseManager? = null
            private set

        // all servers
        lateinit var arenaManager: ArenaManager private set
        lateinit var gameManager: GameManager private set
        lateinit var serverManager: ServerManager private set
        lateinit var visualServerManager: VisualServerManager private set

        // lobby servers
        lateinit var lobbyManager: LobbyManager private set
    }

    override fun onEnable() {

        // server
        instance = this
        saveDefaultConfig()

        // dependencies & utils
        twilight(this)
        val lamp = BukkitLamp.builder(this).build()

        // redis
        RedisManager.init()
        RedisSubscriber.start(this)
        RM_GameRequest()

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
        arenaManager = ArenaManager()
        gameManager = GameManager()
        serverManager = ServerManager()
        visualServerManager = VisualServerManager()

        // load server settings and visual data into cache
        try {
            DB_ServerSettings.loadAllSettingsIntoCache().get()
            logger.info("Successfully loaded server settings into cache.")
        } catch (e: Exception) {
            logger.warning("Failed to load server settings into cache: ${e.message}")
        }

        try {
            NpcManager.init()
            // register protocol listener (if ProtocolLib present)
            try {
                val listener = org.tainorazum.universum.server.npc.NpcPacketListener(this)
                listener.register()
            } catch (e: NoClassDefFoundError) {
                logger.info("ProtocolLib not present; packet-based NPC interactions will be disabled. Falling back to events.")
            }
        } catch (e: Exception) {
            logger.warning("Failed to initialize NPC manager: ${e.message}")
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

        // commands
        // lamp.register(Cmd())
        // lamp.register(Landmarks())
        // Listeners()
        lamp.register(UniversumCommand())
    }

    override fun onDisable() {

        DB_Stats.shutdownAndFlush()

        dbm?.close()
        if (dbm != null) {
            logger.info("Database pool safely disconnected.")
        }

        RedisManager.close()
        RedisSubscriber.stop()

        // shutdown NPC manager
        try {
            org.tainorazum.universum.server.npc.NpcManager.shutdown()
        } catch (_: Exception) {}

        delay(3000) {
            server.shutdown() // server can't run without this plugin
        }
    }

}
