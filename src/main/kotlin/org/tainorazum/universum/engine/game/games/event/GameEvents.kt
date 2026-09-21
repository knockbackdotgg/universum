package org.tainorazum.universum.engine.game.games.event

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.*
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.event.vehicle.*
import org.bukkit.event.weather.ThunderChangeEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.event.world.*
import org.tainorazum.universum.Universum
import java.lang.reflect.Method

abstract class GameEvents : Listener {

    private val handlerCache = HashMap<Class<out Event>, Method>()

    init {
        cacheEventHandlers()
    }

    // ==========================================
    // CONTEXT HOOKS (Implemented by subclasses)
    // ==========================================

    abstract fun isPlayerInContext(player: Player): Boolean
    abstract fun isLocationInContext(location: Location): Boolean

    open fun allowBypass(player: Player): Boolean {
        return Universum.playerManager.allowBypassLobby(player)
    }

    // ==========================================
    // REGISTRATION & DISPATCHING
    // ==========================================

    fun register(priority: EventPriority = EventPriority.HIGH) {
        val plugin = Universum.instance
        val pm = plugin.server.pluginManager
        for (eventClass in handlerCache.keys) {
            try {
                pm.registerEvent(
                    eventClass,
                    this,
                    priority,
                    { _, event -> dispatchEvent(event) },
                    plugin
                )
            } catch (e: Exception) {
                plugin.logger.warning("Failed to register event listener for ${eventClass.simpleName}: ${e.message}")
            }
        }
    }

    fun unregister() { HandlerList.unregisterAll(this) }

    private fun cacheEventHandlers() {
        for (method in this::class.java.methods) {
            if (method.parameterCount == 1 && Event::class.java.isAssignableFrom(method.parameterTypes[0])) {
                @Suppress("UNCHECKED_CAST")
                val eventClass = method.parameterTypes[0] as Class<out Event>
                handlerCache[eventClass] = method
            }
        }
    }

