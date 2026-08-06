package org.tainorazum.universum.lobby.npc

import de.oliver.fancynpcs.api.FancyNpcsPlugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.util.Vector
import org.tainorazum.universum.Universum
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object NpcBouncer {

    // Tracks player UUIDs and the timestamp (in ms) when they were last launched
    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    /**
     * Starts a repeating bouncer task for ALL registered FancyNpcs.
     */
    fun startBouncerTask(
        radius: Double = 1.0,
        launchStrength: Double = 0.5,
        upwardStrength: Double = 0.2,
        cooldownMs: Long = 1000L // 1 second cooldown prevents multiple launches on high ping
    ) {
        val radiusSquared = radius * radius

        Bukkit.getScheduler().runTaskTimer(Universum.instance, Runnable {
            val npcManager = FancyNpcsPlugin.get().npcManager
            val allNpcs = npcManager.allNpcs

            if (allNpcs.isEmpty()) return@Runnable

            val currentTime = System.currentTimeMillis()

            for (npc in allNpcs) {
                val npcLoc = npc.data.location ?: continue
                val world = npcLoc.world ?: continue

                for (player in world.players) {
                    if (player.gameMode.name == "SPECTATOR") continue

                    // Check if player is on ping cooldown
                    val lastLaunch = cooldowns.getOrDefault(player.uniqueId, 0L)
                    if (currentTime - lastLaunch < cooldownMs) continue

                    val playerLoc = player.location

                    // Distance check
                    if (npcLoc.distanceSquared(playerLoc) <= radiusSquared) {

                        // Set cooldown immediately so high ping doesn't re-trigger
                        cooldowns[player.uniqueId] = currentTime

                        // Vector pointing away from NPC towards player
                        val direction: Vector = playerLoc.toVector().subtract(npcLoc.toVector())

                        if (direction.lengthSquared() == 0.0) {
                            direction.x = 1.0
                        } else {
                            direction.normalize()
                        }

                        // Apply launch velocity
                        val velocity = direction.multiply(launchStrength)
                        velocity.y = upwardStrength

                        player.velocity = velocity
                        player.playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f)
                    }
                }
            }
        }, 2L, 2L)
    }
}