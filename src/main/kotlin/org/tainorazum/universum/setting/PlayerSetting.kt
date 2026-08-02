package org.tainorazum.universum.setting

enum class PlayerSetting(
    val options: List<String> // default = 1st option
) {

    ALLOW_PRIVATE_MESSAGES(listOf("true, false")),

    ALLOW_FRIEND_REQUESTS(listOf("true", "false")),

    SEE_FRIEND_ACTIVITY(listOf("true", "false")),

    CHAT_MODE(listOf("global", "friends", "none")),

}