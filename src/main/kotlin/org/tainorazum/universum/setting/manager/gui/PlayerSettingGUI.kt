package org.tainorazum.universum.setting.manager.gui

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.setting.DB_PlayerSettings
import org.tainorazum.universum.setting.PlayerSetting
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import java.util.concurrent.CompletableFuture

object PlayerSettingGUI {

    /**
     * Asynchronously loads player setting data and constructs the GUI.
     */
    fun settingGUI(player: Player, previous: GUI? = null): CompletableFuture<GUI> {
        val titleText = "Player Settings"

        val pattern = listOf(
            "#########",
            "#SSSSSSS#",
            "#########",
            "#SSSSSSS#",
            "#########",
            "B########"
        )

        val settingSlots = pattern.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'S') (rowIndex * 9) + colIndex else null
            }
        }

        val settingsList = PlayerSetting.entries
        val playerUuid = player.uniqueId

        // Fetch all setting futures asynchronously
        val settingFutures = settingsList.map { setting ->
            DB_PlayerSettings.getSetting(playerUuid, setting).thenApply { valStr -> setting to valStr }
        }

        return CompletableFuture.allOf(*settingFutures.toTypedArray()).thenApply {
            val settingsMap = settingFutures.map { it.join() }.toMap()

            gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
                pattern(*pattern.toTypedArray())

                if (previous != null) {
                    set('B', item(Material.ARROW, Text.toSmallTextComponent("Go Back", Colours.RED), "Click to return to the previous menu.")) {
                        isCancelled = true
                        previous.open(player)
                    }
                } else {
                    set('B', item(Material.BARRIER, Text.toSmallTextComponent("Close", Colours.RED), "Click to close this menu.")) {
                        isCancelled = true
                        player.closeInventory()
                    }
                }

                settingSlots.forEachIndexed { index, slotIndex ->
                    if (index < settingsList.size) {
                        val setting = settingsList[index]
                        val currentVal = settingsMap[setting] ?: (setting.options.firstOrNull() ?: "")

                        set(slotIndex, createSettingItem(setting, currentVal)) {
                            isCancelled = true

                            val nextValue = getNextOption(setting, currentVal)

                            // Async DB Update
                            DB_PlayerSettings.setSetting(playerUuid, setting, nextValue).thenAccept { success ->
                                if (success) {
                                    // Ensure UI reopen happens safely on Bukkit Main Thread
                                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                                        openGUI(player, previous)
                                    })
                                }
                            }
                        }
                    } else {
                        set(slotIndex, item(Material.BARRIER, Text.toSmallTextComponent("No Setting Available", Colours.DARK_GREY), emptyList())) {
                            isCancelled = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility method to open the async GUI safely on the Bukkit main thread.
     */
    fun openGUI(player: Player, previous: GUI? = null) {
        settingGUI(player, previous).thenAccept { gui ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                gui.open(player)
            })
        }
    }

    private fun createSettingItem(setting: PlayerSetting, currentValue: String): ItemStack {
        val loreList = mutableListOf(Component.empty())

        setting.options.forEach { option ->
            if (option.equals(currentValue, ignoreCase = true)) {
                loreList.add(Component.text("▶ $option", Colours.GREEN))
            } else {
                loreList.add(Component.text("  $option", Colours.GREY))
            }
        }

        loreList.add(Component.empty())
        loreList.add(Component.text("Click to cycle setting.", Colours.DARK_GREY))

        return item(
            Material.COMPARATOR,
            Text.toSmallTextComponent(setting.name.lowercase().replaceFirstChar { it.uppercase() }, Colours.RED),
            loreList
        )
    }

    private fun getNextOption(setting: PlayerSetting, current: String): String {
        val options = setting.options
        if (options.isEmpty()) return ""

        val currentIndex = options.indexOfFirst { it.equals(current, ignoreCase = true) }
        if (currentIndex == -1) return options.first()

        val nextIndex = (currentIndex + 1) % options.size
        return options[nextIndex]
    }

    private fun item(material: Material, name: Component, lore: String): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name)
                meta.lore(
                    listOf(
                        Component.empty(),
                        Component.text(lore, Colours.GREY),
                        Component.empty()
                    )
                )
            }
        }
    }

    private fun item(material: Material, name: Component, lore: List<Component>): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name)
                meta.lore(lore)
            }
        }
    }
}