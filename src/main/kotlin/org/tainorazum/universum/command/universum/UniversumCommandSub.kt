package org.tainorazum.universum.command.universum

import org.tainorazum.universum.command.universum.commands.UniversumCommandGeneral


enum class UniversumCommandSub {

    GENERAL,
    OTHER,
    LOBBY,
    DATABASE,
    SERVERLIST,
    KITEDITOR,

    ;

    private var cachedCommand: UniversumCommandGeneral? = null

    fun getCommand(): UniversumCommandGeneral? {
        return cachedCommand
    }

    fun register(command: UniversumCommandGeneral) {
        cachedCommand = command
    }
}