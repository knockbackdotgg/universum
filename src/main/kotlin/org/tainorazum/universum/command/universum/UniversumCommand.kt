package org.tainorazum.universum.command.universum

import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import com.google.gson.Gson
import net.kyori.adventure.text.Component
import org.tainorazum.universum.utility.text.Colours
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.actor.BukkitCommandActor

enum class UniversumCommandSub {

    GENERAL,

    ;

    private var cachedCommand: UniversumCommand? = null

    fun getCommand(): UniversumCommand? {
        if (cachedCommand != null) return cachedCommand
        
        cachedCommand = when (this) {
            else -> null
        }
        return cachedCommand
    }
}

@Command("universum", "uni", "u")
abstract class UniversumCommand(val sub: UniversumCommandSub?) {

    open val gson = Gson()
    open val tutorial : List<String> = mutableListOf<String>(
        "/universum help <subject> - Receive help on a specific command.",
    )

    @Subcommand("help")
    fun help(actor: BukkitCommandActor, @Optional subArg: UniversumCommandSub = UniversumCommandSub.GENERAL) {
        var component = Component.empty()
            .append(Component.text("Universum command help (${subArg.name})", Colours.GREEN))
            .append(Component.newline())

        val tutorialList = subArg.getCommand()?.tutorial ?: tutorial
        for(line in tutorialList) {
            component = component
                .append(Component.text(line, Colours.GREY))
                .append(Component.newline())
        }
        
        actor.reply(component)
    }

}
