package org.tainorazum.universum.server.npc

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import java.util.UUID

class NpcPacketListener(private val plugin: Universum) {
    private val manager = ProtocolLibrary.getProtocolManager()

    fun register() {
        manager.addPacketListener(object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            override fun onPacketReceiving(event: PacketEvent) {
                val packet = event.packet
                handleUseEntity(event.player, packet)
            }
        })
    }

    private fun handleUseEntity(player: Player, packet: PacketContainer) {
        try {
            val entityId = packet.integers.read(0)
            // action is an int: 0 = INTERACT, 1 = ATTACK, 2 = INTERACT_AT
            val actionId = packet.integers.read(1)
            val actionName = when (actionId) {
                0 -> "INTERACT"
                1 -> "ATTACK"
                2 -> "INTERACT_AT"
                else -> "UNKNOWN"
            }
            NpcManager.handleClick(player.uniqueId, entityId, actionName)
        } catch (e: Exception) {
            // ignore malformed
        }
    }

}
