package org.tainorazum.universum.lobby

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
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
import org.tainorazum.universum.hologram.HologramPageManager
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.lobby.npc.NpcBouncer
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.setting.DB_PlayerSettings
import org.tainorazum.universum.utility.other.formatDate
import org.tainorazum.universum.utility.other.toHumanReadable
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import kotlin.use

class LobbyManager {

    var success: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        private set

    private val players = mutableListOf<Player>()

    init {
        registerEventListener()
        startNpcBouncer()
        build().thenAccept {
            if (it) success.complete(true)
        }.exceptionally { ex ->
            Universum.instance.logger.severe("Failed to build lobby schematic: ${ex.message}")
            ex.printStackTrace()
            success.complete(false)
            null
        }
    }
    // interaction

    fun enter(player: Player): Boolean {
        return players.add(player)
    }

    fun leave(player: Player): Boolean {
        return players.remove(player)
    }

    // map

    private fun build(): CompletableFuture<Boolean> {
        val targetLocationFuture = DB_Lobby.getCenterLocation()
        val schematicBytesFuture = DB_Lobby.getSchematicBytes()

        return CompletableFuture.allOf(targetLocationFuture, schematicBytesFuture).thenCompose {
            val targetLocation = targetLocationFuture.join()
            val schematicBytes = schematicBytesFuture.join()

            if (schematicBytes == null || schematicBytes.isEmpty()) {
                Universum.instance.logger.warning("Lobby schematic bytes are empty or null! Skipping paste.")
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            if (targetLocation.world == null) {
                Universum.instance.logger.severe("Target world for lobby schematic paste is null!")
                return@thenCompose CompletableFuture.completedFuture(false)
            }

            pasteSchematicTo(schematicBytes, targetLocation, ignoreAir = false)
        }.exceptionally { ex ->
            Universum.instance.logger.severe("Failed to paste lobby schematic: ${ex.cause?.message ?: ex.message}")
            ex.printStackTrace()
            false
        }
    }

    private fun pasteSchematicTo(
        schematicBytes: ByteArray,
        targetLocation: Location,
        ignoreAir: Boolean = false
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val world = targetLocation.world ?: run {
            future.completeExceptionally(IllegalArgumentException("Target location world cannot be null."))
            return future
        }

        val worldRef = BukkitAdapter.adapt(world)

        CompletableFuture.runAsync {
            try {
                ByteArrayInputStream(schematicBytes).use { inputStream ->
                    BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(inputStream).use { reader ->
                        val clipboard = reader.read() ?: throw NullPointerException("Clipboard returned null")

                        val region = clipboard.region
                        val schemCenter = region.center
                        val originToCenterOffset = schemCenter.subtract(clipboard.origin.toVector3())

                        val bukkitTargetVector = BlockVector3.at(targetLocation.x, targetLocation.y, targetLocation.z)
                        val adjustedPastePosition = bukkitTargetVector.subtract(originToCenterOffset.toBlockPoint())

                        WorldEdit.getInstance().newEditSession(worldRef).use { editSession ->
                            val operation = ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(adjustedPastePosition)
                                .ignoreAirBlocks(ignoreAir)
                                .copyEntities(false)
                                .build()

                            Operations.complete(operation)
                        }
                    }
                }
                future.complete(true)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    // npcs

    private fun startNpcBouncer() {
        NpcBouncer.startBouncerTask(
            radius = 1.5,
            launchStrength = 1.5,
            upwardStrength = 0.4
        )
    }

    // events

    private fun registerEventListener() {
        val listener = object : Listener {}
        val eventClasses = listOf(
            BlockBreakEvent::class.java, BlockPlaceEvent::class.java, BlockBurnEvent::class.java, BlockFadeEvent::class.java,
            BlockFormEvent::class.java, BlockFromToEvent::class.java, BlockGrowEvent::class.java, BlockIgniteEvent::class.java,
            BlockPhysicsEvent::class.java, BlockSpreadEvent::class.java, BlockPistonExtendEvent::class.java, BlockPistonRetractEvent::class.java,
            PlayerJoinEvent::class.java, PlayerQuitEvent::class.java, PlayerKickEvent::class.java, PlayerDeathEvent::class.java,
            PlayerMoveEvent::class.java, PlayerTeleportEvent::class.java, AsyncChatEvent::class.java, AsyncPlayerPreLoginEvent::class.java,
            PlayerInteractEvent::class.java, PlayerInteractEntityEvent::class.java, PlayerDropItemEvent::class.java,
            PlayerRespawnEvent::class.java, PlayerItemConsumeEvent::class.java, PlayerSwapHandItemsEvent::class.java,
            FoodLevelChangeEvent::class.java,
            PlayerFishEvent::class.java, PlayerCommandPreprocessEvent::class.java, PlayerBedEnterEvent::class.java, PlayerBedLeaveEvent::class.java,
            EntityDamageEvent::class.java, EntityDamageByEntityEvent::class.java, EntityDeathEvent::class.java,
            CreatureSpawnEvent::class.java, EntityTargetEvent::class.java, EntityCombustEvent::class.java,
            EntityExplodeEvent::class.java, EntityInteractEvent::class.java, ProjectileLaunchEvent::class.java,
            ProjectileHitEvent::class.java, EntityTameEvent::class.java, EntityPickupItemEvent::class.java,
            InventoryClickEvent::class.java, InventoryCloseEvent::class.java, InventoryOpenEvent::class.java,
            InventoryDragEvent::class.java, FurnaceBurnEvent::class.java, FurnaceSmeltEvent::class.java,
            PrepareItemCraftEvent::class.java, CraftItemEvent::class.java, WeatherChangeEvent::class.java, ThunderChangeEvent::class.java
        )

        eventClasses.forEach { eventClass ->
            try {
                Universum.instance.server.pluginManager.registerEvent(
                    eventClass,
                    listener,
                    org.bukkit.event.EventPriority.HIGH,
                    { _, event -> dispatchEvent(event) },
                    Universum.instance
                )
            } catch (e: Exception) {
                Universum.instance.logger.warning("Failed to register event listener for ${eventClass.simpleName}:${e.message}")
            }
        }
    }

    // blocks
    fun BLOCK_BREAK_EVENT(event: BlockBreakEvent) { cancel(event) }
    fun BLOCK_PLACE_EVENT(event: BlockPlaceEvent) { cancel(event) }
    fun BLOCK_BURN_EVENT(event: BlockBurnEvent) { cancel(event) }
    fun BLOCK_FADE_EVENT(event: BlockFadeEvent) { cancel(event) }
    fun BLOCK_FORM_EVENT(event: BlockFormEvent) { cancel(event) }
    fun BLOCK_FROM_TO_EVENT(event: BlockFromToEvent) { cancel(event) }
    fun BLOCK_GROW_EVENT(event: BlockGrowEvent) { cancel(event) }
    fun BLOCK_IGNITE_EVENT(event: BlockIgniteEvent) { cancel(event) }
    fun BLOCK_PHYSICS_EVENT(event: BlockPhysicsEvent) { cancel(event) }
    fun BLOCK_SPREAD_EVENT(event: BlockSpreadEvent) { cancel(event) }
    fun BLOCK_PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    fun BLOCK_PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    // player

    fun FOOD_LEVEL_CHANGE_EVENT(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
        cancel(event)
    }
    fun ASYNC_PLAYER_PRE_LOGIN_EVENT(event: AsyncPlayerPreLoginEvent) {
        try {
            val result = Universum.playerManager.allowJoin(event.uniqueId).get()

            when (result) {
                "ALLOWED" -> {
                    event.loginResult = AsyncPlayerPreLoginEvent.Result.ALLOWED

                    try {
                        Universum.playerSettingManager.cacheSettings(event.uniqueId).get()
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        Universum.instance.logger.severe("Failed to cache settings for ${event.uniqueId}: ${cause.message}")

                        event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            Messages.SETTINGS_INVALID_MSG()
                        )
                    }
                }
                "WHITELISTED" -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Messages.WHITELIST_MSG())
                "BANNED" -> {
                    var msg: Component = Messages.BANNED_MSG_SHORT()
                    val punishment = Universum.punishmentManager.isBanned(event.uniqueId).get()
                    if (punishment != null) {
                        msg = Messages.BANNED_MSG()
                            .replace("<id>", punishment.id.toString())
                            .replace("<type>", punishment.type.toString())
                            .replace("<reason>", punishment.reason)
                            .replace("<duration>", punishment.duration?.toHumanReadable() ?: "/")
                            .replace("<banned>", formatDate(punishment.punishedOn))
                            .replace("<unbanned>", punishment.expiry()?.let { formatDate(it) } ?: "NEVER")
                    }
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, msg)
                }
                else -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.KICKED_MSG())
            }
        } catch (e: Exception) {
            Universum.instance.logger.severe("Error checking player login data for ${event.uniqueId}: ${e.message}")
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Error checking player data."))
        }
    }

