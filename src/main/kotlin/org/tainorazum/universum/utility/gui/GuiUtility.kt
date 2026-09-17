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

    // Main method handling nullable List<Component>
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
                    meta.addItemFlags(*ItemFlag.entries.toTypedArray())
                    meta.isHideTooltip = true
                }
            }
        }
    }
}