package org.tainorazum.universum.utility.gui

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.view.AnvilView
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.UUID

object AnvilInput : Listener {

    private val activeInputs = mutableMapOf<UUID, (String) -> Unit>()

    fun init(plugin: Universum) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun openTextInput(
        player: Player,
        title: String,
        defaultText: String = "",
        onComplete: (String) -> Unit
    ) {
        val view = player.openAnvil(null, true) ?: return
        view.title = Text.toSmallText(title)

        val inventory = view.topInventory
        inventory.setItem(0, item(material = Material.PAPER, name = Component.text(defaultText, Colours.YELLOW).norm()))

        activeInputs[player.uniqueId] = onComplete
    }

    fun openIntInput(
        player: Player,
        title: String,
        defaultVal: Int = 0,
        onComplete: (Int) -> Unit
    ) {
        openTextInput(player, title, defaultVal.toString()) { input ->
            val number = input.toIntOrNull()
            if (number != null && number >= 0) {
                onComplete(number)
            } else {
                player.sendMessage(Component.text("Invalid number entered!", Colours.RED).norm())
            }
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val callback = activeInputs[player.uniqueId] ?: return

        val anvilView = event.view as? AnvilView ?: return

        // Prevent moving/taking items out of the top anvil inventory
        if (event.clickedInventory == anvilView.topInventory) {
            event.isCancelled = true

            // Slot 2 is the output slot
            if (event.rawSlot == 2) {
                val input = anvilView.renameText?.trim() ?: ""

                activeInputs.remove(player.uniqueId)
                anvilView.topInventory.clear()
                player.closeInventory()

                if (input.isNotEmpty()) {
                    callback(input)
                }
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        // Clearing top inventory on close stops Minecraft from putting the paper into the player's inventory
        if (activeInputs.remove(event.player.uniqueId) != null) {
            event.inventory.clear()
        }
    }
}