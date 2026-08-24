package org.tainorazum.universum.cosmetic.pet

import gg.flyte.twilight.event.event
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Ageable
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Wither
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PetManager {

    private val activePets = ConcurrentHashMap<UUID, Entity>() // Player UUID -> Entity
    private val petEntityUuids = ConcurrentHashMap.newKeySet<UUID>() // Entity UUID set

    init {
        startFollowTask()
    }

    fun registerEvents() {
        event<EntityTargetEvent> {
            if (isPetEntity(entity.uniqueId)) {
                isCancelled = true
                target = null
            }
        }

        event<EntityDamageEvent> {
            if (isPetEntity(entity.uniqueId)) {
                isCancelled = true
            }
        }

        event<PlayerInteractEntityEvent> {
            if (isPetEntity(rightClicked.uniqueId)) {
                isCancelled = true
            }
        }
    }

    fun spawnPet(player: Player, petData: CosmeticData.Pet) {
        despawnPet(player.uniqueId)

        if (!player.isOnline) return

        val location = player.location
        val world = location.world ?: return

        val entity = world.spawnEntity(location, petData.entityType)

        val petName = petData.displayName.append(Component.text(" [${player.name}'s pet]", Colours.DARK_GREY))

        entity.customName(petName)
        entity.isCustomNameVisible = true
        entity.isInvulnerable = true
        entity.isPersistent = false
        entity.isSilent = true

        if (entity is Mob) {
            entity.isCollidable = false
            entity.canPickupItems = false
            entity.equipment.clear()
            entity.target = null
        }

        if (petData.isBaby) {
            when (entity) {
                is Ageable -> entity.setBaby()
                is Zombie -> entity.setBaby(true)
            }
        }

        suppressBossBar(entity)

        activePets[player.uniqueId] = entity
        petEntityUuids.add(entity.uniqueId)
    }

    fun despawnPet(uuid: UUID) {
        activePets.remove(uuid)?.let { entity ->
            petEntityUuids.remove(entity.uniqueId)
            if (entity.isValid) {
                entity.remove()
            }
        }
    }

    fun hasPet(playerUuid: UUID): Boolean = activePets.containsKey(playerUuid)

    fun isPetEntity(entityUuid: UUID): Boolean = petEntityUuids.contains(entityUuid)

    private fun suppressBossBar(entity: Entity) {
        if (entity is Wither) {
            try {
                val nmsEntity = entity.javaClass.getMethod("getHandle").invoke(entity)
                val bossEventField = nmsEntity.javaClass.declaredFields.firstOrNull {
                    it.type.simpleName.contains("ServerBossEvent") || it.type.simpleName.contains("BossBattleServer")
                } ?: nmsEntity.javaClass.getDeclaredField("bossEvent")

                bossEventField.isAccessible = true
                val bossEvent = bossEventField.get(nmsEntity) ?: return

                val setVisible = bossEvent.javaClass.methods.firstOrNull { it.name == "setVisible" }
                setVisible?.invoke(bossEvent, false)

                val removeAllPlayers = bossEvent.javaClass.methods.firstOrNull { it.name == "removeAllPlayers" }
                removeAllPlayers?.invoke(bossEvent)
            } catch (_: Exception) {
                // Silently ignore if NMS mappings vary
            }
        }
    }

    private fun startFollowTask() {
        Bukkit.getScheduler().runTaskTimer(Universum.instance, Runnable {
            val iterator = activePets.entries.iterator()

            while (iterator.hasNext()) {
                val (uuid, entity) = iterator.next()
                val player = Bukkit.getPlayer(uuid)

                if (player == null || !player.isOnline || !entity.isValid) {
                    petEntityUuids.remove(entity.uniqueId)
                    if (entity.isValid) entity.remove()
                    iterator.remove()
                    continue
                }

                suppressBossBar(entity)

                val petLoc = entity.location
                val playerLoc = player.location

                if (petLoc.world != playerLoc.world) {
                    entity.teleport(playerLoc)
                    continue
                }

                val distanceSq = petLoc.distanceSquared(playerLoc)

                if (distanceSq > 256.0) {
                    entity.teleport(playerLoc)
                } else if (distanceSq > 6.25 && entity is Mob) {
                    entity.pathfinder.moveTo(playerLoc, 1.25)
                }
            }
        }, 10L, 10L)
    }
}