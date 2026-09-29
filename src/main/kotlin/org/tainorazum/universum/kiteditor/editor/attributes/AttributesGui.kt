package org.tainorazum.universum.kiteditor.editor.attributes

import gg.flyte.twilight.gui.GUI.Companion.openInventory
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.tainorazum.universum.utility.gui.GuiUtility
import org.tainorazum.universum.utility.other.isEnchantable
import org.tainorazum.universum.utility.player.PlayerUtility.msg
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class AttributeType(val displayName: String, val icon: Material) {
    ENCHANTMENT("Enchantments", Material.ENCHANTED_BOOK),
    AMOUNT("Item Amount", Material.PAPER),
    ROCKET_FLIGHT_DURATION("Flight Duration", Material.FIREWORK_ROCKET)
}

object AttributesGui {

    private val openInventories = ConcurrentHashMap<UUID, Inventory>()

    private fun registerInventory(player: Player) {
        val inventory = player.openInventory.topInventory
        openInventories[player.uniqueId] = inventory
    }

    fun clear(player: Player) {
        openInventories.remove(player.uniqueId)
    }

    fun isAttributesGui(player: Player): Boolean {
        val registeredInventory = openInventories[player.uniqueId] ?: return false
        return player.openInventory.topInventory === registeredInventory
    }

    /**
     * Main selector menu showing current item preview and buttons for each editable attribute.
     */
    fun open(player: Player, item: ItemStack, onSave: (ItemStack) -> Unit) {
        val currentEditingItem = item.clone()

        val gi = gui(Text.toSmallTextComponent("Modify Item Attributes", Colours.GREY), 36) {

            pattern(
                "#########",
                "#I###EAF#",
                "#########",
                "####B####"
            )

            // Preview & Quick Save
            set('I', previewItem(currentEditingItem)) {
                isCancelled = true

                player.closeInventory()
                onSave(currentEditingItem)
                clear(player)
            }

            // Enchantments Button
            if (currentEditingItem.isEnchantable()) {
                set('E', GuiUtility.item(
                    material = AttributeType.ENCHANTMENT.icon,
                    name = Text.toSmallTextComponent(AttributeType.ENCHANTMENT.displayName, Colours.YELLOW),
                    lore = listOf(Component.text("Click to modify enchantments", Colours.GREY).norm())
                )) {
                    isCancelled = true
                    openEnchantmentMenu(player, currentEditingItem, onSave)
                }
            } else {
                set('E', disabledButton("Enchantments")) {
                    isCancelled = true
                }
            }

            // Amount Button
            if (currentEditingItem.type.maxStackSize > 1) {
                set('A', GuiUtility.item(
                    material = AttributeType.AMOUNT.icon,
                    name = Text.toSmallTextComponent(AttributeType.AMOUNT.displayName, Colours.YELLOW).norm(),
                    lore = listOf(Component.text("Click to change stack count", Colours.GREY).norm())
                )) {
                    isCancelled = true
                    openAmountMenu(player, currentEditingItem, onSave)
                }
            } else {
                set('A', disabledButton("Amount")) {
                    isCancelled = true
                }
            }

            // Flight Duration Button (Only for Firework Rockets)
            if (currentEditingItem.type == Material.FIREWORK_ROCKET) {
                set('F', GuiUtility.item(
                    material = AttributeType.ROCKET_FLIGHT_DURATION.icon,
                    name = Text.toSmallTextComponent(AttributeType.ROCKET_FLIGHT_DURATION.displayName, Colours.YELLOW),
                    lore = listOf(Component.text("Click to change flight duration", Colours.GREY).norm())
                )) {
                    isCancelled = true
                    openFlightDurationMenu(player, currentEditingItem, onSave)
                }
            } else {
                set('F', disabledButton("Flight Duration")) {
                    isCancelled = true
                }
            }

            // Save & Confirm Button
            set('S', GuiUtility.item(
                material = Material.LIME_DYE,
                name = Text.toSmallTextComponent("Save Changes", Colours.GREEN),
                lore = listOf(Text.toSmallTextComponent("Click to apply modifications", Colours.GREY).norm())
            )) {
                isCancelled = true

                player.closeInventory()
                onSave(currentEditingItem)
                clear(player)
            }

            set('B', GuiUtility.item(
                material = Material.BARRIER,
                name = Text.toSmallTextComponent("Cancel", Colours.RED),
                lore = listOf(Component.text("Cancel modifying this item.", Colours.GREY).norm())
            )) {
                isCancelled = true
                player.closeInventory()
            }
        }

        player.openInventory(gi)
        registerInventory(player)
    }

