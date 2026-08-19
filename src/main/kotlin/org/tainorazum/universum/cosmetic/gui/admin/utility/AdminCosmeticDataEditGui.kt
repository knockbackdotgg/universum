package org.tainorazum.universum.cosmetic.gui.admin.utility

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.database.CosmeticEntry
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.util.CosmeticData
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.gui.GuiUtility.item
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm

object AdminCosmeticDataEditGui {

    fun open(player: Player, cosmeticType: CosmeticType, cosmeticName: String) {
        DB_Cosmetics.getCosmeticEntry(cosmeticType, cosmeticName).thenAccept { entry ->
            if (entry == null) {
                player.sendMessage(Component.text("Cosmetic not found!", Colours.RED).norm())
                return@thenAccept
            }

            val title = "Data: ${entry.name}"
            val gui = gg.flyte.twilight.gui.gui(Text.toSmallTextComponent(title, Colours.GREY), 27) {

                when (entry.type) {
                    CosmeticType.PET -> buildPetMenu(player, entry)
                    CosmeticType.CHAT_TAG -> buildChatTagMenu(player, entry)
                    CosmeticType.CHAT_COLOR -> buildChatColorMenu(player, entry)
                    CosmeticType.GLOW -> buildGlowMenu(player, entry)
                    CosmeticType.TRAIL -> buildTrailMenu(player, entry)
                    CosmeticType.AURA -> buildAuraMenu(player, entry)
                    CosmeticType.ARMOR_TRIMS -> buildArmorTrimMenu(player, entry)
                    CosmeticType.ARMOR_TRIM_COLOR -> buildArmorTrimColorMenu(player, entry)
                }

                // Back Button (Slot 18)
                set(18, item(Material.ARROW, Component.text("Back", Colours.RED).norm(), listOf(Component.text("Return to main editor.", Colours.DARK_GREY).norm()))) {
                    isCancelled = true
                    AdminCosmeticEditGui.open(player, entry.type, entry.name)
                }
            }

            Bukkit.getScheduler().runTask(Universum.instance, Runnable { gui.open(player) })
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildPetMenu(player: Player, entry: CosmeticEntry) {
        val petData = entry.data as? CosmeticData.Pet ?: CosmeticData.Pet(EntityType.ZOMBIE, Component.text(entry.name))

        // Slot 11: Entity Type
        set(11, item(Material.ZOMBIE_SPAWN_EGG, Component.text("Entity Type", Colours.YELLOW).norm(), listOf(Component.text("Current: ${petData.entityType.name}", Colours.GREY).norm(), Component.text("Click to select entity type.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AdminEntityTypeSelectGui.open(player) { newType ->
                updateAndReopen(player, entry, petData.copy(entityType = newType))
            }
        }

        // Slot 13: Display Name
        set(13, item(Material.NAME_TAG, Component.text("Display Name", Colours.YELLOW).norm(), listOf(Component.text("Click to edit display name.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Enter Display Name", entry.name) { newName ->
                updateAndReopen(player, entry, petData.copy(displayName = Component.text(newName)))
            }
        }

        // Slot 15: Baby Toggle
        val babyMaterial = if (petData.isBaby) Material.LIME_DYE else Material.GRAY_DYE
        val babyStatus = if (petData.isBaby) "Yes" else "No"
        set(15, item(babyMaterial, Component.text("Is Baby: $babyStatus", Colours.YELLOW).norm(), listOf(Component.text("Click to toggle baby state.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            updateAndReopen(player, entry, petData.copy(isBaby = !petData.isBaby))
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildChatTagMenu(player: Player, entry: CosmeticEntry) {
        val tagData = entry.data as? CosmeticData.ChatTag ?: CosmeticData.ChatTag(entry.name, Colours.WHITE)

        // Slot 11: Text
        set(11, item(Material.NAME_TAG, Component.text("Tag Text", Colours.YELLOW).norm(), listOf(Component.text("Current: ${tagData.text}", Colours.GREY).norm(), Component.text("Click to edit tag text.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Enter Tag Text", tagData.text) { newText ->
                updateAndReopen(player, entry, tagData.copy(text = newText))
            }
        }

        // Slot 15: Color
        set(15, item(Material.RED_DYE, Component.text("Tag Color", Colours.YELLOW).norm(), listOf(Component.text("Current: ${tagData.color.asHexString()}", Colours.GREY).norm(), Component.text("Click to enter hex color.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Hex Color (#RRGGBB)", tagData.color.asHexString()) { hex ->
                val newColor = parseHexColor(hex) ?: tagData.color
                updateAndReopen(player, entry, tagData.copy(color = newColor))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildChatColorMenu(player: Player, entry: CosmeticEntry) {
        val colorData = entry.data as? CosmeticData.ChatColor ?: CosmeticData.ChatColor(Colours.WHITE)

        // Slot 13: Color
        set(13, item(Material.FEATHER, Component.text("Chat Color", Colours.YELLOW).norm(), listOf(Component.text("Current: ${colorData.color.asHexString()}", Colours.GREY).norm(), Component.text("Click to enter hex color.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Hex Color (#RRGGBB)", colorData.color.asHexString()) { hex ->
                val newColor = parseHexColor(hex) ?: colorData.color
                updateAndReopen(player, entry, colorData.copy(color = newColor))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildGlowMenu(player: Player, entry: CosmeticEntry) {
        val glowData = entry.data as? CosmeticData.Glow ?: CosmeticData.Glow(Colours.WHITE)

        // Slot 13: Color
        set(13, item(Material.GLOWSTONE_DUST, Component.text("Glow Color", Colours.YELLOW).norm(), listOf(Component.text("Current: ${glowData.color.asHexString()}", Colours.GREY).norm(), Component.text("Click to enter hex color.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Hex Color (#RRGGBB)", glowData.color.asHexString()) { hex ->
                val newColor = parseHexColor(hex) ?: glowData.color
                updateAndReopen(player, entry, glowData.copy(color = newColor))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildTrailMenu(player: Player, entry: CosmeticEntry) {
        val trailData = entry.data as? CosmeticData.Trail ?: CosmeticData.Trail(entry.name, Colours.WHITE)

        // Slot 11: Name
        set(11, item(Material.PAPER, Component.text("Trail Name", Colours.YELLOW).norm(), listOf(Component.text("Current: ${trailData.name}", Colours.GREY).norm(), Component.text("Click to edit trail name.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Enter Trail Name", trailData.name) { newName ->
                updateAndReopen(player, entry, trailData.copy(name = newName))
            }
        }

        // Slot 15: Color
        set(15, item(Material.GLOWSTONE_DUST, Component.text("Trail Color", Colours.YELLOW).norm(), listOf(Component.text("Current: ${trailData.color.asHexString()}", Colours.GREY).norm(), Component.text("Click to enter hex color.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Hex Color (#RRGGBB)", trailData.color.asHexString()) { hex ->
                val newColor = parseHexColor(hex) ?: trailData.color
                updateAndReopen(player, entry, trailData.copy(color = newColor))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildAuraMenu(player: Player, entry: CosmeticEntry) {
        val auraData = entry.data as? CosmeticData.Aura ?: CosmeticData.Aura(entry.name, Colours.WHITE)

        // Slot 11: Name
        set(11, item(Material.PAPER, Component.text("Aura Name", Colours.YELLOW).norm(), listOf(Component.text("Current: ${auraData.name}", Colours.GREY).norm(), Component.text("Click to edit aura name.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Enter Aura Name", auraData.name) { newName ->
                updateAndReopen(player, entry, auraData.copy(name = newName))
            }
        }

        // Slot 15: Color
        set(15, item(Material.BLAZE_POWDER, Component.text("Aura Color", Colours.YELLOW).norm(), listOf(Component.text("Current: ${auraData.color.asHexString()}", Colours.GREY).norm(), Component.text("Click to enter hex color.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AnvilInput.openTextInput(player, "Hex Color (#RRGGBB)", auraData.color.asHexString()) { hex ->
                val newColor = parseHexColor(hex) ?: auraData.color
                updateAndReopen(player, entry, auraData.copy(color = newColor))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildArmorTrimMenu(player: Player, entry: CosmeticEntry) {
        val trimData = entry.data as? CosmeticData.ArmorTrim ?: CosmeticData.ArmorTrim(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)

        // Slot 13: Material
        set(13, item(trimData.material, Component.text("Trim Material", Colours.YELLOW).norm(), listOf(Component.text("Current: ${trimData.material.name}", Colours.GREY).norm(), Component.text("Click to select material.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AdminMaterialSelectGui.open(player) { newMat ->
                updateAndReopen(player, entry, trimData.copy(material = newMat))
            }
        }
    }

    private fun gg.flyte.twilight.gui.GUI.buildArmorTrimColorMenu(player: Player, entry: CosmeticEntry) {
        val trimColorData = entry.data as? CosmeticData.ArmorTrimColor ?: CosmeticData.ArmorTrimColor(Material.REDSTONE)

        // Slot 13: Material
        set(13, item(trimColorData.material, Component.text("Trim Color Material", Colours.YELLOW).norm(), listOf(Component.text("Current: ${trimColorData.material.name}", Colours.GREY).norm(), Component.text("Click to select material.", Colours.DARK_GREY).norm()))) {
            isCancelled = true
            AdminMaterialSelectGui.open(player) { newMat ->
                updateAndReopen(player, entry, trimColorData.copy(material = newMat))
            }
        }
    }

    private fun updateAndReopen(player: Player, entry: CosmeticEntry, newData: CosmeticData) {
        DB_Cosmetics.updateData(entry.type, entry.name, newData).thenAccept {
            open(player, entry.type, entry.name)
        }
    }

    private fun parseHexColor(input: String): TextColor? {
        val cleaned = input.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null
        val formatted = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
        return TextColor.fromHexString(formatted)
    }
}