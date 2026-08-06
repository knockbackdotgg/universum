package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

object Icon {

    fun SINGLE_SWORD(color: TextColor) = Component.text("\uD83D\uDDE1", color)
    fun SKULL(color: TextColor) = Component.text("☠", color)
    fun DOUBLE_SWORD(color: TextColor) = Component.text("⚔", color)
    fun STAR(color: TextColor) = Component.text("⭐", color)
    fun HEART(color: TextColor) = Component.text("❤", color)
    fun DIAMOND(color: TextColor) = Component.text("♦", color)
    fun MOON(color: TextColor) = Component.text("☽", color)
    fun LIGHTNING(color: TextColor) = Component.text("⚡", color)
    fun CROWN(color: TextColor) = Component.text("\uD83D\uDC51", color).decoration(TextDecoration.BOLD, true)
    fun CHECKMARK(color: TextColor) = Component.text("✔", color)
    fun FIRE(color: TextColor) = Component.text("\uD83D\uDD25", color)
    fun ERROR(color: TextColor) = Component.text("⚠", color)
    fun BOW(color: TextColor) = Component.text("\uD83C\uDFF9", color)

}