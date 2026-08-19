package org.tainorazum.universum.setting.manager

import org.bukkit.Material

enum class PlayerSetting(
    val options: List<String>, // default = 1st option
    val description: String,
    val material: Material,
) {

    ALLOW_PRIVATE_MESSAGES(
        listOf("everyone", "friends", "none"), "Players who are able to send you private message.", Material.BOOK
    ),

    ALLOW_FRIEND_REQUESTS(
        listOf("yes", "no"), "Are players allowed to send you friend requests.", Material.IRON_CHESTPLATE
    ),

    SEE_FRIEND_ACTIVITY(
        listOf("yes", "no"), "See your friends their activity. Ex.: Joins, leaves,..", Material.CLOCK
    ),

    CHAT_MODE(
        listOf("global", "friends", "none"), "Whose messages you receive in chat.", Material.COMPASS
    ),

    SHOW_SIDEBAR(
        listOf("yes", "no"), "Show the sidebar.", Material.OAK_SIGN
    ),

}