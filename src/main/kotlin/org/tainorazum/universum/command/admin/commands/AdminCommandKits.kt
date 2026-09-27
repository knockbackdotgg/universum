package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.engine.kit.database.DB_Kits
import org.tainorazum.universum.kiteditor.editor.items.KitEditorItemsType
import org.tainorazum.universum.kiteditor.editor.items.database.DB_KE_Items
import org.tainorazum.universum.kiteditor.gui.admin.AdminKitEditorGui
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Command("administrator", "admin", "a")
class AdminCommandKits : AdminCommandGeneral(AdminCommandSub.PUNISHMENTS) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin kits", "Opens the kit management gui."),
        Pair("/admin kits create <identifier>", "Create a kit with its identifier."),
        Pair("/admin kits items add <category> <material>", "Add an item to a kit editor category."),
        Pair("/admin kits items remove <category> <material>", "Add an item to a kit editor category."),
    )

    @Subcommand("kits")
    fun kits(
        actor: BukkitCommandActor,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        AdminKitEditorGui.openGUI(player)

    }

    @Subcommand("kits items remove")
    fun itemsRemove(
        actor: BukkitCommandActor,
        category: KitEditorItemsType,
        material: Material,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        DB_KE_Items.removeMaterial(category, material).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Removed material ${material.name} to category ${category.name}.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Failed to remove material ${material.name} to category ${category.name}. (Item has already been added or error.)", Colours.RED)))
            }
        }
    }

    @Subcommand("kits items add")
    fun itemsAdd(
        actor: BukkitCommandActor,
        category: KitEditorItemsType,
        material: Material,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        DB_KE_Items.addMaterial(category, material).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Added material ${material.name} to category ${category.name}.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Failed to add material ${material.name} to category ${category.name}.", Colours.RED)))
            }
        }
    }

    @Subcommand("kits items add")
    fun itemsAdd(
        actor: BukkitCommandActor,
        category: KitEditorItemsType,
        vararg materials: Material,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()

        DB_KE_Items.addMaterials(category, materials.toList()).thenAccept { success ->
            if (success) {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(
                            Component.text(
                                "Added ${materials.size} material(s) to category ${category.name}.",
                                Colours.GREEN
                            )
                        )
                )
            } else {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(
                            Component.text(
                                "Failed to add materials to category ${category.name}.",
                                Colours.RED
                            )
                        )
                )
            }
        }
    }

    @Subcommand("kits create")
    fun create(
        actor: BukkitCommandActor,
        identifier: String
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_KITS) {

        val player = actor.requirePlayer()
        DB_Kits.createKit(identifier).thenAccept { success ->
            if(success) {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Kit '$identifier' has been successfully made. You may now modify it.", Colours.GREEN)))
            } else {
                player.sendMessage(Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("Failed to make kit. Either it already exists or there is an error. Look in console for details.", Colours.RED)))
            }
        }

    }

}