package org.tainorazum.universum.lobby

import gg.flyte.twilight.event.event
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.event.vehicle.VehicleCreateEvent
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.weather.ThunderChangeEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.lobby.database.DB_Lobby

class LobbyManager {

    private val players = mutableListOf<Player>()

    fun enter(player: Player): Boolean {
        return players.add(player)
    }

    fun leave(player: Player): Boolean {
        return players.remove(player)
    }

    init {

        event<Event> {
            dispatchEvent(this)
        }

    }

    // blocks
    private fun BLOCK_BREAK_EVENT(event: BlockBreakEvent) { cancel(event) }
    private fun BLOCK_PLACE_EVENT(event: BlockPlaceEvent) { cancel(event) }
    private fun BLOCK_BURN_EVENT(event: BlockBurnEvent) { cancel(event) }
    private fun BLOCK_FADE_EVENT(event: BlockFadeEvent) { cancel(event) }
    private fun BLOCK_FORM_EVENT(event: BlockFormEvent) { cancel(event) }
    private fun BLOCK_FROM_TO_EVENT(event: BlockFromToEvent) { cancel(event) }
    private fun BLOCK_GROW_EVENT(event: BlockGrowEvent) { cancel(event) }
    private fun BLOCK_IGNITE_EVENT(event: BlockIgniteEvent) { cancel(event) }
    private fun BLOCK_PHYSICS_EVENT(event: BlockPhysicsEvent) { cancel(event) }
    private fun BLOCK_SPREAD_EVENT(event: BlockSpreadEvent) { cancel(event) }
    private fun BLOCK_PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    private fun BLOCK_PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    // player
    private fun PLAYER_JOIN_EVENT(event: PlayerJoinEvent) { cancel(event) }
    private fun PLAYER_QUIT_EVENT(event: PlayerQuitEvent) { cancel(event) }
    private fun PLAYER_KICK_EVENT(event: PlayerKickEvent) { cancel(event) }
    private fun PLAYER_DEATH_EVENT(event: PlayerDeathEvent) {
        event.deathMessage(null)
        try { event.drops.clear() } catch (_: Exception) {}
        cancel(event)
    }
    private fun PLAYER_MOVE_EVENT(event: PlayerMoveEvent) { cancel(event) }
    private fun PLAYER_TELEPORT_EVENT(event: PlayerTeleportEvent) { cancel(event) }
    private fun ASYNC_PLAYER_CHAT_EVENT(event: AsyncPlayerChatEvent) { cancel(event) }
    private fun PLAYER_INTERACT_EVENT(event: PlayerInteractEvent) { cancel(event) }
    private fun PLAYER_INTERACT_ENTITY_EVENT(event: PlayerInteractEntityEvent) { cancel(event) }
    private fun PLAYER_DROP_ITEM_EVENT(event: PlayerDropItemEvent) { cancel(event) }
    private fun PLAYER_RESPAWN_EVENT(event: PlayerRespawnEvent) { cancel(event) }
    private fun PLAYER_ITEM_CONSUME_EVENT(event: PlayerItemConsumeEvent) { cancel(event) }
    private fun PLAYER_SWAP_HAND_ITEMS_EVENT(event: PlayerSwapHandItemsEvent) { cancel(event) }
    private fun PLAYER_FISH_EVENT(event: PlayerFishEvent) { cancel(event) }
    private fun PLAYER_COMMAND_PREPROCESS_EVENT(event: PlayerCommandPreprocessEvent) { cancel(event) }
    private fun PLAYER_BED_ENTER_EVENT(event: PlayerBedEnterEvent) { cancel(event) }
    private fun PLAYER_BED_LEAVE_EVENT(event: PlayerBedLeaveEvent) { cancel(event) }

    // entity
    private fun ENTITY_DAMAGE_EVENT(event: EntityDamageEvent) { cancel(event) }
    private fun ENTITY_DAMAGE_BY_ENTITY_EVENT(event: EntityDamageByEntityEvent) { cancel(event) }
    private fun ENTITY_DEATH_EVENT(event: EntityDeathEvent) { cancel(event) }
    private fun CREATURE_SPAWN_EVENT(event: CreatureSpawnEvent) { cancel(event) }
    private fun ENTITY_TARGET_EVENT(event: EntityTargetEvent) { cancel(event) }
    private fun ENTITY_COMBUST_EVENT(event: EntityCombustEvent) { cancel(event) }
    private fun ENTITY_EXPLODE_EVENT(event: EntityExplodeEvent) { cancel(event) }
    private fun ENTITY_INTERACT_EVENT(event: EntityInteractEvent) { cancel(event) }
    private fun PROJECTILE_LAUNCH_EVENT(event: ProjectileLaunchEvent) { cancel(event) }
    private fun PROJECTILE_HIT_EVENT(event: ProjectileHitEvent) { cancel(event) }
    private fun ENTITY_TAME_EVENT(event: EntityTameEvent) { cancel(event) }
    private fun ENTITY_PICKUP_ITEM_EVENT(event: EntityPickupItemEvent) { cancel(event) }

