package org.tainorazum.universum.utility.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.norm

object GuiUtility {

    fun item(material: Material, name: Component, lore: String): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name.norm())
                meta.lore(
                    listOf(
                        Component.text(lore, Colours.GREY).norm(),
                        Component.empty().norm()
                    )
                )
            }
        }
    }

    fun item(material: Material, name: Component, lore: List<Component>, showFlags: Boolean = false): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name.norm())
                meta.lore(lore)
                if(!showFlags) {
                    meta.addItemFlags(*ItemFlag.entries.toTypedArray())
                }
            }
        }
    }

}