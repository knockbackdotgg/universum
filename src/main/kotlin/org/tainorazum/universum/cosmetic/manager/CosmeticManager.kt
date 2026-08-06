package org.tainorazum.universum.cosmetic.manager

import net.kyori.adventure.text.Component
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CosmeticManager {

    /**
     * Retrieves the active cosmetic payload/object for a player.
     * - [CosmeticType.TAG] -> Returns formatted [Component] bracketed with dark grey: `[Tag]`
     * - [CosmeticType.PET] -> Returns [CosmeticData.Pet]
     * - [CosmeticType.ARMOR_TRIMS], [CosmeticType.ARMOR_TRIM_COLOR] -> Returns [org.bukkit.Material]
     * - [CosmeticType.TRAIL], [CosmeticType.AURA] -> Returns their respective [CosmeticData] objects
     */
    fun get(uuid: UUID, type: CosmeticType): CompletableFuture<Any?> {
        return DB_PlayerCosmetics.getEnabled(uuid, type).thenCompose { enabledName ->
            if (enabledName == null) {
                CompletableFuture.completedFuture(null)
            } else {
                DB_Cosmetics.getCosmetic(type, enabledName).thenApply { data ->
                    when (data) {
                        is CosmeticData.Tag -> {
                            Component.text("[", Colours.DARK_GREY)
                                .append(data.component)
                                .append(Component.text("] ", Colours.DARK_GREY))
                        }

                        is CosmeticData.Pet -> data

                        is CosmeticData.ArmorTrim -> data.material

                        is CosmeticData.ArmorTrimColor -> data.material

                        is CosmeticData.Trail -> data

                        is CosmeticData.Aura -> data

                        null -> {
                            println("[Cosmetics] Enabled cosmetic '$enabledName' not found in database for $uuid.")
                            null
                        }
                    }
                }
            }
        }
    }
}