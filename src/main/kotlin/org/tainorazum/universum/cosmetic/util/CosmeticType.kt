package org.tainorazum.universum.cosmetic.util

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.EntityType

enum class CosmeticType {
    TAG,
    PET,
    TRAIL,
    AURA,
    ARMOR_TRIMS,
    ARMOR_TRIM_COLOR
}

sealed class CosmeticData {
    data class Tag(val component: Component) : CosmeticData()

    data class Pet(
        val entityType: EntityType,
        val displayName: Component,
        val isBaby: Boolean = false
    ) : CosmeticData()

    data class ArmorTrim(val material: Material) : CosmeticData()

    data class ArmorTrimColor(val material: Material) : CosmeticData()

    data class Trail(val particleName: String) : CosmeticData()
    data class Aura(val particleName: String) : CosmeticData()
}