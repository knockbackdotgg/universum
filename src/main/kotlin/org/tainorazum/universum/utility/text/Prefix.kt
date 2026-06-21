package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.tainorazum.core.Core
import net.tainorazum.core.util.Colours

object Prefix {

    fun ARROW() = Component.text(" → ", Colours.GREY)
    fun ARROW2() = Component.text(" ▶ ", Colours.GREY)
    fun ARROW3() = Component.text(" » ", Colours.GREY)
    fun MANAGE() = Component.text(Text.toSmallText("manage"), Colours.RED)
    fun CORE() = Component.text(Text.toSmallText("core"), Colours.AQUA)
    fun FFA() = Component.text("FFA", Colours.LIGHT_BLUE)
    fun FRIEND() = Component.text(Text.toSmallText("friend"), Colours.GOLD)
    fun COSMETIC() = Component.text(Text.toSmallText("cosmetic"), Colours.GOLD)
    fun SETTINGS() = Component.text(Text.toSmallText("settings"), Colours.GOLD)
    fun PUNISHMENTS() = Component.text(Text.toSmallText("punishments"), Colours.GOLD)
    fun QUESTS() = Component.text(Text.toSmallText("quests"), Colours.GOLD)
    fun STATS() = Component.text(Text.toSmallText("stats"), Colours.GREEN)
    fun STAFF() = Component.text(Text.toSmallText("staff"), Colours.GREEN)

}