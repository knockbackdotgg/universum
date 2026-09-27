package org.tainorazum.universum.utility.player

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.tainorazum.universum.Universum
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.server.server.ServerType
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Messages
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object PlayerUtility {

    // =========================================================================
    // Name & UUID Cache
    // =========================================================================

    private val uuidToNameCache = ConcurrentHashMap<UUID, String>()
    private val nameToUuidCache = ConcurrentHashMap<String, UUID>()

    fun cachePlayer(uuid: UUID, name: String) {
        uuidToNameCache[uuid] = name
        nameToUuidCache[name.lowercase(Locale.ROOT)] = uuid
    }

    fun uncachePlayer(uuid: UUID) {
        val name = uuidToNameCache.remove(uuid)
        if (name != null) {
            nameToUuidCache.remove(name.lowercase(Locale.ROOT))
        }
    }

    fun uncachePlayer(username: String) {
        val uuid = nameToUuidCache.remove(username.lowercase(Locale.ROOT))
        if (uuid != null) {
            uuidToNameCache.remove(uuid)
        }
    }

    fun clearNameAndUuidCache() {
        uuidToNameCache.clear()
        nameToUuidCache.clear()
    }

    // =========================================================================
    // UUID / Name Resolution Methods (Cached)
    // =========================================================================

    fun getName(uuid: UUID): CompletableFuture<String?> {
        val cachedName = uuidToNameCache[uuid]
        if (cachedName != null) {
            return CompletableFuture.completedFuture(cachedName)
        }

        val onlinePlayer = Bukkit.getPlayer(uuid)
        if (onlinePlayer != null) {
            cachePlayer(uuid, onlinePlayer.name)
            return CompletableFuture.completedFuture(onlinePlayer.name)
        }

        val profile = Bukkit.createPlayerProfile(uuid)
        return profile.update().thenApply { updatedProfile ->
            val name = updatedProfile.name
            if (updatedProfile.uniqueId != null && name != null) {
                cachePlayer(uuid, name)
                name
            } else {
                null
            }
        }
    }

    fun getUUID(username: String): CompletableFuture<UUID?> {
        val lowerName = username.lowercase(Locale.ROOT)

        val cachedUuid = nameToUuidCache[lowerName]
        if (cachedUuid != null) {
            return CompletableFuture.completedFuture(cachedUuid)
        }

        val onlinePlayer = Bukkit.getPlayerExact(username)
        if (onlinePlayer != null) {
            cachePlayer(onlinePlayer.uniqueId, onlinePlayer.name)
            return CompletableFuture.completedFuture(onlinePlayer.uniqueId)
        }

        val profile = Bukkit.createPlayerProfile(username)
        return profile.update().thenApply { updatedProfile ->
            val fetchedUuid = updatedProfile.uniqueId
            val fetchedName = updatedProfile.name
            if (fetchedUuid != null && fetchedName != null) {
                cachePlayer(fetchedUuid, fetchedName)
                fetchedUuid
            } else {
                null
            }
        }
    }

    // =========================================================================
    // Blackout (Display Entity Driven)
    // =========================================================================

// =========================================================================
    // Blackout (Display Entity Driven)
    // =========================================================================

    private data class BlackoutSession(
        val originalMode: GameMode,
        val cameraEntity: ArmorStand,
        val displayEntity: BlockDisplay,
        val originalWalkSpeed: Float,
        val originalFlySpeed: Float,
        val originalLocation: Location,
        val lockTask: BukkitTask,
        val autoStopTask: BukkitTask,
        val hiddenPlayerUuids: MutableSet<UUID>
    )

    private val activeSessions = ConcurrentHashMap<UUID, BlackoutSession>()

    fun handlePlayerJoin(joinedPlayer: Player) {
        if (activeSessions.isEmpty()) return

        for ((blackoutUuid, session) in activeSessions) {
            val blackoutPlayer = Bukkit.getPlayer(blackoutUuid) ?: continue
            if (joinedPlayer.uniqueId != blackoutUuid) {
                blackoutPlayer.hidePlayer(Universum.instance, joinedPlayer)
                joinedPlayer.hidePlayer(Universum.instance, blackoutPlayer)
                session.hiddenPlayerUuids.add(joinedPlayer.uniqueId)
            }
        }
    }

    fun handlePlayerQuit(quittingPlayer: Player) {
        // If the quitting player is currently in a blackout, clean up their session
        if (activeSessions.containsKey(quittingPlayer.uniqueId)) {
            stopBlackout(quittingPlayer)
        }

        // Remove the quitting player from all active hidden sets
        activeSessions.values.forEach { session ->
            session.hiddenPlayerUuids.remove(quittingPlayer.uniqueId)
        }
    }

    fun blackout(
        player: Player,
        durationMs: Int,
        fadeOutMs: Int = 500,
        title: Component = Component.empty(),
        subTitle: Component = Component.empty()
    ) {
        if (activeSessions.containsKey(player.uniqueId)) {
            stopBlackout(player)
        }

        val originalMode = player.gameMode
        val originalWalkSpeed = player.walkSpeed
        val originalFlySpeed = player.flySpeed
        val originalLoc = player.location.clone()

        // 1. Thread-safe set for dynamically added/removed players
        val hiddenUuids = ConcurrentHashMap.newKeySet<UUID>()
        for (other in Bukkit.getOnlinePlayers()) {
            if (other.uniqueId != player.uniqueId) {
                player.hidePlayer(Universum.instance, other)
                other.hidePlayer(Universum.instance, player)
                hiddenUuids.add(other.uniqueId)
            }
        }

        // 2. Anchor location far away in dark bounds facing forward (0 pitch, 0 yaw)
        val anchorLoc = Location(originalLoc.world, originalLoc.x, 310.0, originalLoc.z, 0f, 0f)

        // 3. Spawn invisible camera stand
        val cameraEntity = anchorLoc.world.spawnEntity(anchorLoc, EntityType.ARMOR_STAND) as ArmorStand
        cameraEntity.isInvisible = true
        cameraEntity.isMarker = true
        cameraEntity.setGravity(false)

        // 4. Spawn a scaled BlockDisplay engulfing the camera with forced 0 light (Matte Black)
        val displayEntity = anchorLoc.world.spawn(anchorLoc.clone().add(0.0, 1.0, 0.0), BlockDisplay::class.java) { display ->
            display.block = Material.BLACK_CONCRETE.createBlockData()

            display.transformation = Transformation(
                Vector3f(-3f, -3f, -3f),
                AxisAngle4f(0f, 0f, 0f, 1f),
                Vector3f(6f, 6f, 6f),
                AxisAngle4f(0f, 0f, 0f, 1f)
            )

            display.brightness = Display.Brightness(0, 0)
        }

        // 5. Freeze movement speeds and apply potion effects
        val effectTicks = (durationMs / 50) + 40
        player.walkSpeed = 0f
        player.flySpeed = 0f
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, effectTicks, 255, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, effectTicks, 255, false, false, false))

        setClientGamemode(player, GameMode.SPECTATOR)
        setCamera(player, cameraEntity.entityId)

        // 6. Lock player position & view rotation every tick
        val lockTask = Universum.instance.server.scheduler.runTaskTimer(Universum.instance, Runnable {
            if (!player.isOnline) return@Runnable
            player.teleport(anchorLoc)
        }, 0L, 1L)

        // 7. Send Title Overlay
        val times = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(durationMs.toLong()),
            Duration.ofMillis(fadeOutMs.toLong())
        )
        player.showTitle(Title.title(title, subTitle, times))

        // 8. Schedule Auto-Cleanup
        val durationTicks = (durationMs / 50).coerceAtLeast(1).toLong()
        val autoStopTask = Universum.instance.server.scheduler.runTaskLater(Universum.instance, Runnable {
            stopBlackout(player)
        }, durationTicks)

        activeSessions[player.uniqueId] = BlackoutSession(
            originalMode = originalMode,
            cameraEntity = cameraEntity,
            displayEntity = displayEntity,
            originalWalkSpeed = originalWalkSpeed,
            originalFlySpeed = originalFlySpeed,
            originalLocation = originalLoc,
            lockTask = lockTask,
            autoStopTask = autoStopTask,
            hiddenPlayerUuids = hiddenUuids
        )
    }

    fun stopBlackout(player: Player) {
        val session = activeSessions.remove(player.uniqueId) ?: return

        session.lockTask.cancel()
        session.autoStopTask.cancel()

        for (uuid in session.hiddenPlayerUuids) {
            val other = Bukkit.getPlayer(uuid)
            if (other != null && other.isOnline) {
                player.showPlayer(Universum.instance, other)
                other.showPlayer(Universum.instance, player)
            }
        }

        setCamera(player, player.entityId)
        setClientGamemode(player, session.originalMode)
        player.walkSpeed = session.originalWalkSpeed
        player.flySpeed = session.originalFlySpeed
        player.teleport(session.originalLocation)

        player.clearTitle()
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removePotionEffect(PotionEffectType.SLOWNESS)

        session.displayEntity.remove()
        session.cameraEntity.remove()
    }

    private fun setCamera(player: Player, entityId: Int) {
        val pm = ProtocolLibrary.getProtocolManager()
        val packet = pm.createPacket(PacketType.Play.Server.CAMERA)
        packet.integers.write(0, entityId)
        pm.sendServerPacket(player, packet)
    }

    private fun setClientGamemode(player: Player, mode: GameMode) {
        val pm = ProtocolLibrary.getProtocolManager()
        val packet = pm.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE)
        packet.gameStateIDs.write(0, 3)
        packet.float.write(0, mode.value.toFloat())
        pm.sendServerPacket(player, packet)
    }

    // =========================================================================
    // Reset
    // =========================================================================

    fun reset(
        player: Player,
        gameMode: GameMode,
        health: Double = 20.0,
        foodLevel: Int = 20,
        saturation: Float = 20f,
        exhaustion: Float = 0f,
        isFlying: Boolean = false,
        allowFlight: Boolean = false,
        clearInventory: Boolean = true,
        armorContents: Array<ItemStack?> = arrayOfNulls(4),
        itemInOffHand: ItemStack = ItemStack(Material.AIR),
        itemOnCursor: ItemStack = ItemStack(Material.AIR),
        heldItemSlot: Int = 0,
        clearEnderPearls: Boolean = true,
        arrowsInBody: Int = 0,
        clearPotionEffects: Boolean = true,
        velocity: Vector = Vector(0.0, 0.0, 0.0),
        fireTicks: Int = 0,
        freezeTicks: Int = 0,
        fallDistance: Float = 0f,
        remainingAir: Int = 300,
        maximumAir: Int = 300,
        noDamageTicks: Int = 0,
        maximumNoDamageTicks: Int = 20,
        exp: Float = 0f,
        totalExperience: Int = 0,
        level: Int = 0,
        expCooldown: Int = 0,
        healthScale: Double = 20.0,
        setHealthScaled: Boolean = false,
        sprinting: Boolean = false,
        sneaking: Boolean = false,
        gliding: Boolean = false,
        swimming: Boolean = false,
        collidable: Boolean = true,
        invulnerable: Boolean = false,
        gravity: Boolean = true,
        walkSpeed: Float = 0.2f,
        flySpeed: Float = 0.1f,
        resetAttackCooldown: Boolean = true,
        portalCooldown: Int = 0,
        updateInventory: Boolean = true
    ) {
        player.gameMode = gameMode
        player.health = health
        player.foodLevel = foodLevel
        player.saturation = saturation
        player.exhaustion = exhaustion
        player.isFlying = isFlying
        player.allowFlight = allowFlight

        if (clearInventory) {
            player.closeInventory()
            player.inventory.clear()
        }

        player.inventory.armorContents = armorContents
        player.inventory.setItemInOffHand(itemInOffHand)
        player.setItemOnCursor(itemOnCursor)
        player.inventory.heldItemSlot = heldItemSlot

        if (clearEnderPearls) {
            player.enderPearls.forEach { it.remove() }
        }

        player.arrowsInBody = arrowsInBody

        if (clearPotionEffects) {
            player.clearActivePotionEffects()
        }

        player.velocity = velocity
        player.fireTicks = fireTicks
        player.freezeTicks = freezeTicks
        player.fallDistance = fallDistance
        player.remainingAir = remainingAir
        player.maximumAir = maximumAir
        player.noDamageTicks = noDamageTicks
        player.maximumNoDamageTicks = maximumNoDamageTicks
        player.exp = exp
        player.totalExperience = totalExperience
        player.level = level
        player.expCooldown = expCooldown
        player.healthScale = healthScale
        player.isHealthScaled = setHealthScaled
        player.isSprinting = sprinting
        player.isSneaking = sneaking
        player.isGliding = gliding
        player.isSwimming = swimming
        player.isCollidable = collidable
        player.isInvulnerable = invulnerable
        player.setGravity(gravity)
        player.walkSpeed = walkSpeed
        player.flySpeed = flySpeed

        if (resetAttackCooldown) {
            player.resetCooldown()
        }

        player.portalCooldown = portalCooldown

        if (updateInventory) {
            player.updateInventory()
        }
    }

    // =========================================================================
    // Permission
    // =========================================================================

    fun allowed(actor: BukkitCommandActor, permission: Permission): CompletableFuture<Boolean> {
        val player = actor.asPlayer() ?: return CompletableFuture.completedFuture(true)
        return permission.check(player.uniqueId)
    }

    inline fun guard(
        actor: BukkitCommandActor,
        permission: Permission,
        requireLobby: Boolean = false,
        crossinline action: () -> Unit
    ) {
        if (Universum.server.type == ServerType.LOBBY
            && requireLobby
            && !Universum.lobbyManager.isPlayerInLobby(actor.requirePlayer())
        ) {
            val player = actor.requirePlayer()
            player.sendMessage(Messages.LOBBY_REQUIRED())
            return
        }
        allowed(actor, permission).thenAccept { isAllowed ->
            if (isAllowed) action()
            else {
                val player = actor.requirePlayer()
                player.sendMessage(Messages.NO_PERMISSION_COMMAND())
            }
        }
    }

    // =========================================================================
    // Sound
    // =========================================================================

    fun Player.noSound() {
        val sound = Sound.ENTITY_VILLAGER_NO
        val volume = 0.7f
        val pitch = 1.0f

        player?.let { it.playSound(it, sound, volume, pitch) }
    }

    fun Player.sound(sound: Sound, volume: Float, pitch: Float) {
        player?.let { it.playSound(it, sound, volume, pitch) }
    }

    // =========================================================================
    // Message
    // =========================================================================

    fun Player.msg(msg: String, color: TextColor) {

        player?.sendMessage(Component.text(msg, color))

    }

}