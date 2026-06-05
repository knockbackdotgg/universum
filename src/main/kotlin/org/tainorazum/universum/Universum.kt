package org.tainorazum.universum

import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.twilight
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.game.GameManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_GameRequest
import revxrsal.commands.bukkit.BukkitLamp
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set

        var dbm: DatabaseManager? = null
            private set

        lateinit var arenaManager: ArenaManager private set
        lateinit var gameManager: GameManager private set
    }

    override fun onEnable() {
        instance = this

        saveDefaultConfig()

        twilight(this)
        val lamp = BukkitLamp.builder(this).build()

        RedisManager.init()
        // register & start redis pub/sub listeners
        RM_GameRequest()
        RedisSubscriber.start(this)

        try {
            dbm = DatabaseManager()
            dbm?.init()
            logger.info("Successfully connected to the database pool.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize database connection! Disabling plugin...", e)
            server.pluginManager.disablePlugin(this)
            return
        }

        arenaManager = ArenaManager()
        gameManager = GameManager()

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

        delay(5000) {
            server.shutdown() // server can't run without this plugin
        }

    }
}
