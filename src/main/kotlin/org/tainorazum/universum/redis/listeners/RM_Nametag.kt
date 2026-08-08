package org.tainorazum.universum.redis.listeners

import org.tainorazum.universum.network.nametag.NametagManager
import org.tainorazum.universum.redis.RedisSubscriber

class RM_Nametag : RM_Channel("nametag") {

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                if (message.startsWith("REMOVE:")) {
                    val uuid = message.removePrefix("REMOVE:")
                    NametagManager.handleNetworkRemove(uuid)
                } else {
                    NametagManager.handleNetworkUpdate(message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}