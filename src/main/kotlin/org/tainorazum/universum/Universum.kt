package org.tainorazum.universum

import gg.flyte.twilight.twilight
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.game.arena.ArenaManager
import revxrsal.commands.bukkit.BukkitLamp
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set

        var dbm: DatabaseManager? = null
            private set

        lateinit var arenaManager: ArenaManager private set
    }

    override fun onEnable() {
        instance = this

        saveDefaultConfig()

        twilight(this)
        val lamp = BukkitLamp.builder(this).build()

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

        // lamp.register(Cmd())
        // lamp.register(Landmarks())
        // Listeners()
    }

    override fun onDisable() {

        dbm?.close()
        if (dbm != null) {
            logger.info("Database pool safely disconnected.")
        }
    }
}
