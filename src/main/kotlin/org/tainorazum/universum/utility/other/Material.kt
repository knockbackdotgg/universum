package org.tainorazum.universum.utility.other

import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.kiteditor.editor.attributes.AttributeType

fun ItemStack.getAvailableAttributes(): List<AttributeType> {
    val list = mutableListOf<AttributeType>()

    if (type == Material.FIREWORK_ROCKET) {
        list.add(AttributeType.ROCKET_FLIGHT_DURATION)
    }

    if (type.maxStackSize > 1) {
        list.add(AttributeType.AMOUNT)
    }

    if (isEnchantable()) {
        list.add(AttributeType.ENCHANTMENT)
    }

    return list
}

fun ItemStack.hasAdditionalAttributes(): Boolean {
    return getAvailableAttributes().isNotEmpty()
}

fun ItemStack.hasAdjustableFlightDuration(): Boolean {
    if (this.type.isAir) return false

    return (this.type == Material.FIREWORK_ROCKET)
}

fun ItemStack.hasAdjustableAmount(): Boolean {
    if (this.type.isAir) return false

    return (this.maxStackSize > 1)
}

fun ItemStack.isEnchantable(): Boolean {
    if (this.type.isAir) return false

    return Registry.ENCHANTMENT.any { enchantment ->
        enchantment.canEnchantItem(this)
    }
}