    private fun dispatchEvent(event: Event) {
        if (!isRelated(event)) return

        val handler = handlerCache[event.javaClass]
        if (handler != null) {
            try {
                handler.invoke(this, event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (event is Cancellable) {
            cancel(event)
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    protected fun cancel(event: Event) {
        if (event is Cancellable) {
            val player = getPlayerFromEvent(event)
            if (player != null) {
                when (event) {
                    is PlayerJoinEvent, is PlayerQuitEvent, is PlayerTeleportEvent -> {}
                    else -> if (allowBypass(player)) return
                }
            }
            event.isCancelled = true
        }
    }

    protected fun getPlayerFromEvent(event: Event): Player? {
        return when (event) {
            is PlayerEvent -> event.player
            is AsyncChatEvent -> event.player
            is InventoryClickEvent -> event.whoClicked as? Player
            is BlockBreakEvent -> event.player
            is BlockPlaceEvent -> event.player
            is BlockIgniteEvent -> event.player
            is HangingPlaceEvent -> event.player
            is HangingBreakByEntityEvent -> event.remover as? Player
            is EntityDamageByEntityEvent -> event.damager as? Player
            is EntityEvent -> event.entity as? Player
            is VehicleEnterEvent -> event.entered as? Player
            is VehicleDestroyEvent -> event.attacker as? Player
            is VehicleDamageEvent -> event.attacker as? Player
            else -> {
                try {
                    val method = event::class.java.getMethod("getPlayer")
                    method.invoke(event) as? Player
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    protected fun isRelated(event: Event): Boolean {
        try {
            if (event is AsyncChatEvent || event is AsyncPlayerPreLoginEvent) return true

            val player = getPlayerFromEvent(event)
            if (player != null && allowBypass(player)) {
                return false
            }

            if (event is PlayerJoinEvent || event is PlayerQuitEvent || event is PlayerKickEvent) return true

            if (event is PlayerDeathEvent) {
                val p = event.entity
                return isPlayerInContext(p) || isLocationInContext(p.location)
            }

            if (event is PlayerEvent) {
                return isPlayerInContext(event.player) || isLocationInContext(event.player.location)
            }

            if (event is EntityEvent) {
                val entity = event.entity
                if (entity is Player) return isPlayerInContext(entity) || isLocationInContext(entity.location)
                return isLocationInContext(entity.location)
            }

            if (event is BlockEvent) {
                return isLocationInContext(event.block.location)
            }

            if (event is InventoryClickEvent) {
                val who = event.whoClicked
                if (who is Player) return isPlayerInContext(who) || isLocationInContext(who.location)
            }

            val cls = event::class.java
            val tryGet = listOf("getPlayer", "getEntity", "getBlock", "getLocation", "getWhoClicked")
            for (name in tryGet) {
                try {
                    val m = cls.getMethod(name)
                    val res = m.invoke(event) ?: continue
                    when (res) {
                        is Player -> return isPlayerInContext(res) || isLocationInContext(res.location)
                        is Entity -> return isLocationInContext(res.location)
                        is Block -> return isLocationInContext(res.location)
                        is Location -> return isLocationInContext(res)
                    }
                } catch (_: NoSuchMethodException) {
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return false
    }

    // ==========================================
    // OVERRIDABLE EVENT HANDLERS
    // ==========================================

    open fun onBlockBreak(event: BlockBreakEvent) = cancel(event)
    open fun onBlockPlace(event: BlockPlaceEvent) = cancel(event)
    open fun onBlockBurn(event: BlockBurnEvent) = cancel(event)
    open fun onBlockFade(event: BlockFadeEvent) = cancel(event)
    open fun onBlockForm(event: BlockFormEvent) = cancel(event)
    open fun onBlockFromTo(event: BlockFromToEvent) = cancel(event)
    open fun onBlockGrow(event: BlockGrowEvent) = cancel(event)
    open fun onBlockIgnite(event: BlockIgniteEvent) = cancel(event)
    open fun onBlockPhysics(event: BlockPhysicsEvent) = cancel(event)
    open fun onBlockSpread(event: BlockSpreadEvent) = cancel(event)
    open fun onBlockPistonExtend(event: BlockPistonExtendEvent) = cancel(event)
    open fun onBlockPistonRetract(event: BlockPistonRetractEvent) = cancel(event)

    open fun onFoodLevelChange(event: FoodLevelChangeEvent) = cancel(event)
    open fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {}
    open fun onPlayerJoin(event: PlayerJoinEvent) {}
    open fun onPlayerQuit(event: PlayerQuitEvent) {}
    open fun onPlayerKick(event: PlayerKickEvent) {}
    open fun onPlayerDeath(event: PlayerDeathEvent) { cancel(event) }
    open fun onPlayerMove(event: PlayerMoveEvent) {}
    open fun onPlayerTeleport(event: PlayerTeleportEvent) {}
    open fun onAsyncChat(event: AsyncChatEvent) { cancel(event) }
    open fun onPlayerInteract(event: PlayerInteractEvent) = cancel(event)
    open fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) = cancel(event)
    open fun onPlayerDropItem(event: PlayerDropItemEvent) = cancel(event)
    open fun onPlayerRespawn(event: PlayerRespawnEvent) = cancel(event)
    open fun onPlayerItemConsume(event: PlayerItemConsumeEvent) = cancel(event)
    open fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) = cancel(event)
    open fun onPlayerFish(event: PlayerFishEvent) = cancel(event)
    open fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {}
    open fun onPlayerBedEnter(event: PlayerBedEnterEvent) = cancel(event)
    open fun onPlayerBedLeave(event: PlayerBedLeaveEvent) = cancel(event)

    open fun onEntityDamage(event: EntityDamageEvent) = cancel(event)
    open fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) = cancel(event)
    open fun onEntityDeath(event: EntityDeathEvent) = cancel(event)
    open fun onCreatureSpawn(event: CreatureSpawnEvent) = cancel(event)
    open fun onEntityTarget(event: EntityTargetEvent) = cancel(event)
    open fun onEntityCombust(event: EntityCombustEvent) = cancel(event)
    open fun onEntityExplode(event: EntityExplodeEvent) = cancel(event)
    open fun onEntityInteract(event: EntityInteractEvent) = cancel(event)
    open fun onProjectileLaunch(event: ProjectileLaunchEvent) = cancel(event)
    open fun onProjectileHit(event: ProjectileHitEvent) = cancel(event)
    open fun onEntityTame(event: EntityTameEvent) = cancel(event)
    open fun onEntityPickupItem(event: EntityPickupItemEvent) = cancel(event)

    open fun onInventoryClick(event: InventoryClickEvent) {}
    open fun onInventoryClose(event: InventoryCloseEvent) {}
    open fun onInventoryOpen(event: InventoryOpenEvent) {}
    open fun onInventoryDrag(event: InventoryDragEvent) {}
    open fun onFurnaceBurn(event: FurnaceBurnEvent) = cancel(event)
    open fun onFurnaceSmelt(event: FurnaceSmeltEvent) = cancel(event)
    open fun onPrepareItemCraft(event: PrepareItemCraftEvent) = cancel(event)
    open fun onCraftItem(event: CraftItemEvent) = cancel(event)

    open fun onWeatherChange(event: WeatherChangeEvent) = cancel(event)
    open fun onThunderChange(event: ThunderChangeEvent) = cancel(event)
    open fun onChunkLoad(event: ChunkLoadEvent) = cancel(event)
    open fun onChunkUnload(event: ChunkUnloadEvent) = cancel(event)
    open fun onWorldSave(event: WorldSaveEvent) = cancel(event)
    open fun onWorldLoad(event: WorldLoadEvent) = cancel(event)
    open fun onWorldUnload(event: WorldUnloadEvent) = cancel(event)
    open fun onStructureGrow(event: StructureGrowEvent) = cancel(event)

    open fun onServerListPing(event: ServerListPingEvent) = cancel(event)
    open fun onPluginEnable(event: PluginEnableEvent) = cancel(event)
    open fun onPluginDisable(event: PluginDisableEvent) = cancel(event)

    open fun onVehicleEnter(event: VehicleEnterEvent) = cancel(event)
    open fun onVehicleExit(event: VehicleExitEvent) = cancel(event)
    open fun onVehicleMove(event: VehicleMoveEvent) = cancel(event)
    open fun onVehicleDamage(event: VehicleDamageEvent) = cancel(event)
    open fun onVehicleCreate(event: VehicleCreateEvent) = cancel(event)
    open fun onVehicleDestroy(event: VehicleDestroyEvent) = cancel(event)

    open fun onHangingBreak(event: HangingBreakEvent) = cancel(event)
    open fun onHangingPlace(event: HangingPlaceEvent) = cancel(event)
    open fun onEnchantItem(event: EnchantItemEvent) = cancel(event)
    open fun onPortalCreate(event: PortalCreateEvent) = cancel(event)
}