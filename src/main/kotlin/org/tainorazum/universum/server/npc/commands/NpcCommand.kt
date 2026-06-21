package org.tainorazum.universum.server.npc.commands

import org.bukkit.entity.Player
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.tainorazum.universum.server.npc.NpcModifier
import com.google.gson.Gson

/**
 * Admin command handler for NPC management.
 * Usage:
 * /npc create <name> - Creates an NPC at player's location
 * /npc list - Lists all NPCs
 * /npc delete <id> - Deletes an NPC
 * /npc holo add <npcId> <text> - Adds a hologram line
 * /npc holo clear <npcId> - Clears all holograms
 * /npc action add <npcId> <type> <json> - Adds an action
 */
class NpcCommand : CommandExecutor {
    private val gson = Gson()

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "create" -> handleCreate(sender, args)
            "delete" -> handleDelete(sender, args)
            "list" -> handleList(sender, args)
            "holo" -> handleHolo(sender, args)
            "action" -> handleAction(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleCreate(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /npc create <name>")
            return
        }
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command.")
            return
        }

        val name = args[1]
        val loc = sender.location
        NpcModifier.createNpc(name, loc)
        sender.sendMessage("§aCreated NPC '$name' at your location.")
    }

    private fun handleDelete(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /npc delete <id>")
            return
        }

        val npcId = args[1].toLongOrNull() ?: run {
            sender.sendMessage("§cInvalid NPC ID.")
            return
        }

        NpcModifier.deleteNpc(npcId)
        sender.sendMessage("§aDeleted NPC #$npcId.")
    }

    private fun handleList(sender: CommandSender, args: Array<String>) {
        sender.sendMessage("§a=== NPC List ===")
        sender.sendMessage("§6To view NPCs, check the database or use /npc list (live list not yet implemented)")
    }

    private fun handleHolo(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /npc holo <add|clear> <npcId> [text...]")
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 4) {
                    sender.sendMessage("§cUsage: /npc holo add <npcId> <text>")
                    return
                }
                val npcId = args[2].toLongOrNull() ?: run {
                    sender.sendMessage("§cInvalid NPC ID.")
                    return
                }
                val text = args.drop(3).joinToString(" ")
                NpcModifier.addHologramLine(npcId, 0, "TEXT", text)
                sender.sendMessage("§aAdded hologram line to NPC #$npcId: $text")
            }
            "clear" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cUsage: /npc holo clear <npcId>")
                    return
                }
                val npcId = args[2].toLongOrNull() ?: run {
                    sender.sendMessage("§cInvalid NPC ID.")
                    return
                }
                NpcModifier.clearHolograms(npcId)
                sender.sendMessage("§aCleared all holograms for NPC #$npcId.")
            }
            else -> sender.sendMessage("§cUsage: /npc holo <add|clear>")
        }
    }

    private fun handleAction(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /npc action <add|clear> <npcId> [type] [json]")
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 5) {
                    sender.sendMessage("§cUsage: /npc action add <npcId> <type> <json>")
                    sender.sendMessage("§6Example: /npc action add 1 RUN_COMMAND {\"command\":\"say hello\"}")
                    return
                }
                val npcId = args[2].toLongOrNull() ?: run {
                    sender.sendMessage("§cInvalid NPC ID.")
                    return
                }
                val type = args[3]
                val json = args.drop(4).joinToString(" ")
                try {
                    gson.fromJson(json, Map::class.java) // validate JSON
                    NpcModifier.addAction(npcId, type, json)
                    sender.sendMessage("§aAdded action to NPC #$npcId.")
                } catch (e: Exception) {
                    sender.sendMessage("§cInvalid JSON: ${e.message}")
                }
            }
            "clear" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cUsage: /npc action clear <npcId>")
                    return
                }
                val npcId = args[2].toLongOrNull() ?: run {
                    sender.sendMessage("§cInvalid NPC ID.")
                    return
                }
                NpcModifier.clearActions(npcId)
                sender.sendMessage("§aCleared all actions for NPC #$npcId.")
            }
            else -> sender.sendMessage("§cUsage: /npc action <add|clear>")
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6=== NPC Commands ===")
        sender.sendMessage("§a/npc create <name> §7- Create NPC at your location")
        sender.sendMessage("§a/npc delete <id> §7- Delete an NPC")
        sender.sendMessage("§a/npc list §7- List all NPCs")
        sender.sendMessage("§a/npc holo add <id> <text> §7- Add hologram line")
        sender.sendMessage("§a/npc holo clear <id> §7- Clear holograms")
        sender.sendMessage("§a/npc action add <id> <type> <json> §7- Add action")
        sender.sendMessage("§a/npc action clear <id> §7- Clear actions")
    }
}
