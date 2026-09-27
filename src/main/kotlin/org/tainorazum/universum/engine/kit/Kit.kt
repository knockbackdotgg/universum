package org.tainorazum.universum.engine.kit

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.engine.kit.database.DB_Kits.getDefaultKitJson
import org.tainorazum.universum.engine.kit.database.DB_Kits.getKitJson
import org.tainorazum.universum.engine.kit.database.DB_Kits.getMaterial
import org.tainorazum.universum.engine.kit.util.KitSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.mta
import java.util.concurrent.CompletableFuture

data class Kit(
    val id: String,
    val material: Material,
    val canCustomize: Boolean,
    val itemAccess: Boolean,
) {

    /**
     * @Usage Apply usable kit to player.
     */
    fun applyToPlayer(player: Player, isDefault: Boolean = false): CompletableFuture<Boolean> {
        return getKitJson(id, player.uniqueId).thenCompose { personalJson ->
            if (personalJson != null && !isDefault) {
                applyJsonToPlayer(player, personalJson)
            } else {
                getDefaultKitJson(id).thenCompose { defaultJson ->
                    if (defaultJson != null) {
                        applyJsonToPlayer(player, defaultJson)
                    } else {
                        CompletableFuture.completedFuture(false)
                    }
                }
            }
        }
    }

    private fun applyJsonToPlayer(player: Player, json: String): CompletableFuture<Boolean> {
        return mta {
            try {
                val (contents, armor, off) = KitSerializer.jsonToInventory(json)

                // Safe assignment for main inventory contents (handles array length mismatches)
                val currentContents = player.inventory.contents
                val safeContents = arrayOfNulls<ItemStack>(currentContents.size)
                contents.copyInto(safeContents, endIndex = safeContents.size.coerceAtMost(contents.size))
                player.inventory.contents = safeContents

                // Safe assignment for armor contents
                val currentArmor = player.inventory.armorContents
                val safeArmor = arrayOfNulls<ItemStack>(currentArmor.size)
                armor.copyInto(safeArmor, endIndex = safeArmor.size.coerceAtMost(armor.size))
                player.inventory.armorContents = safeArmor

                // Safe offhand assignment
                player.inventory.setItemInOffHand(off)

                player.updateInventory()
                true
            } catch (e: Exception) {
                // Fallback: Clear inventory on invalid/corrupted kit JSON instead of throwing an error
                player.inventory.clear()
                player.updateInventory()
                false
            }
        }
    }

}
