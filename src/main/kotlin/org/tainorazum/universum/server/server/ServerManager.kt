package org.tainorazum.universum.server.server

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import gg.flyte.twilight.event.event
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.setting.ServerSetting
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import javax.imageio.ImageIO

class ServerManager {

    private val servers = mutableListOf<Server>()

    init {
        registerServer()

        event<PaperServerListPingEvent> {
            try {
                val currentIdentifier = DB_ServerSettings.get(ServerSetting.CURRENT_MOTD)
                    .getNow(ServerSetting.CURRENT_MOTD.default).toString()

                val motd = Universum.visualServerManager.getMOTD(currentIdentifier).getNow(null)
                if (motd != null) {
                    motd(motd)
                }

                val faviconBytes = Universum.visualServerManager.getFavicon(currentIdentifier).getNow(null)
                if (faviconBytes != null) {
                    val bufferedImage = ImageIO.read(ByteArrayInputStream(faviconBytes))
                    if (bufferedImage != null) {
                        val icon = Bukkit.getServer().loadServerIcon(bufferedImage)
                        setServerIcon(icon)
                    }
                }
            } catch (e: Exception) {
                Universum.instance.logger.log(Level.WARNING, "Failed to apply dynamic server list visuals during ping", e)
            }

            val count = NetworkPlayerManager.onlinePlayers.size

            this.numPlayers = count
            this.maxPlayers = count + 1
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
            DB_Server.register(identifier, type).join()
            DB_Server.preloadPublic(identifier).join()
        } catch (e: Exception) {
            Universum.instance.logger.log(Level.SEVERE, "Could not register server! Disabling plugin...", e)
            Universum.instance.server.pluginManager.disablePlugin(Universum.instance)
        }
    }

    fun public(): CompletableFuture<Boolean> {
        return DB_Server.setPublic(Universum.server.identifier, true)
    }

    fun private(): CompletableFuture<Boolean> {
        return DB_Server.setPublic(Universum.server.identifier, false)
    }

    fun online(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        Universum.server.public.thenAccept { public ->
            DB_Server.update(Universum.server.identifier, Universum.server.type, public, online = true).thenAccept { success ->
                future.complete(success)
            }
        }
        return future
    }

    fun offline(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        Universum.server.public.thenAccept { public ->
            DB_Server.update(Universum.server.identifier, Universum.server.type, public, online = false).thenAccept { success ->
                future.complete(success)
            }
        }
        return future
    }
}
