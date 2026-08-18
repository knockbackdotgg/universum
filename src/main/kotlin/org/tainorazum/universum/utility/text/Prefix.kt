package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object Prefix {

    fun TEST() = Component.text("★", Colours.YELLOW)
    fun ERROR() = Component.text("⚠", Colours.RED)
    fun ARROW() = Component.text(" → ", Colours.GREY)
    fun ARROW2() = Component.text(" ▶ ", Colours.GREY)
    fun ARROW3() = Component.text(" » ", Colours.GREY)
    fun UNIVERSUM() = Component.text(Text.toSmallText("universum"), Colours.GREEN)
    fun ADMIN() = Component.text(Text.toSmallText("admin"), Colours.GREEN)
    fun MANAGE() = Component.text(Text.toSmallText("manage"), Colours.RED)
    fun FFA() = Component.text("FFA", Colours.LIGHT_BLUE)
    fun FRIEND() = Component.text(Text.toSmallText("friends"), Colours.GOLD)
    fun COSMETIC() = Component.text(Text.toSmallText("cosmetic"), Colours.GOLD)
    fun SETTINGS() = Component.text(Text.toSmallText("settings"), Colours.GOLD)
    fun PUNISHMENTS() = Component.text(Text.toSmallText("punishments"), Colours.GOLD)
    fun QUESTS() = Component.text(Text.toSmallText("quests"), Colours.GOLD)
    fun STATS() = Component.text(Text.toSmallText("stats"), Colours.GREEN)
    fun STAFF() = Component.text(Text.toSmallText("staff"), Colours.GREEN)

}