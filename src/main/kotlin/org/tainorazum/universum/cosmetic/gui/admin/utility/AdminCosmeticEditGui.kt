package org.tainorazum.universum.cosmetic.gui.admin.utility

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.database.CosmeticEntry
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.gui.admin.AdminCosmeticsGui
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm

object AdminCosmeticEditGui {

    fun open(player: Player, cosmeticType: CosmeticType, cosmeticName: String) {
        DB_Cosmetics.getCosmeticEntry(cosmeticType, cosmeticName).thenAccept { entry ->
            if (entry == null) {
                player.sendMessage(Component.text("Cosmetic not found!", Colours.RED).norm())
                return@thenAccept
            }

            val title = "Edit: ${entry.name}"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 36) {

                // Overview Icon (Slot 4)
                val overviewLore = listOf(
                    Component.text("Type: ${entry.type.name}", Colours.GREY).norm(),
                    Component.text("Price: ${entry.price} Coins", Colours.GOLD).norm(),
                    Component.text("Material: ${entry.material.name}", Colours.GREY).norm(),
                    Component.text("Description: ${entry.description ?: "None"}", Colours.DARK_GREY).norm()
                )
                set(4, item(entry.material, Component.text(entry.name, Colours.GREEN).norm(), overviewLore)) { isCancelled = true }

                // Edit Price (Slot 10)
                set(10, item(Material.GOLD_INGOT, Component.text("Edit Price", Colours.YELLOW).norm(), listOf(Component.text("Current: ${entry.price} Coins", Colours.GREY).norm(), Component.text("Click to change price.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AnvilInput.openIntInput(player, "Enter New Price", entry.price) { newPrice ->
                        DB_Cosmetics.updatePrice(entry.type, entry.name, newPrice).thenAccept {
                            open(player, cosmeticType, cosmeticName)
                        }
                    }
                }

                // Edit Material (Slot 12)
                set(12, item(Material.ITEM_FRAME, Component.text("Edit Icon Material", Colours.YELLOW).norm(), listOf(Component.text("Current: ${entry.material.name}", Colours.GREY).norm(), Component.text("Click to select material.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminMaterialSelectGui.open(player) { newMat ->
                        DB_Cosmetics.updateMaterial(entry.type, entry.name, newMat).thenAccept {
                            open(player, cosmeticType, cosmeticName)
                        }
                    }
                }

                // Edit Description (Slot 14)
                set(14, item(Material.WRITABLE_BOOK, Component.text("Edit Description", Colours.YELLOW).norm(), listOf(Component.text("Current: ${entry.description ?: "None"}", Colours.GREY).norm(), Component.text("Click to change description.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AnvilInput.openTextInput(player, "Enter Description", entry.description ?: "") { newDesc ->
                        DB_Cosmetics.updateDescription(entry.type, entry.name, newDesc).thenAccept {
                            open(player, cosmeticType, cosmeticName)
                        }
                    }
                }

                // Edit Cosmetic Data (Slot 16)
                set(16, item(Material.REDSTONE, Component.text("Edit Cosmetic Data", Colours.PURPLE).norm(), listOf(Component.text("Click to modify underlying data.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminCosmeticDataEditGui.open(player, entry.type, entry.name)
                }

                // Delete Cosmetic (Slot 31)
                set(31, item(Material.LAVA_BUCKET, Component.text("Delete Cosmetic", Colours.RED).norm(), listOf(Component.text("Click to permanently delete.", Colours.RED).norm()))) {
                    isCancelled = true
                    openDeleteConfirm(player, entry)
                }

                // Back Button (Slot 27)
                set(27, item(Material.ARROW, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to selector.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminCosmeticsGui.openGUI(player, cosmeticType)
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }

    private fun openDeleteConfirm(player: Player, entry: CosmeticEntry) {
        val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent("Delete ${entry.name}?", Colours.RED), 27) {

            set(11, item(Material.LIME_STAINED_GLASS_PANE, Component.text("Confirm Delete", Colours.RED).norm(), listOf(Component.text("Action cannot be undone!", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                DB_Cosmetics.deleteCosmetic(entry.type, entry.name).thenAccept { success ->
                    if (success) {
                        player.sendMessage(Component.text("Deleted ${entry.name} successfully.", Colours.GREEN).norm())
                    }
                    AdminCosmeticsGui.openGUI(player, entry.type)
                }
            }

            set(15, item(Material.RED_STAINED_GLASS_PANE, Component.text("Cancel", Colours.GREEN).norm(), listOf(Component.text("Return to editor.", Colours.DARK_GREY).norm()))) {
                isCancelled = true
                open(player, entry.type, entry.name)
            }
        }

        Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
    }
}