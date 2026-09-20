package org.tainorazum.universum.utility.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.norm

object GuiUtility {

    // Make lore non-nullable String so null resolves to List<Component>?
    fun item(
        material: Material,
        name: Component? = null,
        lore: String,
        showFlags: Boolean = false,
        enchanted: Boolean = false
    ): ItemStack {
        val loreComponents = listOf(
            Component.text(lore, Colours.GREY).norm(),
            Component.empty().norm()
        )
        return item(material, name, loreComponents, showFlags, enchanted)
    }

    fun item(
        material: Material,
        name: Component? = null,
        lore: List<Component>? = null,
        showFlags: Boolean = false,
        enchanted: Boolean = false
    ): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name?.norm())
                meta.lore(lore)

                if (enchanted) {
                    meta.setEnchantmentGlintOverride(true)
                }

                if (!showFlags) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                    meta.addItemFlags(ItemFlag.HIDE_DESTROYS)
                    meta.addItemFlags(ItemFlag.HIDE_PLACED_ON)
                    meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    meta.addItemFlags(ItemFlag.HIDE_DYE)
                    meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM)
                    meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS)
                }
            }
        }
    }
}