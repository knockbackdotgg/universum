package org.tainorazum.universum.engine.arena.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.admin.AdminArenasGui
import org.tainorazum.universum.engine.arena.admin.utility.AdminArenaGameTypesGui
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.arena.database.ArenaTemplate
import org.tainorazum.universum.engine.arena.database.DB_Arenas
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm

object AdminArenaEditGui {

    private val dbManager = ArenaDatabaseManager()

    fun open(player: Player, arenaId: String) {
        dbManager.getArenaTemplate(arenaId).thenAccept { template ->
            if (template == null) {
                player.sendMessage(Component.text("Arena not found!", Colours.RED).norm())
                return@thenAccept
            }

            val title = "Edit Arena: ${template.id}"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 36) {

                // Overview Icon (Slot 4)
                val overviewLore = listOf(
                    Component.text("ID: ${template.id}", Colours.GREY).norm(),
                    Component.text("Radius: ${template.radius}", Colours.GOLD).norm(),
                    Component.text("Center: ${template.center.world.name} (${template.center.blockX}, ${template.center.blockY}, ${template.center.blockZ})", Colours.GREY).norm(),
                    Component.text("Schematic Size: ${template.schematic.size} bytes", Colours.DARK_GREY).norm()
                )
                set(4, item(Material.GRASS_BLOCK, template.name, overviewLore)) { isCancelled = true }

                // Edit Radius (Slot 10)
                set(10, item(Material.COMPASS, Component.text("Edit Radius", Colours.YELLOW).norm(), listOf(Component.text("Current: ${template.radius}", Colours.GREY).norm(), Component.text("Click to change radius.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AnvilInput.openIntInput(player, "Enter Radius", template.radius.toInt()) { newRadius ->
                        updateRadius(arenaId, newRadius.toDouble()).thenAccept {
                            open(player, arenaId)
                        }
                    }
                }

                // Edit Game Types (Slot 12)
                set(12, item(Material.DIAMOND_SWORD, Component.text("Manage Game Types", Colours.YELLOW).norm(), listOf(Component.text("Click to configure allowed game types.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaGameTypesGui.open(player, arenaId)
                }

                // Edit Kits (Slot 14)
                set(14, item(Material.CHEST, Component.text("Manage Allowed Kits", Colours.YELLOW).norm(), listOf(Component.text("Click to configure allowed kits.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaKitsGui.open(player, arenaId)
                }

                // Manage Locations (Slot 16)
                set(16, item(Material.REDSTONE_TORCH, Component.text("Manage Locations", Colours.PURPLE).norm(), listOf(Component.text("Click to view and edit saved locations.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenaLocationsGui.open(player, arenaId)
                }

                // Save FAWE Clipboard as Schematic (Slot 22)
                set(22, item(Material.PAPER, Component.text("Set Schematic (Clipboard)", Colours.AQUA).norm(), listOf(Component.text("Click to save active FAWE clipboard.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    dbManager.saveClipboardToArena(player, arenaId).thenAccept { success ->
                        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                            if (success) {
                                player.sendMessage(Component.text("${Prefix.ADMIN().append(Prefix.ARROW3())} Saved FAWE clipboard to arena '$arenaId'!", Colours.GREEN).norm())
                                open(player, arenaId)
                            } else {
                                player.sendMessage(Component.text("${Prefix.ADMIN().append(Prefix.ARROW3())} Failed to save schematic. Make sure you copied an area with WorldEdit!", Colours.RED).norm())
                            }
                        })
                    }
                }

                // Delete Arena (Slot 31)
                set(31, item(Material.LAVA_BUCKET, Component.text("Delete Arena", Colours.RED).norm(), listOf(Component.text("Click to permanently delete.", Colours.RED).norm()))) {
                    isCancelled = true
                    openDeleteConfirm(player, template)
                }

                // Back Button (Slot 27)
                set(27, item(Material.ARROW, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to arena selector.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminArenasGui.openGUI(player)
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }

    private fun openDeleteConfirm(player: Player, template: ArenaTemplate) {
        val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent("Delete ${template.id}?", Colours.RED), 27) {

            set(11, item(Material.LIME_STAINED_GLASS_PANE, Component.text("Confirm Delete", Colours.RED).norm(), listOf(Component.text("Action cannot be undone!", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                deleteArena(template.id).thenAccept { success ->
                    if (success) {
                        player.sendMessage(Component.text("Deleted arena ${template.id} successfully.", Colours.GREEN).norm())
                    }
                    AdminArenasGui.openGUI(player)
                }
            }

            set(15, item(Material.RED_STAINED_GLASS_PANE, Component.text("Cancel", Colours.GREEN).norm(), listOf(Component.text("Return to editor.", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                open(player, template.id)
            }
        }

        Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
    }

    private fun updateRadius(arenaId: String, newRadius: Double) = dba {
        try {
            transaction {
                DB_Arenas.update({ DB_Arenas.identifier eq arenaId }) {
                    it[radius] = newRadius
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteArena(arenaId: String) = dba {
        try {
            transaction {
                DB_Arenas.deleteWhere { DB_Arenas.identifier eq arenaId }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}