    // inventory & crafting
    private fun INVENTORY_CLICK_EVENT(event: InventoryClickEvent) { cancel(event) }
    private fun INVENTORY_CLOSE_EVENT(event: InventoryCloseEvent) { cancel(event) }
    private fun INVENTORY_OPEN_EVENT(event: InventoryOpenEvent) { cancel(event) }
    private fun INVENTORY_DRAG_EVENT(event: InventoryDragEvent) { cancel(event) }
    private fun FURNACE_BURN_EVENT(event: FurnaceBurnEvent) { cancel(event) }
    private fun FURNACE_SMELT_EVENT(event: FurnaceSmeltEvent) { cancel(event) }
    private fun PREPARE_ITEM_CRAFT_EVENT(event: PrepareItemCraftEvent) { cancel(event) }
    private fun CRAFT_ITEM_EVENT(event: CraftItemEvent) { cancel(event) }

    // weather & world
    private fun WEATHER_CHANGE_EVENT(event: WeatherChangeEvent) { cancel(event) }
    private fun THUNDER_CHANGE_EVENT(event: ThunderChangeEvent) { cancel(event) }
    private fun CHUNK_LOAD_EVENT(event: ChunkLoadEvent) { cancel(event) }
    private fun CHUNK_UNLOAD_EVENT(event: ChunkUnloadEvent) { cancel(event) }
    private fun WORLD_SAVE_EVENT(event: WorldSaveEvent) { cancel(event) }
    private fun WORLD_LOAD_EVENT(event: WorldLoadEvent) { cancel(event) }
    private fun WORLD_UNLOAD_EVENT(event: WorldUnloadEvent) { cancel(event) }
    private fun STRUCTURE_GROW_EVENT(event: StructureGrowEvent) { cancel(event) }

    // server & plugin (most likely will never need these)
    private fun SERVER_LIST_PING_EVENT(event: ServerListPingEvent) { cancel(event) }
    private fun PLUGIN_ENABLE_EVENT(event: PluginEnableEvent) { cancel(event) }
    private fun PLUGIN_DISABLE_EVENT(event: PluginDisableEvent) { cancel(event) }

    // vehicle
    private fun VEHICLE_ENTER_EVENT(event: VehicleEnterEvent) { cancel(event) }
    private fun VEHICLE_EXIT_EVENT(event: VehicleExitEvent) { cancel(event) }
    private fun VEHICLE_MOVE_EVENT(event: VehicleMoveEvent) { cancel(event) }
    private fun VEHICLE_DAMAGE_EVENT(event: VehicleDamageEvent) { cancel(event) }
    private fun VEHICLE_CREATE_EVENT(event: VehicleCreateEvent) { cancel(event) }
    private fun VEHICLE_DESTROY_EVENT(event: VehicleDestroyEvent) { cancel(event) }

    // hanging
    private fun HANGING_BREAK_EVENT(event: HangingBreakEvent) { cancel(event) }
    private fun HANGING_PLACE_EVENT(event: HangingPlaceEvent) { cancel(event) }

    // misc
    private fun ENCHANT_ITEM_EVENT(event: EnchantItemEvent) { cancel(event) }
    private fun PORTAL_CREATE_EVENT(event: PortalCreateEvent) { cancel(event) }
    private fun PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    private fun PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    private fun cancel(event: Event) {
        if (event is Cancellable) {
            event.isCancelled = true
        }
    }

    private fun isPlayerInLobby(player: Player) = players.contains(player)
    private fun isLocationInLobby(location: Location) = DB_Lobby.isLocationInLobby(location) // now returns Boolean directly (synchronous)

    private fun isRelated(event: Event): Boolean {
        try {
            // events: player
            if (event is PlayerEvent) {
                return isPlayerInLobby(event.player)
            }

            // events: entity
            if (event is EntityEvent) {
                val entity = event.entity
                if (entity is Player) return isPlayerInLobby(entity)
                return isLocationInLobby(entity.location)
            }

            // events: blocks
            if (event is BlockEvent) {
                return isLocationInLobby(event.block.location)
            }

            // events: inventory
            if (event is InventoryClickEvent) {
                val who = event.whoClicked
                if (who is Player) return isPlayerInLobby(who)
            }

            val cls = event::class.java
            val tryGet = listOf("getPlayer", "getEntity", "getBlock", "getLocation", "getWhoClicked")
            for (name in tryGet) {
                try {
                    val m = cls.getMethod(name)
                    val res = m.invoke(event) ?: continue
                    when (res) {
                        is Player -> return isPlayerInLobby(res)
                        is Entity -> return isLocationInLobby(res.location)
                        is Block -> return isLocationInLobby(res.location)
                        is Location -> return isLocationInLobby(res)
                    }
                } catch (_: NoSuchMethodException) {
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    private fun dispatchEvent(event: Event) {
        if (!isRelated(event)) return

        val simpleName = event::class.java.simpleName ?: return
        val methodName = simpleName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        try {
            val method = this::class.java.methods.firstOrNull { m ->
                m.name == methodName && m.parameterTypes.size == 1 && m.parameterTypes[0].isAssignableFrom(event.javaClass)
            }
            if (method != null) {
                method.invoke(this, event)
                return
            }
        } catch (e: Exception) {
        }

        if (event is Cancellable) {
            event.isCancelled = true
        }
    }

}