    fun PLAYER_JOIN_EVENT(event: PlayerJoinEvent) {
        enter(event.player)

        //event.joinMessage(Messages.JOIN_MSG().replace("<name>", event.player.name))
        NetworkPlayerManager.broadcastJoin(event.player.name)
        event.player.gameMode = GameMode.SPECTATOR

        DB_Lobby.getSpawnpointLocation().thenAccept { loc ->
            Universum.instance.server.scheduler.runTask(Universum.instance, Runnable {
                PlayerUtility.reset(event.player, GameMode.SURVIVAL)
                event.player.teleport(loc)
            })
        }
    }

    fun PLAYER_QUIT_EVENT(event: PlayerQuitEvent) {
        leave(event.player)

        //event.quitMessage(Messages.LEAVE_MSG().replace("<name>", event.player.name))
        NetworkPlayerManager.broadcastLeave(event.player.name)

        HologramPageManager.clearPlayer(event.player.uniqueId)

        DB_PlayerSettings.invalidateCache(event.player.uniqueId) // change after network player leave update
    }

    fun PLAYER_KICK_EVENT(event: PlayerKickEvent) {
        leave(event.player)
    }

    fun PLAYER_DEATH_EVENT(event: PlayerDeathEvent) {
        event.deathMessage(null)
        try { event.drops.clear() } catch (_: Exception) {}
        DB_Lobby.getSpawnpointLocation().thenAccept { loc ->
            Universum.instance.server.scheduler.runTask(Universum.instance, Runnable {
                PlayerUtility.reset(event.player, GameMode.SURVIVAL)
                event.player.teleport(loc)
            })
        }
        cancel(event)
    }

