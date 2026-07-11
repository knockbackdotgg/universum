package org.tainorazum.universum.server.setting

enum class ServerSetting(val default: Any) {

    CURRENT_MOTD("ONLINE_DEFAULT"),
    CURRENT_FAVICON("ONLINE_DEFAULT"),
    WHITELISTED(true),
    REDIS(true)

}