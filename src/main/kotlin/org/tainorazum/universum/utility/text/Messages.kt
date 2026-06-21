package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component

object Messages {

    fun Component.replace(placeholder: String, value: String): Component {
        return this.replaceText { b ->
            b.matchLiteral(placeholder)
                .replacement(Component.text(value))
        }
    }

    fun Component.replace(placeholder: String, value: Component): Component {
        return this.replaceText { b ->
            b.matchLiteral(placeholder)
                .replacement(value)
        }
    }

    // server
    fun SHUTDOWN_KICK_MSG() = Component.text("You have been kicked: Server shutdown.", Colours.RED)
    fun WHITELIST_KICK_MSG() = Component.text("You have been kicked: Server whitelist enabled.", Colours.RED)
    fun MAINTENANCE_MSG() =
        Component.text("Server is currently under maintenance, please try again later.", Colours.RED)

    fun WHITELIST_MSG() = Component.text("Server is currently whitelisted, please try again later.", Colours.RED)

    // join/leave
    fun JOIN_MSG() = Component.text("+ <name>", Colours.GREEN)
    fun LEAVE_MSG() = Component.text("")
    fun KICKED_MSG() = Component.text("")
    fun KICKED_ERROR_MSG() = Component.text("We apologize, an error has occurred. Please rejoin.", Colours.RED)

    // manage player
    fun INSERT_PLAYER() = Component.text("Player has to be online.", Colours.RED)

    // command
    fun INVALID_COMMAND() = Component.text("Invalid sub-command.", Colours.RED)

    // kiteditor
    fun KITEDITOR_NOT_ALLOWED() = Component.text("You can't edit your kits right now.", Colours.RED)

    // gamemode
    fun CHANGED_GAMEMODE_MSG() =
        Component.text("You are now in gamemode: ", Colours.GREY).append(Component.text("<gm>", Colours.YELLOW))

    fun CHANGED_TARGET_GAMEMODE_MSG() = Component.text("You changed <player>'s gamemode to: ", Colours.GREY)
        .append(Component.text("<gm>", Colours.YELLOW))

    // player
    fun NO_PERMISSION_COMMAND() = Component.text("You are not allowed to execute this command!", Colours.RED)

    // game: ffa
    fun FFA_DEATH_PERSONAL() = Component.text("You have been killed by <opponent>!", Colours.RED)
    fun FFA_KILL_PERSONAL() = Component.text("You just killed <opponent>! ", Colours.GREEN)
    fun FFA_KILL_PUBLIC() = Component.text("<player> was killed by <killer>.", Colours.YELLOW)
    fun FFA_SUICIDE_PUBLIC() = Component.text("<player> died.", Colours.YELLOW)

    // game: duel
    fun DUEL_KILL_PUBLIC() = Component.text("<player> was killed by <killer>.", Colours.YELLOW)
    fun DUEL_DIE_PUBLIC() = Component.text("<player> died.", Colours.YELLOW)
    fun DUEL_DISALLOW() = Component.text("You aren't allowed to join.", Colours.RED)

    // friends
    fun ALREADY_FRIENDS() = Component.text("You are already friends with this player.", Colours.RED)
    fun ALREADY_FRIEND_REQUESTED() =
        Component.text("You already friend requested this player, please wait for them to accept.", Colours.RED)

    fun DENY_FRIEND_REQUESTS() = Component.text("This player does not accept friend requests.", Colours.RED)
    fun NOT_ONLINE_FRIEND_REQUEST() = Component.text("Player has to be online to send a friend request.", Colours.RED)
    fun SENT_FRIEND_REQUEST() = Component.text("You sent that player a friend request!", Colours.GREEN)
    fun FRIEND_REQUEST_RECEIVED() = Component.text(" <player> has sent you a friend request.", Colours.GREY)
    fun FRIEND_REQUEST_ACCEPTED() = Component.text("You are now friends with <player>!", Colours.GREEN)
    fun FRIEND_REQUEST_SELF() = Component.text("You can't friend request yourself.", Colours.RED)

}

