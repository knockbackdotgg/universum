package org.tainorazum.universum.server.setting

enum class ServerSetting(val default: Any) {

    CURRENT_MOTD("default"),
    CURRENT_FAVICON("default"),
    WHITELISTED(true),
    REDIS(true)

}