package org.tainorazum.universum.command.admin

import org.tainorazum.universum.command.admin.commands.AdminCommandGeneral

enum class AdminCommandSub {

    GENERAL,
    WHITELIST,
    PUNISHMENTS,
    BYPASS

    ;

    private var cachedCommand: AdminCommandGeneral? = null

    fun getCommand(): AdminCommandGeneral? {
        return cachedCommand
    }

    fun register(command: AdminCommandGeneral) {
        cachedCommand = command
    }
}