    /**
     * Step 1: Displays up to 7 valid enchantment groups for the item with right-click cycling.
     */
    fun openEnchantmentMenu(
        player: Player,
        item: ItemStack,
        onSave: (ItemStack) -> Unit,
        cycleIndices: Map<Int, Int> = emptyMap()
    ) {
        val groupedEnchants = getGroupedEnchantments(item)

        val gi = gui(Text.toSmallTextComponent("Select Enchantment", Colours.GREY), 36) {

            pattern(
                "#########",
                "#1234567#",
                "#########",
                "####B####"
            )

            for (index in 0 until 7) {
                val slotChar = (index + 1).toString().first()

                if (index < groupedEnchants.size) {
                    val group = groupedEnchants[index]

                    val appliedEnchant = group.firstOrNull { item.getEnchantmentLevel(it) > 0 }

                    val currentIndex = if (appliedEnchant != null) {
                        group.indexOf(appliedEnchant)
                    } else {
                        (cycleIndices[index] ?: 0) % group.size
                    }

                    val activeEnchant = group[currentIndex]
                    val currentLevel = item.getEnchantmentLevel(activeEnchant)
                    val isApplied = currentLevel > 0

                    val levelString = if (isApplied) "Level ${toRoman(currentLevel)}" else "Not applied"

                    val loreList = mutableListOf<Component>()
                    loreList.add(Component.text("Left-click to adjust level", Colours.DARK_GREY).norm())
                    if (group.size > 1) {
                        loreList.add(Component.text("Right-click to cycle options", Colours.DARK_GREY).norm())
                        loreList.add(Component.empty())
                        for ((i, e) in group.withIndex()) {
                            loreList.add(
                                Text.toSmallTextComponent(
                                    "▶ ${formatEnchantName(e)}",
                                    if (i == currentIndex) Colours.GREEN else Colours.GREY
                                ).norm()
                            )
                        }
                    }
                    loreList.add(Component.empty())
                    loreList.add(Component.text("Status » $levelString", if (isApplied) Colours.GREEN else Colours.YELLOW).norm())
                    loreList.add(Component.empty())

                    set(slotChar, GuiUtility.item(
                        material = Material.ENCHANTED_BOOK,
                        name = Text.toSmallTextComponent(formatEnchantName(activeEnchant), if (isApplied) Colours.GREEN else Colours.YELLOW),
                        lore = loreList,
                        enchanted = isApplied
                    )) {
                        isCancelled = true

                        if (isRightClick && group.size > 1) {
                            if (appliedEnchant != null) {
                                item.removeEnchantment(appliedEnchant)
                            }

                            val nextIndex = (currentIndex + 1) % group.size
                            openEnchantmentMenu(player, item, onSave, cycleIndices + (index to nextIndex))
                        } else {
                            openEnchantmentLevelMenu(player, item, activeEnchant, onSave)
                        }
                    }
                } else {
                    set(slotChar, disabledButton("Unavailable Slot")) {
                        isCancelled = true
                    }
                }
            }

            // Back Button
            set('B', GuiUtility.item(
                material = Material.BARRIER,
                name = Text.toSmallTextComponent("Back", Colours.RED),
                lore = emptyList()
            )) {
                isCancelled = true
                open(player, item, onSave)
            }

        }
        player.openInventory(gi)
    }

