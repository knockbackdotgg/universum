package org.tainorazum.universum.engine.kit

import org.bukkit.entity.Player
import org.tainorazum.universum.engine.kit.database.DB_Kits.getDefaultKitJson
import org.tainorazum.universum.engine.kit.database.DB_Kits.getKitJson
import org.tainorazum.universum.engine.kit.util.KitSerializer
import org.tainorazum.universum.utility.DatabaseUtil.mta
import java.util.concurrent.CompletableFuture

data class Kit(
    val id: String
) {

    /**
     * @Usage Apply usable kit to player.
     */
    fun applyToPlayer(player: Player): CompletableFuture<Boolean> {
        return getKitJson(id, player.uniqueId).thenCompose { personalJson ->
            if (personalJson != null) {
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
            val (contents, armor, off) = KitSerializer.jsonToInventory(json)
            player.inventory.contents = contents
            player.inventory.armorContents = armor
            player.inventory.setItemInOffHand(off)
            player.updateInventory()
            true
        }
    }

}
