package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object Colours {
    val AQUA: TextColor = TextColor.fromHexString("#42D9C8")!!
    val WHITE: TextColor = TextColor.fromHexString("#F3F7F0")!!
    val YELLOW: TextColor = TextColor.fromHexString("#FFEE63")!!
    val GREEN: TextColor = TextColor.fromHexString("#55FF55")!!
    val DARK_GREEN: TextColor = TextColor.fromHexString("#226F54")!!
    val BLUE: TextColor = TextColor.fromHexString("#384B70")!!
    val LIGHT_BLUE: TextColor = TextColor.fromHexString("#00c0f5")!!
    val SKY_BLUE: TextColor = TextColor.fromHexString("#7EC8FF")!!
    val PURPLE: TextColor = TextColor.fromHexString("#6735CE")!!
    val MAGENTA: TextColor = TextColor.fromHexString("#B119C2")!!
    val LIGHT_PURPLE: TextColor = TextColor.fromHexString("#EE57FF")!!
    val RED: TextColor = TextColor.fromHexString("#F23A3A")!!
    val GOLD: TextColor = TextColor.fromHexString("#FF9800")!!
    val ORANGE: TextColor = TextColor.fromHexString("#F19C79")!!
    val PINK: TextColor = TextColor.fromHexString("#FF53FD")!!
    val GREY: TextColor = TextColor.fromHexString("#9ea4ad")!!
    val DARK_GREY: TextColor = TextColor.fromHexString("#555555")!!

    val ALL: Map<String, TextColor> = mapOf(
        "yellow" to YELLOW,
        "gold" to GOLD,
        "white" to WHITE,
        "red" to RED,
        "orange" to ORANGE,
        "green" to GREEN,
        "dark_green" to DARK_GREEN,
        "blue" to BLUE,
        "aqua" to AQUA,
        "light_purple" to LIGHT_PURPLE,
        "purple" to PURPLE,
        "magenta" to MAGENTA,
        "pink" to PINK,
        "grey" to GREY,
        "light_blue" to LIGHT_BLUE,
        "sky_blue" to SKY_BLUE,
        "dark_grey" to DARK_GREY
    )

    fun fromName(name: String?): TextColor? = ALL[name?.lowercase()]

    fun tagResolver(): TagResolver = TagResolver.builder().apply {
        ALL.forEach { (name, color) ->
            resolver(Placeholder.styling(name, color))
        }
    }.build()
}