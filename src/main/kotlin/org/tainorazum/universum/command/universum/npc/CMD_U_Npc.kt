package org.tainorazum.universum.command.universum.npc

import com.google.gson.Gson
import org.bukkit.entity.Player
import org.tainorazum.universum.command.universum.UniversumCommand
import org.tainorazum.universum.command.universum.UniversumCommandSub
import org.tainorazum.universum.server.npc.NpcModifier
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("universum", "uni", "u")
class CMD_U_Npc : UniversumCommand(UniversumCommandSub.NPC) {

    override val tutorial : List<String> = mutableListOf<String>(
        "/universum help <subject> - Receive help on a specific command.",
        "/universum npc create <name> - Create NPC at your location.",
        "/universum npc delete <id> - Delete NPC",
        "/universum npc create <name> - Create NPC at your location",
        "/universum npc delete <id> - Delete an NPC",
        "/universum npc holo add <id> <text> - Add hologram line",
        "/universum npc holo clear <id> - Clear holograms",
        "/universum npc action add <id> <type> <json> - Add action",
        "/universum npc action clear <id> - Clear actions"
    )

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

}