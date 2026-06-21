package org.tainorazum.universum.server.npc

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.DatabaseUtil
import java.util.*

object ActionExecutor {
    private val gson = Gson()

    fun execute(playerUuid: UUID, action: NpcActionModel) {
        val player = Bukkit.getPlayer(playerUuid) ?: return
        when (action.actionType.uppercase()) {
            "RUN_COMMAND" -> {
                val data = gson.fromJson(action.actionData, Map::class.java)
                val cmd = (data["command"] as? String)?.replace("%player%", player.name) ?: return
                // run on main thread
                Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                    Universum.instance.server.dispatchCommand(Universum.instance.server.consoleSender, cmd)
                })
            }
            "MESSAGE" -> {
                val data = gson.fromJson(action.actionData, Map::class.java)
                val msg = (data["message"] as? String)?.replace("%player%", player.name) ?: return
                Bukkit.getScheduler().runTask(Universum.instance, Runnable { player.sendMessage(msg) })
            }
            else -> {
                // unsupported action type - ignore for now
            }
        }
    }
}
