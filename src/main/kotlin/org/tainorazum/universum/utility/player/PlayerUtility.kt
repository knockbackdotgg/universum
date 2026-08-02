package org.tainorazum.universum.utility.player

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.tainorazum.universum.Universum
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.text.Messages
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object PlayerUtility {

    // blackout

    private data class BlackoutSession(
        val originalMode: GameMode,
        val cameraEntity: ArmorStand
    )

    private val activeSessions = ConcurrentHashMap<UUID, BlackoutSession>()

    fun blackout(
        player: Player,
        durationMs: Int,
        fadeOutMs: Int,
        title: Component = Component.empty(),
        subTitle: Component = Component.empty()
    ) {
        val originalMode = player.gameMode
        val loc = player.location

        // 1. Spawn a light server-side entity locked inside a solid black block location
        val cameraEntity = loc.world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
        cameraEntity.isInvisible = true
        cameraEntity.isMarker = true
        cameraEntity.setGravity(false)
        cameraEntity.isInvulnerable = true

        activeSessions[player.uniqueId] = BlackoutSession(originalMode, cameraEntity)

        // 2. Send client-side BLACK_CONCRETE at camera entity head to force 100% solid pitch-black suffocation
        player.sendBlockChange(cameraEntity.eyeLocation, Material.BLACK_CONCRETE.createBlockData())

        // 3. Switch client to Spectator & lock camera (Disables F5 & movement)
        setClientGamemode(player, GameMode.SPECTATOR)
        setCamera(player, cameraEntity.entityId)

        // 4. Send Title Overlay
        val times = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(durationMs.toLong()),
            Duration.ofMillis(fadeOutMs.toLong())
        )
        player.showTitle(Title.title(title, subTitle, times))

        // 5. Schedule Cleanup on Main Thread
        val durationTicks = (durationMs / 50).coerceAtLeast(1).toLong()
        Universum.instance.server.scheduler.runTaskLater(Universum.instance, Runnable {
            stopBlackout(player)
        }, durationTicks)
    }

    private fun stopBlackout(player: Player) {
        val session = activeSessions.remove(player.uniqueId) ?: return

        // 1. Reset Camera back to player entity
        setCamera(player, player.entityId)

        // 2. Restore Client Block & Gamemode
        player.sendBlockChange(session.cameraEntity.eyeLocation, session.cameraEntity.eyeLocation.block.blockData)
        setClientGamemode(player, session.originalMode)

        // 3. Clean up camera anchor entity
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

        packet.gameStateIDs.write(0, 3) // 3 = Change Game Mode
        packet.float.write(0, mode.value.toFloat())

        pm.sendServerPacket(player, packet)
    }

    // reset

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

        player.exp = exp // visual xp bar

        player.totalExperience = totalExperience // raw xp points

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

    // permission

    fun allowed(actor: BukkitCommandActor, permission: Permission): CompletableFuture<Boolean> {
        val player = actor.asPlayer() ?: return CompletableFuture.completedFuture(true)
        return permission.check(player.uniqueId)
    }

    inline fun guard(
        actor: BukkitCommandActor,
        permission: Permission,
        crossinline action: () -> Unit
    ) {
        allowed(actor, permission).thenAccept { isAllowed ->
            if (isAllowed) action()
            else {
                val player = actor.requirePlayer()
                player.sendMessage(Messages.NO_PERMISSION_COMMAND())
            }
        }
    }

    // uuid

    fun getName(uuid: UUID): CompletableFuture<String?> {
        val profile = Bukkit.createPlayerProfile(uuid)

        return profile.update().thenApply { updatedProfile ->
            if (updatedProfile.uniqueId != null && updatedProfile.name != null) {
                updatedProfile.name
            } else {
                null
            }
        }
    }

    fun getUUID(username: String): CompletableFuture<UUID?> {
        val profile = Bukkit.createPlayerProfile(username)

        return profile.update().thenApply { updatedProfile ->
            if (updatedProfile.uniqueId != null && updatedProfile.name != null) {
                updatedProfile.uniqueId
            } else {
                null
            }
        }
    }

}