package org.tainorazum.universum.punishment

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.tainorazum.universum.utility.other.formatDate
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.other.toHumanReadable
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import java.util.UUID

object PunishmentGUI {

    val orders = listOf("Type", "Date", "Duration")
    private val currentOrderIndex = mutableMapOf<UUID, Int>()

    // Track active page per player
    private val currentPageIndex = mutableMapOf<UUID, Int>()

    fun createOrderItem(current: String): ItemStack {
        val loreList = mutableListOf(Component.empty())

        orders.forEach { order ->
            if (order == current) {
                loreList.add(Component.text("▶ $order", Colours.GREEN))
            } else {
                loreList.add(Component.text("  $order", Colours.GREY))
            }
        }

        loreList.add(Component.empty())
        loreList.add(Component.text("Click to cycle order", Colours.DARK_GREY))

        return item(
            Material.ARROW,
            Text.toSmallTextComponent("Order By", Colours.RED),
            loreList
        )
    }

    /**
     * Builds the Page Cycler Item displaying current page status.
     */
    fun createPageItem(currentPage: Int, totalPages: Int): ItemStack {
        val loreList = mutableListOf(
            Component.empty(),
            Component.text("Current page » ", Colours.GREY)
                .append(Component.text("${currentPage + 1}/$totalPages", Colours.WHITE)),
            Component.empty(),
            Component.text("Click to cycle page", Colours.DARK_GREY)
        )

        return item(
            Material.PAPER,
            Text.toSmallTextComponent("page", Colours.RED),
            loreList
        )
    }

    fun item(material: Material, name: Component, lore: String): ItemStack {
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

    fun item(material: Material, name: Component, lore: List<Component>): ItemStack {
        return ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name)
                meta.lore(lore)
            }
        }
    }

    fun historyWithTargetInitial(player: Player, target: String, targetUUID: UUID): GUI {
        lateinit var punishmentGui: GUI

        punishmentGui = gui(Text.toSmallTextComponent("$target's punishments", Colours.GREY), 27) {
            pattern(
                "#########",
                "#BMKWN#A#",
                "#########"
            )

            set('B', item(Material.RED_DYE, Component.text("Bans", Colours.RED), "See all bans.")) {
                isCancelled = true
                // Reset page on fresh view open
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, PunishmentType.BAN).open(player)
            }

            set('M', item(Material.ORANGE_DYE, Component.text("Mutes", Colours.ORANGE), "See all mutes.")) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, PunishmentType.MUTE).open(player)
            }

            set('K', item(Material.YELLOW_DYE, Component.text("Kicks", Colours.YELLOW), "See all kicks.")) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, PunishmentType.KICK).open(player)
            }

            set('W', item(Material.PURPLE_DYE, Component.text("Warns", Colours.PURPLE), "See all warns.")) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, PunishmentType.WARN).open(player)
            }

            set('N', item(Material.MAGENTA_DYE, Component.text("Notes", Colours.MAGENTA), "See all notes.")) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, PunishmentType.NOTE).open(player)
            }

            set('A', item(Material.WHITE_DYE, Component.text("All punishments", Colours.WHITE), "See all punishments.")) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = 0
                historyWithTargetPunishments(punishmentGui, player, target, targetUUID, null).open(player)
            }
        }

        return punishmentGui
    }

    fun historyWithTargetPunishments(
        previous: GUI,
        player: Player,
        target: String,
        targetUuid: UUID,
        type: PunishmentType? = null
    ): GUI {
        val titleText = "$target's ${type?.name?.lowercase()?.plus("s") ?: "punishments"}"
        val activeOrderIndex = currentOrderIndex.getOrDefault(player.uniqueId, 0)
        val activeOrder = orders[activeOrderIndex]

        val punishmentsList = fetchAndSortPunishments(targetUuid, type, activeOrder)

        val pattern = listOf(
            "IIIIIIIII",
            "IIIIIIIII",
            "IIIIIIIII",
            "IIIIIIIII",
            "GGGGGGGGG",
            "B###O###P"
        )

        val itemSlots = pattern.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == 'I') (rowIndex * 9) + colIndex else null
            }
        }

        // Pagination Calculations
        val pageSize = itemSlots.size
        val totalPages = maxOf(1, kotlin.math.ceil(punishmentsList.size.toDouble() / pageSize).toInt())

        var page = currentPageIndex.getOrDefault(player.uniqueId, 0)
        if (page >= totalPages) page = 0 // Bounds safety reset

        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, punishmentsList.size)
        val pagedPunishments = if (punishmentsList.isNotEmpty()) punishmentsList.subList(startIndex, endIndex) else emptyList()

        val punishmentGui = gui(Text.toSmallTextComponent(titleText, Colours.GREY), 54) {
            pattern(*pattern.toTypedArray())

            set('B', item(Material.ARROW, Text.toSmallTextComponent("Go Back", Colours.RED), "Click to return to the previous menu.")) {
                isCancelled = true
                previous.open(player)
            }

            set('O', createOrderItem(activeOrder)) {
                isCancelled = true
                currentOrderIndex[player.uniqueId] = (activeOrderIndex + 1) % orders.size
                historyWithTargetPunishments(previous, player, target, targetUuid, type).open(player)
            }

            // Page Cycler Button ('P')
            set('P', createPageItem(page, totalPages)) {
                isCancelled = true
                currentPageIndex[player.uniqueId] = (page + 1) % totalPages
                historyWithTargetPunishments(previous, player, target, targetUuid, type).open(player)
            }

            // Render current page items inside pattern slots 'I'
            pagedPunishments.forEachIndexed { index, punishment ->
                set(itemSlots[index], createPunishmentItem(punishment)) {
                    isCancelled = true
                }
            }
        }

        return punishmentGui
    }

    private fun createPunishmentItem(punishment: Punishment): ItemStack {
        var material = when (punishment.type) {
            PunishmentType.BAN -> Material.RED_CONCRETE
            PunishmentType.MUTE -> Material.JUKEBOX
            PunishmentType.KICK -> Material.LEATHER_BOOTS
            PunishmentType.WARN -> Material.PAPER
            PunishmentType.NOTE -> Material.BOOK
        }

        if (punishment.archived) {
            material = Material.LIGHT_GRAY_DYE
        }

        val title = Component.text("#${punishment.id} - ${punishment.type.name}", Colours.RED)
        val lore = listOf(
            Component.empty(),
            Component.text("Reason » ", Colours.GREY).append(Component.text(punishment.reason, Colours.WHITE)),
            Component.text("Issued on » ", Colours.GREY).append(Component.text(formatDate(punishment.punishedOn), Colours.WHITE)),
            Component.text("Duration » ", Colours.GREY).append(Component.text(punishment.duration?.toHumanReadable() ?: "Permanent", Colours.WHITE)),
            Component.text("Archived » ", Colours.GREY).append(Component.text(if (punishment.archived) "Yes" else "No", Colours.WHITE)),
            Component.empty()
        )

        return item(material, title, lore)
    }

    private fun fetchAndSortPunishments(
        targetUuid: UUID,
        type: PunishmentType?,
        order: String
    ): List<Punishment> {
        val future = if (type == null) {
            DB_Punishments.getPunishments(targetUuid)
        } else {
            DB_Punishments.getPunishments(targetUuid, type)
        }

        val rawList = try {
            future.get() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return when (order) {
            "Type" -> rawList.sortedBy { it.type.name }
            "Date" -> rawList.sortedByDescending { it.punishedOn.time }
            "Duration" -> rawList.sortedByDescending { it.duration?.toMillis() ?: Long.MAX_VALUE }
            else -> rawList
        }
    }
}