    /**
     * Step 2: Level selection GUI (0 / Remove up to maxLevel).
     */
    fun openEnchantmentLevelMenu(
        player: Player,
        item: ItemStack,
        enchantment: Enchantment,
        onSave: (ItemStack) -> Unit
    ) {
        val currentLevel = item.getEnchantmentLevel(enchantment)
        val maxLevel = enchantment.maxLevel

        val initial = item.clone()

        val gi = gui(Text.toSmallTextComponent("level: ${formatEnchantName(enchantment)}", Colours.GREY), 36) {

            pattern(
                "#########",
                "#0123456#",
                "#########",
                "####B####"
            )

            set('0', GuiUtility.item(
                material = Material.RED_STAINED_GLASS_PANE,
                name = Text.toSmallTextComponent("Remove Enchantment", Colours.RED),
                lore = listOf(
                    Component.text(if (currentLevel == 0) "Currently not applied" else "Click to remove", Colours.GREY).norm()
                ),
                enchanted = currentLevel == 0
            )) {
                isCancelled = true
                item.removeEnchantment(enchantment)
                openEnchantmentMenu(player, item, onSave)
            }

            for (level in 1..6) {
                val slotChar = level.toString().first()

                if (level <= maxLevel) {
                    val isSelected = currentLevel == level

                    set(slotChar, GuiUtility.item(
                        material = if (isSelected) Material.EXPERIENCE_BOTTLE else Material.GLASS_BOTTLE,
                        name = Text.toSmallTextComponent("Level ${toRoman(level)}", if (isSelected) Colours.GREEN else Colours.YELLOW),
                        lore = listOf(
                            Component.text(if (isSelected) "Currently selected" else "Click to set to level ${toRoman(level)}", Colours.GREY).norm()
                        ),
                        enchanted = isSelected
                    )) {
                        isCancelled = true

                        val conflictingEnchantments = item.enchantments.keys.filter { conflicting ->
                            conflicting != enchantment &&
                                    (conflicting.conflictsWith(enchantment) || enchantment.conflictsWith(conflicting))
                        }

                        conflictingEnchantments.forEach { conflicting ->
                            item.removeEnchantment(conflicting)
                        }

                        item.addUnsafeEnchantment(enchantment, level)
                        openEnchantmentMenu(player, item, onSave)

                    }
                } else {
                    set(slotChar, GuiUtility.item(
                        material = Material.GRAY_STAINED_GLASS_PANE,
                        name = Text.toSmallTextComponent("Level ${toRoman(level)}", Colours.GREY),
                        lore = listOf(Text.toSmallTextComponent("Max level is ${toRoman(maxLevel)}", Colours.GREY).norm())
                    )) {
                        isCancelled = true
                    }
                }
            }

            // Back Button
            set('B', GuiUtility.item(
                material = Material.BARRIER,
                name = Text.toSmallTextComponent("Back", Colours.RED),
                lore = listOf(Component.text("Go back to previous menu and revert changes.", Colours.GREY).norm())
            )) {
                isCancelled = true
                openEnchantmentMenu(player, initial, onSave)
            }
        }

        player.openInventory(gi)
    }

    /**
     * Flight Duration Selection GUI (1 - 3 seconds for Firework Rockets)
     */
    fun openFlightDurationMenu(player: Player, item: ItemStack, onSave: (ItemStack) -> Unit) {
        val meta = item.itemMeta as? FireworkMeta ?: return
        val currentPower = if (meta.power in 1..3) meta.power else 1

        val initial = item.clone()

        val gi = gui(Text.toSmallTextComponent("Select Flight Duration", Colours.GREY), 36) {

            pattern(
                "#########",
                "##1#2#3##",
                "#########",
                "####B####"
            )

            for (power in 1..3) {
                val isSelected = currentPower == power
                val char = power.toString().first()

                set(char, GuiUtility.item(
                    material = if (isSelected) Material.FIREWORK_ROCKET else Material.GUNPOWDER,
                    name = Text.toSmallTextComponent("Flight Duration: $power", if (isSelected) Colours.GREEN else Colours.YELLOW),
                    lore = listOf(
                        Component.text(if (isSelected) "Currently Selected" else "Click to set power $power", Colours.GREY).norm()
                    ),
                    enchanted = isSelected
                )) {
                    isCancelled = true
                    val updatedMeta = item.itemMeta as FireworkMeta
                    updatedMeta.power = power
                    item.itemMeta = updatedMeta

                    open(player, item, onSave)
                }
            }

            // Back button
            set('B', GuiUtility.item(
                material = Material.BARRIER,
                name = Text.toSmallTextComponent("Back", Colours.RED),
                lore = listOf(Component.text("Go back to previous menu and revert changes.", Colours.GREY).norm())
            )) {
                isCancelled = true
                open(player, initial, onSave)
            }

        }

        player.openInventory(gi)
    }

