package org.tainorazum.universum.engine.arena.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import kotlin.math.ceil

object AdminArenaLocationsGui {

    private val dbManager = ArenaDatabaseManager()
    private val contentSlots = (10..16) + (19..25) + (28..34) + (37..43) // 28 items per page

    fun open(player: Player, arenaId: String, page: Int = 1) {
        dbManager.getArenaLocations(arenaId).thenAccept { locations ->
            val locationList = locations.entries.toList()
            val totalPages = maxOf(1, ceil(locationList.size.toDouble() / contentSlots.size).toInt())
            val currentPage = page.coerceIn(1, totalPages)

            val title = "Locations: $arenaId ($currentPage/$totalPages)"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 54) {

                // Display page items across defined content slots
                val pageItems = locationList.drop((currentPage - 1) * contentSlots.size).take(contentSlots.size)
                pageItems.forEachIndexed { index, (key, loc) ->
                    val slot = contentSlots[index]
                    val lore = listOf(
                        Component.text("World: ${loc.world?.name ?: "Unknown"}", Colours.GREY).norm(),
                        Component.text("X: ${loc.x}, Y: ${loc.y}, Z: ${loc.z}", Colours.GREY).norm(),
                        Component.text("Yaw: ${loc.yaw}, Pitch: ${loc.pitch}", Colours.DARK_GREY).norm(),
                        Component.empty(),
                        Component.text("Click to remove location.", Colours.RED).norm()
                    )

                    set(slot, item(Material.PAPER, Component.text(key, Colours.YELLOW).norm(), lore)) {
                        isCancelled = true
                        dbManager.removeArenaLocation(arenaId, key).thenAccept {
                            open(player, arenaId, currentPage)
                        }
                    }
                }

                // Previous Page (Slot 45)
                if (currentPage > 1) {
                    set(45, item(Material.ARROW, Component.text("Previous Page", Colours.YELLOW).norm(), listOf(Component.text("Go to page ${currentPage - 1}", Colours.DARK_GREY).norm()))) {
                        isCancelled = true
                        open(player, arenaId, currentPage - 1)
                    }
                }

                // Add Location Button (Slot 48) - Manual Input
                set(48, item(Material.EMERALD_BLOCK, Component.text("Add Location", Colours.GREEN).norm(), listOf(Component.text("Click to manually enter key & coordinates.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    promptKeyAndCoordinates(player, arenaId, currentPage)
                }

                // Back Button (Slot 49)
                set(49, item(Material.BARRIER, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to arena editor.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaEditGui.open(player, arenaId)
                }

                // Dedicated Remove Location Button (Slot 50)
                set(50, item(Material.REDSTONE_BLOCK, Component.text("Remove Location", Colours.RED).norm(), listOf(Component.text("Click to enter location key to delete.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AnvilInput.openTextInput(player, "Enter Key to Remove", "") { key ->
                        if (key.isNotBlank()) {
                            dbManager.removeArenaLocation(arenaId, key.trim()).thenAccept {
                                open(player, arenaId, currentPage)
                            }
                        } else {
                            open(player, arenaId, currentPage)
                        }
                    }
                }

                // Next Page (Slot 53)
                if (currentPage < totalPages) {
                    set(53, item(Material.ARROW, Component.text("Next Page", Colours.YELLOW).norm(), listOf(Component.text("Go to page ${currentPage + 1}", Colours.DARK_GREY).norm()))) {
                        isCancelled = true
                        open(player, arenaId, currentPage + 1)
                    }
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }

    private fun promptKeyAndCoordinates(player: Player, arenaId: String, page: Int) {
        AnvilInput.openTextInput(player, "Enter Location Key", "") { key ->
            val trimmedKey = key.trim()
            if (trimmedKey.isBlank()) {
                open(player, arenaId, page)
                return@openTextInput
            }

            // Prompt for X Y Z [Yaw Pitch] coordinates format: "10 64 -20" or "10 64 -20 90 0"
            AnvilInput.openTextInput(player, "X Y Z [Yaw] [Pitch]", "0 64 0") { coords ->
                val parts = coords.trim().split("\\s+".toRegex())
                val x = parts.getOrNull(0)?.toDoubleOrNull()
                val y = parts.getOrNull(1)?.toDoubleOrNull()
                val z = parts.getOrNull(2)?.toDoubleOrNull()
                val yaw = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
                val pitch = parts.getOrNull(4)?.toFloatOrNull() ?: 0f

                if (x == null || y == null || z == null) {
                    player.sendMessage(Component.text("Invalid coordinates provided! Format: X Y Z [Yaw Pitch]", Colours.RED).norm())
                    open(player, arenaId, page)
                    return@openTextInput
                }

                val worldName = Universum.arenaManager.worldName
                val world = Bukkit.getWorld(worldName)
                if (world == null) {
                    player.sendMessage(Component.text("World '$worldName' could not be found!", Colours.RED).norm())
                    open(player, arenaId, page)
                    return@openTextInput
                }

                val location = Location(world, x, y, z, yaw, pitch)
                dbManager.setArenaLocation(arenaId, trimmedKey, location).thenAccept {
                    open(player, arenaId, page)
                }
            }
        }
    }
}