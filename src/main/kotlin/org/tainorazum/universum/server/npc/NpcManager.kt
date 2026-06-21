package org.tainorazum.universum.server.npc

import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.tainorazum.universum.Universum
import java.lang.reflect.Field
import java.util.*

/**
 * Simple manager: spawns NPCs as ArmorStands with player skull heads (for skins).
 * Uses ProtocolLib listener (registered separately) to detect interactions by entity id.
 */
object NpcManager {
    private val npcCache = mutableMapOf<Long, NpcData>()
    private val entityToNpc = mutableMapOf<Int, Long>()
    private val spawnedEntities = mutableMapOf<Long, MutableList<Entity>>()

    fun init() {
        // load from DB and spawn on main thread
        val list = NpcRepository.loadAll()
        npcCache.clear()
        list.forEach { npc ->
            npcCache[npc.id] = npc
        }

        Bukkit.getScheduler().runTask(Universum.instance) {
            list.forEach { entity ->
                spawnNpc(entity)
            }
        }
    }

    fun shutdown() {
        // despawn all
        Bukkit.getScheduler().runTask(Universum.instance) {
            spawnedEntities.values.flatten().forEach { it.remove() }
            spawnedEntities.clear()
            entityToNpc.clear()
        }
    }

    private fun spawnNpc(npc: NpcData) {
        val world = Bukkit.getWorld(npc.world) ?: return
        val loc = Location(world, npc.x, npc.y, npc.z, npc.yaw.toDouble().toFloat(), npc.pitch.toDouble().toFloat())

        // body armor stand
        val asEntity = world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
        asEntity.isVisible = false
        asEntity.isMarker = true
        asEntity.isCustomNameVisible = true
        asEntity.customName = npc.name

        // head skull
        npc.skinTexture?.let { tex ->
            val skull = ItemStack(org.bukkit.Material.PLAYER_HEAD)
            val meta = skull.itemMeta as SkullMeta
            try {
                val profile = WrappedGameProfile(UUID.randomUUID(), null)
                if (npc.skinSignature != null) {
                    profile.properties.put("textures", WrappedSignedProperty("textures", tex, npc.skinSignature))
                } else {
                    profile.properties.put("textures", WrappedSignedProperty("textures", tex, null))
                }

                val profileField: Field = meta.javaClass.getDeclaredField("profile")
                profileField.isAccessible = true
                profileField.set(meta, profile)
            } catch (e: Exception) {
                // reflection failed; ignore
            }
            skull.itemMeta = meta
            asEntity.equipment?.helmet = skull
        }

        // store mappings
        spawnedEntities.computeIfAbsent(npc.id) { mutableListOf() }.add(asEntity)
        entityToNpc[asEntity.entityId] = npc.id

        // spawn holograms
        val holo = NpcRepository.loadHologramsFor(npc.id)
        if (holo.isNotEmpty()) {
            val holos = HologramService.spawnHologram(loc, holo)
            spawnedEntities[npc.id]?.addAll(holos)
            holos.forEach { entityToNpc[it.entityId] = npc.id }
        }
    }

    fun handleClick(playerUuid: UUID, entityId: Int, action: String) {
        val npcId = entityToNpc[entityId] ?: return
        val actions = NpcRepository.loadActionsFor(npcId)
        // execute actions matching action type
        actions.filter { it.actionType.equals(action, true) }.sortedBy { it.order }
            .forEach { act -> ActionExecutor.execute(playerUuid, act) }
    }

    fun despawnNpc(npcId: Long) {
        Bukkit.getScheduler().runTask(Universum.instance) {
            spawnedEntities[npcId]?.forEach { it.remove() }
            spawnedEntities[npcId]?.forEach { entityToNpc.remove(it.entityId) }
            spawnedEntities.remove(npcId)
            npcCache.remove(npcId)
        }
    }

    fun reloadAndRespawn(npcId: Long? = null) {
        Bukkit.getScheduler().runTask(Universum.instance) {
            if (npcId == null) {
                // reload all
                npcCache.keys.toList().forEach { despawnNpc(it) }
                val list = NpcRepository.loadAll()
                npcCache.clear()
                list.forEach { npc ->
                    npcCache[npc.id] = npc
                    spawnNpc(npc)
                }
            } else {
                // reload specific NPC
                despawnNpc(npcId)
                val npc = NpcRepository.loadAll().find { it.id == npcId } ?: return@runTask
                npcCache[npc.id] = npc
                spawnNpc(npc)
            }
        }
    }
}
