package org.tainorazum.universum.redis.listeners.friends

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.manager.FriendAcceptPayload
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace

class RM_FriendAccept : RM_Channel(Universum.friendsManager.FRIEND_ACCEPT_CHANNEL) {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val payload = gson.fromJson(message, FriendAcceptPayload::class.java)

                // If the original requester is on this server, notify them
                val requesterPlayer = Bukkit.getPlayer(payload.requesterUUID)
                if (requesterPlayer != null && requesterPlayer.isOnline) {
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        requesterPlayer.sendMessage(Messages.FRIEND_REQUEST_ACCEPTED().replace("<player>", payload.acceptorName))
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}