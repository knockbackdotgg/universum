package org.tainorazum.universum.server.server

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import gg.flyte.twilight.event.event
import org.tainorazum.universum.Universum
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.setting.ServerSetting
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class ServerManager {

    private val servers = mutableListOf<Server>()

    init {
        registerServer()

        event<PaperServerListPingEvent> {
            DB_ServerSettings.get(ServerSetting.CURRENT_MOTD).thenAccept { currentMotd ->
                Universum.visualServerManager.getMOTD(currentMotd.toString()).thenAccept { motd ->
                    if (motd != null) motd(motd)
                }
            }
            maxPlayers = numPlayers+1
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
            DB_Server.update(identifier, type, public = false, online = true)
        } catch (e: Exception) {
            Universum.instance.logger.log(Level.SEVERE, "Could not register server! Disabling plugin...", e)
            Universum.instance.server.pluginManager.disablePlugin(Universum.instance)
        }
    }

    fun public() : CompletableFuture<Boolean> {
        return DB_Server.setPublic(Universum.server.identifier, true)
    }

    fun private() : CompletableFuture<Boolean> {
        return DB_Server.setPublic(Universum.server.identifier, false)
    }

    fun online() : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        Universum.server.public.thenAccept { public ->
            DB_Server.update(Universum.server.identifier, Universum.server.type, public , online = true).thenAccept { success ->
                future.complete(success)
            }
        }
        return future
    }

    fun offline() : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        Universum.server.public.thenAccept { public ->
            DB_Server.update(Universum.server.identifier, Universum.server.type, public , online = false).thenAccept { success ->
                future.complete(success)
            }
        }
        return future
    }

}