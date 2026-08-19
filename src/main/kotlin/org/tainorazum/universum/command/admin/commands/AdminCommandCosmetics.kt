package org.tainorazum.universum.command.admin.commands

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.cosmetic.database.CosmeticEntry
import org.tainorazum.universum.cosmetic.database.DB_Cosmetics
import org.tainorazum.universum.cosmetic.gui.admin.AdminCosmeticsMenuGui
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.util.logging.Level

@Command("administrator", "admin", "a")
class AdminCommandCosmetics : AdminCommandGeneral(AdminCommandSub.COSMETICS) {

    override val tutorial: List<Pair<String, String>> = mutableListOf(
        Pair("/admin cosmetics", "Opens the cosmetic admin gui."),
        Pair("/admin cosmetics create <type> <name>", "Create a cosmetic."),
        Pair("/admin cosmetics create <type> <name> <material> <price> <description>", "Create a cosmetic.")
    )

    @Subcommand("cosmetics")
    fun cosmetics(
        actor: BukkitCommandActor
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_COSMETICS) {
        val player = actor.requirePlayer()
        AdminCosmeticsMenuGui.gui(player, null).open(player)
    }

    @Subcommand("cosmetics create")
    fun create(
        actor: BukkitCommandActor,
        type: CosmeticType,
        name: String,
        @Optional material: Material = Material.STONE,
        @Optional @Suggest("<price>", "100", "200", "500") price: Int = 0,
        @Optional @Suggest("<description>") description: String = "Default cosmetic description."
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_COSMETICS) {

        val player = actor.requirePlayer()
        val entry = CosmeticEntry(type, name, material, description, price)

        DB_Cosmetics.saveCosmetic(entry).thenAccept { success ->
            if (success) {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Created cosmetic: $name (${type.name})", Colours.GREEN))
                )
            } else {
                player.sendMessage(
                    Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Failed to create cosmetic with that name and type.", Colours.RED))
                )
            }
        }.exceptionally { throwable ->
            player.sendMessage(
                Prefix.ADMIN()
                    .append(Prefix.ARROW3())
                    .append(Component.text("An error occurred while saving the cosmetic. Check console for details.", Colours.RED))
            )
            throwable.printStackTrace()
            null
        }
    }
}