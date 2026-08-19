package org.tainorazum.universum.redis.listeners.friends

import com.google.gson.Gson
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.manager.FriendRequest
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Text

class RM_FriendRequest : RM_Channel(Universum.friendsManager.FRIEND_REQUEST_CHANNEL) {

    private val gson = Gson()

    init {
        RedisSubscriber.register(channel) { message ->
            try {
                val request = gson.fromJson(message, FriendRequest::class.java)

                // Store request on this server node
                Universum.friendsManager.addPendingRequest(request)

                // If target player is on this server, notify them on the main thread
                val targetPlayer = Bukkit.getPlayer(request.targetUUID)
                if (targetPlayer != null && targetPlayer.isOnline) {
                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        val smallText = Text.toSmallText("click to accept")

                        val clickableMessage = Messages.FRIEND_REQUEST_RECEIVED()
                            .replaceText { builder ->
                                builder.matchLiteral("<player>")
                                    .replacement(request.requesterName)
                            }
                            .replaceText { builder ->
                                builder.matchLiteral(smallText)
                                    .replacement(
                                        Component.text(smallText)
                                            .clickEvent(ClickEvent.runCommand("/friends accept ${request.requesterName}"))
                                            .hoverEvent(HoverEvent.showText(Component.text("Click to accept ${request.requesterName}'s friend request.", Colours.GREEN)))
                                    )
                            }

                        targetPlayer.sendMessage(clickableMessage)
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}