    /**
     * Item Amount Modification GUI
     */
    fun openAmountMenu(player: Player, item: ItemStack, onSave: (ItemStack) -> Unit) {
        val maxStack = item.type.maxStackSize

        val initial = item.clone()

        val gi = gui(Text.toSmallTextComponent("Modify Item Amount", Colours.GREY), 36) {

            pattern(
                "#########",
                "#-#I#+#M#",
                "#########",
                "####B####"
            )

            set('-', GuiUtility.item(
                material = Material.RED_STAINED_GLASS_PANE,
                name = Component.text("-1 Amount", Colours.RED).norm(),
                lore = emptyList()
            )) {
                isCancelled = true
                if (item.amount > 1) item.amount -= 1
                openAmountMenu(player, item, onSave)
            }

            set('I', previewItem(item)) {
                isCancelled = true
                open(player, item, onSave)
            }

            set('+', GuiUtility.item(
                material = Material.GREEN_STAINED_GLASS_PANE,
                name = Component.text("+1 Amount", Colours.GREEN).norm(),
                lore = emptyList()
            )) {
                isCancelled = true
                if (item.amount < maxStack) item.amount += 1
                openAmountMenu(player, item, onSave)
            }

            set('M', GuiUtility.item(
                material = Material.CHEST,
                name = Component.text("Set to Max ($maxStack)", Colours.YELLOW).norm(),
                lore = emptyList()
            )) {
                isCancelled = true
                item.amount = maxStack
                openAmountMenu(player, item, onSave)
            }

            set('B', GuiUtility.item(
                material = Material.BARRIER,
                name = Text.toSmallTextComponent("Back", Colours.RED),
                lore = listOf(Component.text("Go back to previous menu and revert changes.", Colours.GREY).norm())
            )) {
                isCancelled = true
                open(player, initial, onSave)
            }
        }
        player.openInventory(gi)
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private fun getGroupedEnchantments(item: ItemStack): List<List<Enchantment>> {
        val applicable = Registry.ENCHANTMENT
            .filter { it.canEnchantItem(item) && !it.isCursed }
            .toList()

        val visited = mutableSetOf<Enchantment>()
        val groups = mutableListOf<MutableList<Enchantment>>()

        for (enchant in applicable) {
            if (enchant in visited) continue

            val group = mutableListOf(enchant)
            visited.add(enchant)

            for (other in applicable) {
                if (other in visited) continue
                if (group.any { it.conflictsWith(other) || other.conflictsWith(it) }) {
                    group.add(other)
                    visited.add(other)
                }
            }
            groups.add(group)
        }
        return groups.take(7)
    }

    private fun formatEnchantName(enchantment: Enchantment): String {
        return enchantment.key.key
            .replace('_', ' ')
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun disabledButton(label: String): ItemStack {
        return GuiUtility.item(
            material = Material.GRAY_STAINED_GLASS_PANE,
            name = Text.toSmallTextComponent(label, Colours.GREY),
            lore = listOf(Component.text("Not available for this item", Colours.GREY).norm())
        )
    }

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            else -> number.toString()
        }
    }

    private fun previewItem(item: ItemStack): ItemStack {
        return item.clone().apply {
            editMeta { meta ->
                val current = meta.displayName()?.norm() ?: Component.text(
                    type.name.lowercase().split('_').joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } },
                    Colours.GREY
                ).norm()
                meta.displayName(current.append(Text.toSmallTextComponent(" [Preview]", Colours.YELLOW).norm()))

                val loreList = (meta.lore() ?: emptyList()).toMutableList()
                loreList.add(Component.empty())
                loreList.add(Component.text("Click to apply changes.", Colours.YELLOW).norm())
                loreList.add(Component.empty())

                meta.lore(loreList)
            }
        }
    }
}