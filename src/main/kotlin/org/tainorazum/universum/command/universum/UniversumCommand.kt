package org.tainorazum.universum.command.universum

import org.bukkit.entity.Player
import org.tainorazum.universum.server.npc.NpcModifier
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Subcommand
import com.google.gson.Gson

@Command("universum", "uni", "u")
class UniversumCommand {

    private val gson = Gson()

    @CommandPlaceholder
    fun help() {

    }

    @Subcommand("npc create")
    fun npcCreate(player: Player, name: String) {
        val location = player.location
        NpcModifier.createNpc(name, location)
        player.sendMessage("§aCreated NPC '$name' at your location.")
    }

    @Subcommand("npc delete")
    fun npcDelete(player: Player, npcId: Long) {
        if (!player.hasPermission("universum.npc")) {
            player.sendMessage("§cYou do not have permission.")
            return
        }
        NpcModifier.deleteNpc(npcId)
        player.sendMessage("§aDeleted NPC #$npcId.")
    }

    @Subcommand("npc holo add")
    fun npcHoloAdd(player: Player, npcId: Long, vararg text: String) {
        if (!player.hasPermission("universum.npc")) {
            player.sendMessage("§cYou do not have permission.")
            return
        }
        val content = text.joinToString(" ")
        NpcModifier.addHologramLine(npcId, 0, "TEXT", content)
        player.sendMessage("§aAdded hologram line to NPC #$npcId.")
    }

    @Subcommand("npc holo clear")
    fun npcHoloClear(player: Player, npcId: Long) {
        if (!player.hasPermission("universum.npc")) {
            player.sendMessage("§cYou do not have permission.")
            return
        }
        NpcModifier.clearHolograms(npcId)
        player.sendMessage("§aCleared all holograms for NPC #$npcId.")
    }

    @Subcommand("npc action add")
    fun npcActionAdd(player: Player, npcId: Long, type: String, vararg jsonParts: String) {
        if (!player.hasPermission("universum.npc")) {
            player.sendMessage("§cYou do not have permission.")
            return
        }
        val json = jsonParts.joinToString(" ")
        try {
            gson.fromJson(json, Map::class.java) // validate JSON
            NpcModifier.addAction(npcId, type, json)
            player.sendMessage("§aAdded action to NPC #$npcId.")
        } catch (e: Exception) {
            player.sendMessage("§cInvalid JSON: ${e.message}")
        }
    }

    @Subcommand("npc action clear")
    fun npcActionClear(player: Player, npcId: Long) {
        if (!player.hasPermission("universum.npc")) {
            player.sendMessage("§cYou do not have permission.")
            return
        }
        NpcModifier.clearActions(npcId)
        player.sendMessage("§aCleared all actions for NPC #$npcId.")
    }

    @Subcommand("npc help")
    fun npcHelp(player: Player) {
        player.sendMessage("§6=== NPC Commands ===")
        player.sendMessage("§a/universum npc create <name> §7- Create NPC at your location")
        player.sendMessage("§a/universum npc delete <id> §7- Delete an NPC")
        player.sendMessage("§a/universum npc holo add <id> <text> §7- Add hologram line")
        player.sendMessage("§a/universum npc holo clear <id> §7- Clear holograms")
        player.sendMessage("§a/universum npc action add <id> <type> <json> §7- Add action")
        player.sendMessage("§a/universum npc action clear <id> §7- Clear actions")
    }
}
