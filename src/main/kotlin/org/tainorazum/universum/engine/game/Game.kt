package org.tainorazum.universum.engine.game

import gg.flyte.twilight.event.event
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.Listener
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
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.gametypes.GameType
import java.util.UUID

open class Game(
    val gameType: GameType,
    val arena: Arena
) {

    open fun start() {

    }

    fun updateStatistics(uuid: UUID) {

    }

    // events

    init {
        registerEventListener()
    }

    private fun registerEventListener() {
        val listener = object : Listener {}
        val eventClasses = listOf(
            BlockBreakEvent::class.java, BlockPlaceEvent::class.java, BlockBurnEvent::class.java, BlockFadeEvent::class.java,
            BlockFormEvent::class.java, BlockFromToEvent::class.java, BlockGrowEvent::class.java, BlockIgniteEvent::class.java,
            BlockPhysicsEvent::class.java, BlockSpreadEvent::class.java, BlockPistonExtendEvent::class.java, BlockPistonRetractEvent::class.java,
            PlayerJoinEvent::class.java, PlayerQuitEvent::class.java, PlayerKickEvent::class.java, PlayerDeathEvent::class.java,
            PlayerMoveEvent::class.java, PlayerTeleportEvent::class.java, AsyncPlayerChatEvent::class.java,
            PlayerInteractEvent::class.java, PlayerInteractEntityEvent::class.java, PlayerDropItemEvent::class.java,
            PlayerRespawnEvent::class.java, PlayerItemConsumeEvent::class.java, PlayerSwapHandItemsEvent::class.java,
            PlayerFishEvent::class.java, PlayerCommandPreprocessEvent::class.java, PlayerBedEnterEvent::class.java, PlayerBedLeaveEvent::class.java,
            EntityDamageEvent::class.java, EntityDamageByEntityEvent::class.java, EntityDeathEvent::class.java,
            CreatureSpawnEvent::class.java, EntityTargetEvent::class.java, EntityCombustEvent::class.java,
            EntityExplodeEvent::class.java, EntityInteractEvent::class.java, ProjectileLaunchEvent::class.java,
            ProjectileHitEvent::class.java, EntityTameEvent::class.java, EntityPickupItemEvent::class.java,
            InventoryClickEvent::class.java, InventoryCloseEvent::class.java, InventoryOpenEvent::class.java,
            InventoryDragEvent::class.java, FurnaceBurnEvent::class.java, FurnaceSmeltEvent::class.java,
            PrepareItemCraftEvent::class.java, CraftItemEvent::class.java, WeatherChangeEvent::class.java, ThunderChangeEvent::class.java,
            ChunkLoadEvent::class.java, ChunkUnloadEvent::class.java, WorldSaveEvent::class.java, WorldLoadEvent::class.java,
            WorldUnloadEvent::class.java, StructureGrowEvent::class.java, ServerListPingEvent::class.java,
            PluginEnableEvent::class.java, PluginDisableEvent::class.java, VehicleEnterEvent::class.java,
            VehicleExitEvent::class.java, VehicleMoveEvent::class.java, VehicleDamageEvent::class.java,
            VehicleCreateEvent::class.java, VehicleDestroyEvent::class.java, HangingBreakEvent::class.java,
            HangingPlaceEvent::class.java, EnchantItemEvent::class.java, PortalCreateEvent::class.java
        )

        eventClasses.forEach { eventClass ->
            try {
                Universum.instance.server.pluginManager.registerEvent(
                    eventClass,
                    listener,
                    org.bukkit.event.EventPriority.NORMAL,
                    { _, event -> dispatchEvent(event) },
                    Universum.instance
                )
            } catch (e: Exception) {
                Universum.instance.logger.warning("Failed to register event listener for ${eventClass.simpleName}: ${e.message}")
            }
        }
    }

    // blocks
    open fun BLOCK_BREAK_EVENT(event: BlockBreakEvent) { cancel(event) }
    open fun BLOCK_PLACE_EVENT(event: BlockPlaceEvent) { cancel(event) }
    open fun BLOCK_BURN_EVENT(event: BlockBurnEvent) { cancel(event) }
    open fun BLOCK_FADE_EVENT(event: BlockFadeEvent) { cancel(event) }
    open fun BLOCK_FORM_EVENT(event: BlockFormEvent) { cancel(event) }
    open fun BLOCK_FROM_TO_EVENT(event: BlockFromToEvent) { cancel(event) }
    open fun BLOCK_GROW_EVENT(event: BlockGrowEvent) { cancel(event) }
    open fun BLOCK_IGNITE_EVENT(event: BlockIgniteEvent) { cancel(event) }
    open fun BLOCK_PHYSICS_EVENT(event: BlockPhysicsEvent) { cancel(event) }
    open fun BLOCK_SPREAD_EVENT(event: BlockSpreadEvent) { cancel(event) }
    open fun BLOCK_PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    open fun BLOCK_PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    // player
    open fun PLAYER_JOIN_EVENT(event: PlayerJoinEvent) { cancel(event) }
    open fun PLAYER_QUIT_EVENT(event: PlayerQuitEvent) { cancel(event) }
    open fun PLAYER_KICK_EVENT(event: PlayerKickEvent) { cancel(event) }
    open fun PLAYER_DEATH_EVENT(event: PlayerDeathEvent) {
        event.deathMessage(null)
        try { event.drops.clear() } catch (_: Exception) {}
        cancel(event)
    }
    open fun PLAYER_MOVE_EVENT(event: PlayerMoveEvent) { cancel(event) }
    open fun PLAYER_TELEPORT_EVENT(event: PlayerTeleportEvent) { cancel(event) }
    open fun ASYNC_PLAYER_CHAT_EVENT(event: AsyncPlayerChatEvent) { cancel(event) }
    open fun PLAYER_INTERACT_EVENT(event: PlayerInteractEvent) { cancel(event) }
    open fun PLAYER_INTERACT_ENTITY_EVENT(event: PlayerInteractEntityEvent) { cancel(event) }
    open fun PLAYER_DROP_ITEM_EVENT(event: PlayerDropItemEvent) { cancel(event) }
    open fun PLAYER_RESPAWN_EVENT(event: PlayerRespawnEvent) { cancel(event) }
    open fun PLAYER_ITEM_CONSUME_EVENT(event: PlayerItemConsumeEvent) { cancel(event) }
    open fun PLAYER_SWAP_HAND_ITEMS_EVENT(event: PlayerSwapHandItemsEvent) { cancel(event) }
    open fun PLAYER_FISH_EVENT(event: PlayerFishEvent) { cancel(event) }
    open fun PLAYER_COMMAND_PREPROCESS_EVENT(event: PlayerCommandPreprocessEvent) { cancel(event) }
    open fun PLAYER_BED_ENTER_EVENT(event: PlayerBedEnterEvent) { cancel(event) }
    open fun PLAYER_BED_LEAVE_EVENT(event: PlayerBedLeaveEvent) { cancel(event) }

    // entity
    open fun ENTITY_DAMAGE_EVENT(event: EntityDamageEvent) { cancel(event) }
    open fun ENTITY_DAMAGE_BY_ENTITY_EVENT(event: EntityDamageByEntityEvent) { cancel(event) }
    open fun ENTITY_DEATH_EVENT(event: EntityDeathEvent) { cancel(event) }
    open fun CREATURE_SPAWN_EVENT(event: CreatureSpawnEvent) { cancel(event) }
    open fun ENTITY_TARGET_EVENT(event: EntityTargetEvent) { cancel(event) }
    open fun ENTITY_COMBUST_EVENT(event: EntityCombustEvent) { cancel(event) }
    open fun ENTITY_EXPLODE_EVENT(event: EntityExplodeEvent) { cancel(event) }
    open fun ENTITY_INTERACT_EVENT(event: EntityInteractEvent) { cancel(event) }
    open fun PROJECTILE_LAUNCH_EVENT(event: ProjectileLaunchEvent) { cancel(event) }
    open fun PROJECTILE_HIT_EVENT(event: ProjectileHitEvent) { cancel(event) }
    open fun ENTITY_TAME_EVENT(event: EntityTameEvent) { cancel(event) }
    open fun ENTITY_PICKUP_ITEM_EVENT(event: EntityPickupItemEvent) { cancel(event) }

    // inventory & crafting
    open fun INVENTORY_CLICK_EVENT(event: InventoryClickEvent) { cancel(event) }
    open fun INVENTORY_CLOSE_EVENT(event: InventoryCloseEvent) { cancel(event) }
    open fun INVENTORY_OPEN_EVENT(event: InventoryOpenEvent) { cancel(event) }
    open fun INVENTORY_DRAG_EVENT(event: InventoryDragEvent) { cancel(event) }
    open fun FURNACE_BURN_EVENT(event: FurnaceBurnEvent) { cancel(event) }
    open fun FURNACE_SMELT_EVENT(event: FurnaceSmeltEvent) { cancel(event) }
    open fun PREPARE_ITEM_CRAFT_EVENT(event: PrepareItemCraftEvent) { cancel(event) }
    open fun CRAFT_ITEM_EVENT(event: CraftItemEvent) { cancel(event) }

    // weather & world
    open fun WEATHER_CHANGE_EVENT(event: WeatherChangeEvent) { cancel(event) }
    open fun THUNDER_CHANGE_EVENT(event: ThunderChangeEvent) { cancel(event) }
    open fun CHUNK_LOAD_EVENT(event: ChunkLoadEvent) { cancel(event) }
    open fun CHUNK_UNLOAD_EVENT(event: ChunkUnloadEvent) { cancel(event) }
    open fun WORLD_SAVE_EVENT(event: WorldSaveEvent) { cancel(event) }
    open fun WORLD_LOAD_EVENT(event: WorldLoadEvent) { cancel(event) }
    open fun WORLD_UNLOAD_EVENT(event: WorldUnloadEvent) { cancel(event) }
    open fun STRUCTURE_GROW_EVENT(event: StructureGrowEvent) { cancel(event) }

    // server & plugin (most likely will never need these)
    open fun SERVER_LIST_PING_EVENT(event: ServerListPingEvent) { cancel(event) }
    open fun PLUGIN_ENABLE_EVENT(event: PluginEnableEvent) { cancel(event) }
    open fun PLUGIN_DISABLE_EVENT(event: PluginDisableEvent) { cancel(event) }

    // vehicle
    open fun VEHICLE_ENTER_EVENT(event: VehicleEnterEvent) { cancel(event) }
    open fun VEHICLE_EXIT_EVENT(event: VehicleExitEvent) { cancel(event) }
    open fun VEHICLE_MOVE_EVENT(event: VehicleMoveEvent) { cancel(event) }
    open fun VEHICLE_DAMAGE_EVENT(event: VehicleDamageEvent) { cancel(event) }
    open fun VEHICLE_CREATE_EVENT(event: VehicleCreateEvent) { cancel(event) }
    open fun VEHICLE_DESTROY_EVENT(event: VehicleDestroyEvent) { cancel(event) }

    // Hanging
    open fun HANGING_BREAK_EVENT(event: HangingBreakEvent) { cancel(event) }
    open fun HANGING_PLACE_EVENT(event: HangingPlaceEvent) { cancel(event) }

    // misc
    open fun ENCHANT_ITEM_EVENT(event: EnchantItemEvent) { cancel(event) }
    open fun PORTAL_CREATE_EVENT(event: PortalCreateEvent) { cancel(event) }
    open fun PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    open fun PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    private fun cancel(event: Event) {
        if (event is Cancellable) {
            event.isCancelled = true
        }
    }

    // determine if event is related to this game
    open fun isRelated(event: Event): Boolean {
        try {
            // events: player
            if (event is PlayerEvent) {
                return isPlayerInGame(event.player)
            }

            // events: entity
            if (event is EntityEvent) {
                val entity = event.entity
                if (entity is Player) return isPlayerInGame(entity)
                return isLocationInArena(entity.location)
            }

            // events: blocks
            if (event is BlockEvent) {
                return isLocationInArena(event.block.location)
            }

            // events: inventory
            if (event is InventoryClickEvent) {
                val who = event.whoClicked
                if (who is Player) return isPlayerInGame(who)
            }

            val cls = event::class.java
            val tryGet = listOf("getPlayer", "getEntity", "getBlock", "getLocation", "getWhoClicked")
            for (name in tryGet) {
                try {
                    val m = cls.getMethod(name)
                    val res = m.invoke(event) ?: continue
                    when (res) {
                        is Player -> return isPlayerInGame(res)
                        is Entity -> return isLocationInArena(res.location)
                        is Block -> return isLocationInArena(res.location)
                        is Location -> return isLocationInArena(res)
                    }
                } catch (_: NoSuchMethodException) {
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    /**
     * Whether a player is considered part of this game
     * Ideally adjust per subclass
     */
    open fun isPlayerInGame(player: Player): Boolean {
        val center = arena.centerLocation ?: return false
        if (player.world != center.world) return false
        val dx = player.location.x - center.x
        val dz = player.location.z - center.z
        val dy = player.location.y - center.y
        val distSq = dx * dx + dy * dy + dz * dz
        val radius = arena.radius
        return distSq <= radius * radius
    }

    private fun isLocationInArena(loc: Location): Boolean {
        val center = arena.centerLocation ?: return false
        if (loc.world != center.world) return false
        val dx = loc.x - center.x
        val dz = loc.z - center.z
        val dy = loc.y - center.y
        val distSq = dx * dx + dy * dy + dz * dz
        val radius = arena.radius
        return distSq <= radius * radius
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