    fun PLAYER_MOVE_EVENT(event: PlayerMoveEvent) { }
    fun PLAYER_TELEPORT_EVENT(event: PlayerTeleportEvent) { }

    fun ASYNC_CHAT_EVENT(event: AsyncChatEvent) {
        cancel(event)

        val rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message())
        Universum.chatManager.executeMessage(event.player.uniqueId, rawMessage)
    }

    fun PLAYER_INTERACT_EVENT(event: PlayerInteractEvent) { cancel(event) }
    fun PLAYER_INTERACT_ENTITY_EVENT(event: PlayerInteractEntityEvent) { cancel(event) }
    fun PLAYER_DROP_ITEM_EVENT(event: PlayerDropItemEvent) { cancel(event) }
    fun PLAYER_RESPAWN_EVENT(event: PlayerRespawnEvent) { cancel(event) }
    fun PLAYER_ITEM_CONSUME_EVENT(event: PlayerItemConsumeEvent) { cancel(event) }
    fun PLAYER_SWAP_HAND_ITEMS_EVENT(event: PlayerSwapHandItemsEvent) { cancel(event) }
    fun PLAYER_FISH_EVENT(event: PlayerFishEvent) { cancel(event) }
    fun PLAYER_COMMAND_PREPROCESS_EVENT(event: PlayerCommandPreprocessEvent) { }
    fun PLAYER_BED_ENTER_EVENT(event: PlayerBedEnterEvent) { cancel(event) }
    fun PLAYER_BED_LEAVE_EVENT(event: PlayerBedLeaveEvent) { cancel(event) }

    // entity
    fun ENTITY_DAMAGE_EVENT(event: EntityDamageEvent) { cancel(event) }
    fun ENTITY_DAMAGE_BY_ENTITY_EVENT(event: EntityDamageByEntityEvent) { cancel(event) }
    fun ENTITY_DEATH_EVENT(event: EntityDeathEvent) { cancel(event) }
    fun CREATURE_SPAWN_EVENT(event: CreatureSpawnEvent) { cancel(event) }
    fun ENTITY_TARGET_EVENT(event: EntityTargetEvent) { cancel(event) }
    fun ENTITY_COMBUST_EVENT(event: EntityCombustEvent) { cancel(event) }
    fun ENTITY_EXPLODE_EVENT(event: EntityExplodeEvent) { cancel(event) }
    fun ENTITY_INTERACT_EVENT(event: EntityInteractEvent) { cancel(event) }
    fun PROJECTILE_LAUNCH_EVENT(event: ProjectileLaunchEvent) { cancel(event) }
    fun PROJECTILE_HIT_EVENT(event: ProjectileHitEvent) { cancel(event) }
    fun ENTITY_TAME_EVENT(event: EntityTameEvent) { cancel(event) }
    fun ENTITY_PICKUP_ITEM_EVENT(event: EntityPickupItemEvent) { cancel(event) }

    // inventory & crafting
    fun INVENTORY_CLICK_EVENT(event: InventoryClickEvent) { }
    fun INVENTORY_CLOSE_EVENT(event: InventoryCloseEvent) { }
    fun INVENTORY_OPEN_EVENT(event: InventoryOpenEvent) { }
    fun INVENTORY_DRAG_EVENT(event: InventoryDragEvent) { }
    fun FURNACE_BURN_EVENT(event: FurnaceBurnEvent) { cancel(event) }
    fun FURNACE_SMELT_EVENT(event: FurnaceSmeltEvent) { cancel(event) }
    fun PREPARE_ITEM_CRAFT_EVENT(event: PrepareItemCraftEvent) { cancel(event) }
    fun CRAFT_ITEM_EVENT(event: CraftItemEvent) { cancel(event) }

    // weather & world
    fun WEATHER_CHANGE_EVENT(event: WeatherChangeEvent) { cancel(event) }
    fun THUNDER_CHANGE_EVENT(event: ThunderChangeEvent) { cancel(event) }
    fun CHUNK_LOAD_EVENT(event: ChunkLoadEvent) { cancel(event) }
    fun CHUNK_UNLOAD_EVENT(event: ChunkUnloadEvent) { cancel(event) }
    fun WORLD_SAVE_EVENT(event: WorldSaveEvent) { cancel(event) }
    fun WORLD_LOAD_EVENT(event: WorldLoadEvent) { cancel(event) }
    fun WORLD_UNLOAD_EVENT(event: WorldUnloadEvent) { cancel(event) }
    fun STRUCTURE_GROW_EVENT(event: StructureGrowEvent) { cancel(event) }

    // server & plugin
    fun SERVER_LIST_PING_EVENT(event: ServerListPingEvent) { cancel(event) }
    fun PLUGIN_ENABLE_EVENT(event: PluginEnableEvent) { cancel(event) }
    fun PLUGIN_DISABLE_EVENT(event: PluginDisableEvent) { cancel(event) }

    // vehicle
    fun VEHICLE_ENTER_EVENT(event: VehicleEnterEvent) { cancel(event) }
    fun VEHICLE_EXIT_EVENT(event: VehicleExitEvent) { cancel(event) }
    fun VEHICLE_MOVE_EVENT(event: VehicleMoveEvent) { cancel(event) }
    fun VEHICLE_DAMAGE_EVENT(event: VehicleDamageEvent) { cancel(event) }
    fun VEHICLE_CREATE_EVENT(event: VehicleCreateEvent) { cancel(event) }
    fun VEHICLE_DESTROY_EVENT(event: VehicleDestroyEvent) { cancel(event) }

    // hanging
    fun HANGING_BREAK_EVENT(event: HangingBreakEvent) { cancel(event) }
    fun HANGING_PLACE_EVENT(event: HangingPlaceEvent) { cancel(event) }

    // misc
    fun ENCHANT_ITEM_EVENT(event: EnchantItemEvent) { cancel(event) }
    fun PORTAL_CREATE_EVENT(event: PortalCreateEvent) { cancel(event) }
    fun PISTON_EXTEND_EVENT(event: BlockPistonExtendEvent) { cancel(event) }
    fun PISTON_RETRACT_EVENT(event: BlockPistonRetractEvent) { cancel(event) }

    private fun cancel(event: Event) {
        if (event is Cancellable) {
            val player = getPlayerFromEvent(event)
            when {
                event !is PlayerJoinEvent &&
                        event !is PlayerQuitEvent &&
                        event !is PlayerTeleportEvent -> {
                    if (player != null && Universum.playerManager.allowBypassLobby(player)) return
                }
            }

            event.isCancelled = true
        }
    }

    private fun isPlayerInLobby(player: Player) = players.contains(player)
    private fun isLocationInLobby(location: Location) = DB_Lobby.isLocationInLobby(location)

    private fun getPlayerFromEvent(event: Event): Player? {
        return when (event) {
            // Standard Player & Chat events
            is PlayerEvent -> event.player
            is AsyncChatEvent -> event.player
            is InventoryClickEvent -> event.whoClicked as? Player

            // Block events created by players
            is BlockBreakEvent -> event.player
            is BlockPlaceEvent -> event.player
            is BlockIgniteEvent -> event.player

            // Hanging entity events (e.g., paintings, item frames)
            is HangingPlaceEvent -> event.player
            is HangingBreakByEntityEvent -> event.remover as? Player

            // Entity damage/interact events where a player is the damager/cause
            is EntityDamageByEntityEvent -> event.damager as? Player
            is EntityEvent -> event.entity as? Player

            // Vehicle events where a player is driving/entering/destroying
            is VehicleEnterEvent -> event.entered as? Player
            is VehicleDestroyEvent -> event.attacker as? Player
            is VehicleDamageEvent -> event.attacker as? Player

            // Fallback: Use reflection for any custom or missing event containing a `getPlayer` method
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

    private fun isRelated(event: Event): Boolean {
        try {
            if (event is AsyncChatEvent) {
                return true
            }

            val player = getPlayerFromEvent(event)

            if (player != null) {
                val hasBypass = Universum.playerManager.allowBypassLobby(player)
                if (hasBypass) {
                    return false
                }
            }

            if (event is AsyncPlayerPreLoginEvent) {
                return true
            }

            if (event is PlayerJoinEvent || event is PlayerQuitEvent || event is PlayerKickEvent) {
                return true
            }

            if (event is PlayerDeathEvent) {
                val p = event.entity
                return isPlayerInLobby(p) || isLocationInLobby(p.location)
            }

            if (event is PlayerEvent) {
                return isPlayerInLobby(event.player) || isLocationInLobby(event.player.location)
            }

            if (event is EntityEvent) {
                val entity = event.entity
                if (entity is Player) return isPlayerInLobby(entity) || isLocationInLobby(entity.location)
                return isLocationInLobby(entity.location)
            }

            if (event is BlockEvent) {
                return isLocationInLobby(event.block.location)
            }

            if (event is InventoryClickEvent) {
                val who = event.whoClicked
                if (who is Player) return isPlayerInLobby(who) || isLocationInLobby(who.location)
            }

            val cls = event::class.java
            val tryGet = listOf("getPlayer", "getEntity", "getBlock", "getLocation", "getWhoClicked")
            for (name in tryGet) {
                try {
                    val m = cls.getMethod(name)
                    val res = m.invoke(event) ?: continue
                    when (res) {
                        is Player -> return isPlayerInLobby(res) || isLocationInLobby(res.location)
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
            e.printStackTrace()
        }

        if (event is Cancellable) {
            event.isCancelled = true
        }
    }
}