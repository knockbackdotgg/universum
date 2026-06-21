package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object Text {

    fun NEXT_LINE() = Component.text("\n")

    fun toSmallText(text: String) : String {
        val alphabetShort = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ⁰¹²³⁴⁵⁶⁷⁸⁹"
        val alphabetNormal = "abcdefghijklmnopqrstuvwxyz0123456789"
        text.lowercase()

        return text.map { ch ->
            val lower = ch.lowercaseChar()
            val index = alphabetNormal.indexOf(lower)
            if (index != -1) alphabetShort[index] else ch
        }.joinToString("")

    }

    fun showLoadingScreen(player: Player, text: String) {
        player.addPotionEffect(org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS,
            Int.MAX_VALUE,
            1,
            false,
            false
        ))

        val loadingTitle = net.kyori.adventure.title.Title.title(
            Component.text("█".repeat(20), NamedTextColor.BLACK),
            Component.text(text, Colours.GREEN),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ZERO,
                java.time.Duration.ofDays(1),
                java.time.Duration.ZERO
            )
        )
        player.showTitle(loadingTitle)
        player.playSound(player.location, org.bukkit.Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 2.0f)
    }

    fun toSmallTextComponent(text: String, color: TextColor) : Component {
        val alphabetShort = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ"
        val alphabetNormal = "abcdefghijklmnopqrstuvwxyz"
        text.lowercase();

        return Component.text(
            text.map { ch ->
                val lower = ch.lowercaseChar()
                val index = alphabetNormal.indexOf(lower)
                if (index != -1) alphabetShort[index] else ch
            }.joinToString(""), color
        )


    }

}