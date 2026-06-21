package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class Msg(val player: Player) {

    fun inform(msg: String) {
        player.sendMessage(
            Component.text()
        )
    }

}