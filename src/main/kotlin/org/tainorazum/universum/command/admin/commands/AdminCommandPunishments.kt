package org.tainorazum.universum.command.admin.commands

import gg.flyte.twilight.gui.GUI
import gg.flyte.twilight.gui.GUI.Companion.openInventory
import gg.flyte.twilight.gui.gui
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.tainorazum.universum.Universum
import org.tainorazum.universum.command.admin.AdminCommandSub
import org.tainorazum.universum.punishment.PunishmentGUI
import org.tainorazum.universum.punishment.PunishmentType
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.other.parseDuration
import org.tainorazum.universum.utility.other.toHumanReadable
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Prefix
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Suggest
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.time.Duration
import java.util.concurrent.CompletableFuture

@Command("administrator", "admin", "a")
class AdminCommandPunishments : AdminCommandGeneral(AdminCommandSub.PUNISHMENTS) {

    override val tutorial : List<Pair<String, String>> = mutableListOf(
        Pair("/admin punish", "Opens the punishment tutorial gui."),
        Pair("/admin punish <name>", "Opens the punishments gui regarding a specific player."),
        Pair("/admin punish <name> <type> <reason> <duration>", "Punish a player."),
        Pair("/admin punish history <name>", "See a player's punishment history.")
    )

    @Subcommand("punish")
    fun punish(
        actor: BukkitCommandActor,
        @Optional name: String? = null,
        @Optional type: PunishmentType? = null,
        @Optional reason: String? = null,
        @Optional duration: Duration? = null,
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_PUNISHMENTS) {

        val player = actor.requirePlayer()

        if(name == null && type == null && reason == null && duration == null) {
            // open tutorial gui
        }
        else if(name != null && type == null && reason == null && duration == null) {
            // open punish gui
        }
        else {
            // manual cmd
        }

    }

    @Subcommand("punish history")
    fun punishmentHistory(
        actor: BukkitCommandActor,
        name: String? = null
    ) = PlayerUtility.guard(actor, Permission.CMD_ADMIN_PUNISHMENTS) {

        val player = actor.requirePlayer()

        if(name != null) {
            PlayerUtility.getUUID(name).thenAccept { uuid ->
                if(uuid != null) {
                    PunishmentGUI.historyWithTargetInitial(player, name, uuid).open(player)
                } else {
                    player.sendMessage(Prefix.ADMIN()
                        .append(Prefix.ARROW3())
                        .append(Component.text("Unable to find that player.", Colours.RED)))
                }
            }
        }
    }

}