package org.tainorazum.universum.command.default

import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.tainorazum.universum.hologram.HologramPageManager
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.setting.manager.gui.PlayerSettingGUI
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.player.PlayerUtility.sound
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("cycleholo", "ch")
class CycleHoloCommand {

    @CommandPlaceholder
    fun cycleHolo(
        actor: BukkitCommandActor,
        holoId: String,
        @Suggest("next", "previous") direction: String = "next"
    ) = PlayerUtility.guard(actor, Permission.CMD_DEFAULT_CYCLEHOLO) {

        val player = actor.requirePlayer()

        if(direction.lowercase() == "next") {
            HologramPageManager.cycleNext(player.uniqueId, holoId)

            val current = HologramPageManager.getPage(player.uniqueId, holoId)
            val dynamicPitch = 1.2f + (current * 0.15f)

            player.sound(
                Sound.UI_BUTTON_CLICK,
                0.7f,
                dynamicPitch
            )
        }
        else if(direction.lowercase() == "previous") {
            HologramPageManager.cyclePrev(player.uniqueId, holoId)

            val current = HologramPageManager.getPage(player.uniqueId, holoId)
            val dynamicPitch = 1.2f + (current * 0.15f)

            player.sound(
                Sound.UI_BUTTON_CLICK,
                0.7f,
                dynamicPitch
            )
        } else {
            player.sendMessage(Component.text("That is not a real direction.", Colours.RED))
            player.noSound()
        }

    }

}