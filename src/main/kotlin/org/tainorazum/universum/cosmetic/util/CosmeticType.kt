package org.tainorazum.universum.cosmetic.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.tainorazum.universum.utility.text.Colours

enum class CosmeticType {
    TAG,
    PET,
    TRAIL,
    AURA,
    GLOW,
    ARMOR_TRIMS,
    ARMOR_TRIM_COLOR
}

sealed class CosmeticData {
    data class Tag(val component: Component): CosmeticData()

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