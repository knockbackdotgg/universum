package org.tainorazum.universum.server

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import gg.flyte.twilight.event.event
import org.tainorazum.universum.Universum
import org.tainorazum.universum.Universum.Companion
import java.util.logging.Level

class ServerManager {

    private val servers = mutableListOf<Server>()

    init {
        registerServer()

        event<PaperServerListPingEvent> {
            Universum.visualServerManager.getMOTD().thenAccept { motd ->
                if (motd != null) motd(motd)
            }
        }
    }

    fun getServers() = servers
    fun getServers(type: ServerType) = servers.filter { it.type == type }

    fun getServer(identifier: String) = servers.firstOrNull { it.identifier == identifier }

    private fun registerServer() {
        try {
            val identifier = Universum.instance.config.getString("server.identifier")!!
            val type = ServerType.valueOf(Universum.instance.config.getString("server.type")!!)

            Universum.server = Server(identifier, type)
        } catch (e: Exception) {
            Universum.instance.logger.log(Level.SEVERE, "Could not register server! Disabling plugin...", e)
            Universum.instance.server.pluginManager.disablePlugin(Universum.instance)
        }
    }



}