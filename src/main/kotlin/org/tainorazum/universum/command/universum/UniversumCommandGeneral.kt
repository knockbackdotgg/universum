package org.tainorazum.universum.command.universum

import com.google.gson.Gson
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.tainorazum.universum.security.Permission
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor

enum class UniversumCommandSub {

    GENERAL,
    TEST,

    ;

    private var cachedCommand: UniversumCommandGeneral? = null

    fun getCommand(): UniversumCommandGeneral? {
        return cachedCommand
    }

    fun register(command: UniversumCommandGeneral) {
        cachedCommand = command
    }
}

@Command("universum", "uni", "u")
open class UniversumCommandGeneral(
    sub: UniversumCommandSub = UniversumCommandSub.GENERAL
) {

    open val tutorial : List<String> = mutableListOf(
        "/universum help <subject> - Receive help on a specific command.",
    )

    init {
        sub.register(this)
    }

    fun allowed(actor: BukkitCommandActor) : Boolean {
        return if (actor is Player) {
            Permission.CMD_UNIVERSUM_ALL.check(actor.uniqueId)
        } else true
    }

    @Subcommand("help")
    open fun help(
        actor: BukkitCommandActor,
        @Optional subArg: UniversumCommandSub?
    ) {
        if (!allowed(actor)) return

        val tutorialList = subArg?.getCommand()?.tutorial ?: tutorial
        val subjectName = subArg?.name ?: "GENERAL"

        var component = Component.empty()
            .append(Component.text("Universum command help ($subjectName)", Colours.GREEN))
            .append(Component.newline())

        for (line in tutorialList) {
            component = component
                .append(Component.text(line, Colours.GREY))
                .append(Component.newline())
        }

        actor.reply(component)
    }

}
