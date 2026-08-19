package org.tainorazum.universum.cosmetic.manager

import net.kyori.adventure.text.Component
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CosmeticManager {

    /**
     * Retrieves the active cosmetic payload/object for a player.
     * - [CosmeticType.CHAT_TAG] -> Returns formatted [Component] bracketed with dark grey: `[Tag]`
     * - [CosmeticType.PET] -> Returns [CosmeticData.Pet]
     * - [CosmeticType.ARMOR_TRIMS], [CosmeticType.ARMOR_TRIM_COLOR] -> Returns [org.bukkit.Material]
     * - [CosmeticType.TRAIL], [CosmeticType.AURA] -> Returns their respective [CosmeticData] objects
     */
    fun get(uuid: UUID, type: CosmeticType): CompletableFuture<Any?> {
        return DB_PlayerCosmetics.get(uuid, type)
    }

}