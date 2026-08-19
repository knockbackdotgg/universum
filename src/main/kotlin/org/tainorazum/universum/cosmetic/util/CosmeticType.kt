package org.tainorazum.universum.cosmetic.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.EntityType

enum class CosmeticType {
    CHATTAG,
    CHATCOLOR,
    PET,
    TRAIL,
    AURA,
    GLOW,
    ARMOR_TRIMS,
    ARMOR_TRIM_COLOR
}

sealed class CosmeticData {
    data class ChatTag(val component: Component): CosmeticData()

    data class ChatColor(val color: TextColor): CosmeticData()

    data class Pet(
        val entityType: EntityType,
        val displayName: Component,
        val isBaby: Boolean = false
    ): CosmeticData()

    data class ArmorTrim(val material: Material): CosmeticData()

    data class ArmorTrimColor(val material: Material): CosmeticData()

    data class Trail(val name: String, val color: TextColor): CosmeticData()
    data class Aura(val name: String, val color: TextColor): CosmeticData()
    data class Glow(val color: TextColor): CosmeticData()
}