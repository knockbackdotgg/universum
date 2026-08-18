package org.tainorazum.universum.redis.listeners.friends

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.manager.FriendRemovePayload
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace

class RM_FriendRemove : RM_Channel(Universum.friendsManager.FRIEND_REMOVE_CHANNEL) {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val payload = gson.fromJson(message, FriendRemovePayload::class.java)

                val targetPlayer = Bukkit.getPlayer(payload.targetUUID)
                if (targetPlayer != null && targetPlayer.isOnline) {
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        targetPlayer.sendMessage(Messages.FRIEND_REMOVED_NOTIFY().replace("<player>", payload.removerName))
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}