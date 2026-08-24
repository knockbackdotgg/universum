package org.tainorazum.universum.cosmetic.manager

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.cosmetic.pet.PetManager
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CosmeticManager {

    fun get(uuid: UUID, type: CosmeticType): CompletableFuture<Any?> {
        return DB_PlayerCosmetics.get(uuid, type)
    }

    /**
     * Enables a cosmetic and triggers any entity/visual side effects (e.g., spawning pets).
     */
    fun enableCosmetic(player: Player, type: CosmeticType, name: String): CompletableFuture<Boolean> {
        return DB_PlayerCosmetics.enableCosmetic(player.uniqueId, type, name).thenApplyAsync({ success ->
            if (success && type == CosmeticType.PET) {
                // Fetch new pet data and spawn on main thread
                get(player.uniqueId, CosmeticType.PET).thenAccept { data ->
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        if (!player.isOnline) return@Runnable
                        if (data is CosmeticData.Pet) {
                            PetManager.spawnPet(player, data)
                        }
                    })
                }
            }
            success
        }, Bukkit.getScheduler().getMainThreadExecutor(Universum.instance))
    }

    /**
     * Disables a cosmetic type and removes associated entities/effects.
     */
    fun disableCosmetic(player: Player, type: CosmeticType): CompletableFuture<Boolean> {
        return DB_PlayerCosmetics.disableCosmetic(player.uniqueId, type).thenApplyAsync({ success ->
            if (success && type == CosmeticType.PET) {
                PetManager.despawnPet(player.uniqueId)
            }
            success
        }, Bukkit.getScheduler().getMainThreadExecutor(Universum.instance))
    }

    fun handleQuit(player: Player) {
        removePet(player)
    }

    fun handleJoin(player: Player) {
        refreshPet(player)
    }

      //////////////////////////
     //         PETS         //
    //////////////////////////

    /**
     * Fetches and spawns the player's equipped pet on the main thread.
     */
    fun refreshPet(player: Player) {
        get(player.uniqueId, CosmeticType.PET).thenAccept { data ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                if (!player.isOnline) return@Runnable

                if (data is CosmeticData.Pet) {
                    PetManager.spawnPet(player, data)
                } else {
                    if (data == null) {
                        Universum.instance.logger.warning("No pet found in DB for ${player.name}")
                    }
                    PetManager.despawnPet(player.uniqueId)
                }
            })
        }
    }

    /**
     * Safely despawns a player's pet on the main thread.
     */
    fun removePet(player: Player) {
        if (Bukkit.isPrimaryThread()) {
            PetManager.despawnPet(player.uniqueId)
        } else {
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                PetManager.despawnPet(player.uniqueId)
            })
        }
    }

}