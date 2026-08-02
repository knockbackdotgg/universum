package org.tainorazum.universum.utility.other

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.GUI.Companion.openInventory
import org.bukkit.entity.Player

fun GUI.open(p : Player) {
    p.openInventory(this)
}
