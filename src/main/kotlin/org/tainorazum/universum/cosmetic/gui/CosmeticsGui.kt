package org.tainorazum.universum.cosmetic.gui

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.database.CosmeticEntry
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.database.DB_PlayerCosmetics
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

object CosmeticsGui {

    private val PATTERN = listOf(
        "WWWWWWWWW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WCCCCCCCW",
        "WWWWWWWWW",
        "B#######P"
    )

    // Pre-calculate inventory slot indices corresponding to 'C'
    private val COSMETIC_SLOTS: List<Int> by lazy {
        PATTERN.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'C') (rowIndex * 9) + colIndex else null
            }
        }
    }

    private val PAGE_SIZE: Int get() = COSMETIC_SLOTS.size

    fun gui(player: Player, cosmeticType: CosmeticType, page: Int = 1, previous: GUI? = null): CompletableFuture<GUI> {
        val playerUuid = player.uniqueId

        return DB_Cosmetics.getCosmeticsByType(cosmeticType).thenCompose { allCosmetics ->
            val totalPages = maxOf(1, ceil(allCosmetics.size.toDouble() / PAGE_SIZE).toInt())
            val currentPage = page.coerceIn(1, totalPages)

            val ownedFuture = DB_PlayerCosmetics.getOwnedCosmetics(playerUuid, cosmeticType)
            val enabledFuture = DB_PlayerCosmetics.getEnabled(playerUuid, cosmeticType)

            ownedFuture.thenCombine(enabledFuture) { ownedSet, enabledName ->
                val titleText = "cosmetics »  ${cosmeticType.name.lowercase().replace('_', ' ')}"

                gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
                    pattern(*PATTERN.toTypedArray())

                    set('W', item(Material.WHITE_STAINED_GLASS_PANE, Component.empty(), "")) {
                        isCancelled = true
                    }

                    // Back / Close Navigation ('B')
                    val backLore = listOf(
                        Component.text(if (previous != null) "Click to return to previous menu." else "Click to close this menu.", Colours.DARK_GREY).norm()
                    )
                    set('B', item(Material.ARROW, Text.toSmallTextComponent(if (previous != null) "Go Back" else "Close", Colours.RED), backLore)) {
                        isCancelled = true
                        if (previous != null) previous.open(player) else player.closeInventory()
                    }

                    // Page Navigation ('P')
                    val prevPageText = if (currentPage > 1) "${currentPage - 1}" else "None"
                    val nextPageText = if (currentPage < totalPages) "${currentPage + 1}" else "None"
                    val pageLore = listOf(
                        Component.empty(),
                        Component.text("Current Page » $currentPage / $totalPages", Colours.YELLOW).norm(),
                        Component.text("Previous Page » $prevPageText", Colours.GREY).norm(),
                        Component.text("Next Page » $nextPageText", Colours.GREY).norm(),
                        Component.empty(),
                        Component.text("Left-Click for Next Page", Colours.GREEN).norm(),
                        Component.text("Right-Click for Previous Page", Colours.RED).norm()
                    )

                    set('P', item(Material.PAPER, Text.toSmallTextComponent("Page Navigation", Colours.YELLOW), pageLore)) {
                        isCancelled = true
                        if (isLeftClick && currentPage < totalPages) {
                            openGUI(player, cosmeticType, currentPage + 1, previous)
                        } else if (isRightClick && currentPage > 1) {
                            openGUI(player, cosmeticType, currentPage - 1, previous)
                        }
                    }

                    // Render Cosmetic Items
                    val startIndex = (currentPage - 1) * PAGE_SIZE
                    val cosmeticsForPage = allCosmetics.drop(startIndex).take(PAGE_SIZE)

                    cosmeticsForPage.forEachIndexed { index, cosmeticEntry ->
                        val slotIndex = COSMETIC_SLOTS[index]
                        val isOwned = ownedSet.contains(cosmeticEntry.name)
                        val isEnabled = enabledName == cosmeticEntry.name

                        set(slotIndex, createCosmeticItem(playerUuid, cosmeticEntry, isOwned, isEnabled)) {
                            isCancelled = true

                            if (isOwned) {
                                val action = if (isEnabled) {
                                    Universum.cosmeticManager.disableCosmetic(player, cosmeticType)
                                } else {
                                    Universum.cosmeticManager.enableCosmetic(player, cosmeticType, cosmeticEntry.name)
                                }
                                action.thenAccept { openGUI(player, cosmeticType, currentPage, previous) }
                            } else {
                                if (!Universum.coinsManager.hasCoins(player.uniqueId, cosmeticEntry.price)) {
                                    player.sendMessage(Component.text("You do not have enough coins to buy this!", Colours.RED).norm())
                                    player.noSound()
                                    return@set
                                }

                                openConfirmGui(player, cosmeticEntry, cosmeticType, currentPage, previous)
                            }
                        }
                    }
                }
            }
        }
    }

    fun openGUI(player: Player, cosmeticType: CosmeticType, page: Int = 1, previous: GUI? = null) {
        gui(player, cosmeticType, page, previous).thenAccept { gui ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                gui.open(player)
            })
        }
    }

    private fun createCosmeticItem(uuid: UUID, entry: CosmeticEntry, isOwned: Boolean, isEnabled: Boolean): ItemStack {
        val loreList = mutableListOf<Component>()

        // 1. Description
        if (!entry.description.isNullOrBlank()) {
            loreList.add(Component.text(entry.description, Colours.DARK_GREY).norm())
            loreList.add(Component.empty())
        }

        // 2. Cosmetic Preview (Runs for both Owned and Unowned)
        var hasPreview = false
        when (val data = entry.data) {
            is CosmeticData.ChatTag -> {
                val previewComponent = Component.text("Preview: ", Colours.GREY)
                    .append(Component.text("[", Colours.DARK_GREY))
                    .append(Component.text(data.text, data.color))
                    .append(Component.text("]", Colours.DARK_GREY))
                    .norm()

                loreList.add(previewComponent)
                hasPreview = true
            }
            is CosmeticData.ChatColor -> {
                val previewComponent = Component.text("Preview: ", Colours.GREY)
                    .append(Component.text("Hello!", data.color))
                    .norm()

                loreList.add(previewComponent)
                hasPreview = true
            }
            is CosmeticData.Pet -> {
                val baseName = data.entityType.name
                    .lowercase()
                    .split('_')
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                val formattedName = if (data.isBaby && !baseName.startsWith("Baby", ignoreCase = true)) {
                    "Baby $baseName"
                } else {
                    baseName
                }

                val previewComponent = Component.text("Type: ", Colours.GREY)
                    .append(Component.text(formattedName, Colours.GREEN))
                    .norm()

                loreList.add(previewComponent)
                hasPreview = true
            }
            else -> {}
        }

        // Add spacing after preview if a preview was rendered
        if (hasPreview) {
            loreList.add(Component.empty())
        }

        val material: Material
        val titleColor: TextColor

        // 3. Ownership / Pricing Status
        if (isOwned) {
            material = entry.material
            if (isEnabled) {
                titleColor = Colours.GREEN
                loreList.add(Component.text("Status: ", Colours.GREY).append(Component.text("Equipped", Colours.GREEN)).norm())
                loreList.add(Component.empty())
                loreList.add(Component.text("Click to disable this cosmetic.", Colours.DARK_GREY).norm())
            } else {
                titleColor = Colours.YELLOW
                loreList.add(Component.text("Status: ", Colours.GREY).append(Component.text("Unequipped", Colours.YELLOW)).norm())
                loreList.add(Component.empty())
                loreList.add(Component.text("Click to enable this cosmetic.", Colours.DARK_GREY).norm())
            }
        } else {
            material = Material.BARRIER
            titleColor = Colours.RED
            loreList.add(Component.text("Price: ", Colours.GREY).append(Component.text("${entry.price} Coins", Colours.GOLD)).norm())
            loreList.add(Component.text("You have: ", Colours.GREY).append(Component.text("${Universum.coinsManager.getCoins(uuid)} Coins", Colours.GOLD)).norm())
            loreList.add(Component.empty())
            loreList.add(Component.text("Click to purchase!", Colours.GREEN).norm())
        }

        val displayName = entry.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

        return item(
            material,
            Component.text(displayName, titleColor).norm(),
            loreList,
            enchanted = isEnabled
        )
    }

    private fun openConfirmGui(
        player: Player,
        entry: CosmeticEntry,
        cosmeticType: CosmeticType,
        currentPage: Int,
        previousGui: GUI?
    ) {
        val title = "Confirm Purchase: ${entry.name}"

        val confirmGui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 27) {

            val confirmLore = listOf(
                Component.empty(),
                Component.text("Item: ${entry.name.lowercase().replaceFirstChar { it.uppercase() }}", Colours.GREY).norm(),
                Component.text("Price: ", Colours.GREY).append(Component.text("${entry.price} Coins", Colours.GOLD)).norm(),
                Component.empty(),
                Component.text("Click to confirm purchase.", Colours.GREEN).norm()
            )

            // Confirm Button (Slot 11)
            set(11, item(Material.LIME_STAINED_GLASS_PANE, Component.text("Confirm Purchase", Colours.GREEN).norm(), confirmLore)) {
                isCancelled = true

                if (!Universum.coinsManager.hasCoins(player.uniqueId, entry.price)) {
                    player.sendMessage(Component.text("You do not have enough coins to buy this!", Colours.RED).norm())
                    player.closeInventory()
                    return@set
                }

                if (Universum.coinsManager.removeCoins(player.uniqueId, entry.price)) {
                    DB_PlayerCosmetics.grantCosmetic(player.uniqueId, entry.type, entry.name).thenAccept { success ->
                        if (success) {
                            player.sendMessage(Component.text("Successfully purchased ${entry.name}!", Colours.GREEN).norm())
                        } else {
                            player.sendMessage(Component.text("Failed to process transaction.", Colours.RED).norm())
                        }
                        openGUI(player, cosmeticType, currentPage, previousGui)
                    }
                }
            }

            // Cancel Button (Slot 15)
            set(15, item(Material.RED_STAINED_GLASS_PANE, Component.text("Cancel", Colours.RED).norm(), listOf(Component.text("Click to return to menu.", Colours.GREY).norm()))) {
                isCancelled = true
                openGUI(player, cosmeticType, currentPage, previousGui)
            }
        }

        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
            confirmGui.open(player)
        })
    }
}