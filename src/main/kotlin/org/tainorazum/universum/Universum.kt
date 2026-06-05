package org.tainorazum.universum

import com.sk89q.minecraft.util.commands.Console
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.twilight
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.game.GameManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_GameRequest
import org.tainorazum.universum.server.Server
import org.tainorazum.universum.server.ServerType
import revxrsal.commands.bukkit.BukkitLamp
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set
        lateinit var server: Server private set

        var dbm: DatabaseManager? = null
            private set

        lateinit var arenaManager: ArenaManager private set
        lateinit var gameManager: GameManager private set
    }

    override fun onEnable() {

        // server
        instance = this
        saveDefaultConfig()
        registerServer()

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

        // commands
        // lamp.register(Cmd())
        // lamp.register(Landmarks())
        // Listeners()
    }

    override fun onDisable() {

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

    private fun registerServer() {

        try {
            val identifier = config.getString("server.identifier")!!
            val type = ServerType.valueOf(config.getString("server.type")!!)

            Companion.server = Server(identifier, type)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Could not register server! Disabling plugin...", e)
            server.pluginManager.disablePlugin(this)
        }

    }

}
