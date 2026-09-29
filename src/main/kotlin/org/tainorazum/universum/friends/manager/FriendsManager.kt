package org.tainorazum.universum.friends.manager

import com.google.gson.Gson
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.friends.database.DB_Friends
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.setting.manager.PlayerSetting
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.player.PlayerUtility.noSound
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace
import org.tainorazum.universum.utility.text.Prefix
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class FriendRequest(
    val requesterUUID: UUID,
    val requesterName: String,
    val targetUUID: UUID,
    val targetName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FriendAcceptPayload(
    val acceptorUUID: UUID,
    val acceptorName: String,
    val requesterUUID: UUID,
    val requesterName: String
)

data class FriendRemovePayload(
    val removerUUID: UUID,
    val removerName: String,
    val targetUUID: UUID,
    val targetName: String
)

class FriendsManager {

    private val pendingRequests = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, FriendRequest>>()
    private val requestExpiryMillis = 3 * 60 * 1000L // 3 minutes
    private val gson = Gson()

    val FRIEND_REQUEST_CHANNEL = "friend_request"
    val FRIEND_ACCEPT_CHANNEL = "friend_accept"
    val FRIEND_REMOVE_CHANNEL = "friend_remove"

    /**
     * Called when a player executes /friend add <targetName>
     */
    fun attemptRequestFriend(requester: Player, targetName: String) {
        if (requester.name.equals(targetName, ignoreCase = true)) {
            requester.sendMessage(Messages.FRIEND_REQUEST_SELF())
            requester.noSound()
            return
        }

        PlayerUtility.getUUID(targetName).thenAccept { targetUUID ->
            if (targetUUID == null) {
                requester.sendMessage(Messages.FRIEND_FAILED())
                requester.noSound()
                return@thenAccept
            }

            if (!NetworkPlayerManager.isNetworkOnline(targetUUID)) {
                requester.sendMessage(Messages.NOT_ONLINE_FRIEND_REQUEST())
                requester.noSound()
                return@thenAccept
            }

            val requesterUUID = requester.uniqueId

            DB_Friends.isFriend(requesterUUID, targetUUID).thenAccept { isAlreadyFriend ->
                if (isAlreadyFriend) {
                    requester.sendMessage(Messages.ALREADY_FRIENDS())
                    requester.noSound()
                    return@thenAccept
                }

                // 1. MUTUAL REQUEST CHECK (Auto-Accept)
                // Check if target has ALREADY sent an active request to this requester
                val incomingRequestFromTarget = getValidRequest(targetUUID = requesterUUID, requesterUUID = targetUUID)
                if (incomingRequestFromTarget != null) {
                    removePendingRequest(targetUUID = requesterUUID, requesterUUID = targetUUID)

                    DB_Friends.addFriend(requesterUUID, targetUUID).thenAccept { success ->
                        if (success) {
                            val payload = FriendAcceptPayload(
                                acceptorUUID = requesterUUID,
                                acceptorName = requester.name,
                                requesterUUID = targetUUID,
                                requesterName = targetName
                            )

                            RedisManager.publish(FRIEND_ACCEPT_CHANNEL, gson.toJson(payload))
                            requester.sendMessage(Messages.FRIEND_REQUEST_ACCEPTED().replace("<player>", targetName))
                        } else {
                            requester.sendMessage(Messages.FRIEND_FAILED())
                        }
                    }
                    return@thenAccept
                }

                // 2. DUPLICATE SPAM CHECK
                // Check if requester ALREADY has an active, unexpired request sent to target
                val existingOutgoingRequest = getValidRequest(targetUUID = targetUUID, requesterUUID = requesterUUID)
                if (existingOutgoingRequest != null) {
                    val elapsed = System.currentTimeMillis() - existingOutgoingRequest.timestamp
                    val remainingMillis = maxOf(0L, requestExpiryMillis - elapsed)

                    val totalSeconds = remainingMillis / 1000
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    val formattedExpiry = "${minutes}m${seconds}s"

                    requester.sendMessage(
                        Messages.PENDING_FRIEND_REQUEST_EXISTS()
                            .replace("<player>", targetName)
                            .replace("<time>", formattedExpiry)
                    )
                    requester.noSound()
                    return@thenAccept
                }

                Universum.playerSettingManager.getSetting(targetUUID, PlayerSetting.ALLOW_FRIEND_REQUESTS)
                    .thenAccept { settingValue ->
                        if (settingValue.equals("no", ignoreCase = true)) {
                            requester.sendMessage(Messages.DENY_FRIEND_REQUESTS())
                            requester.noSound()
                            return@thenAccept
                        }

                        val request = FriendRequest(
                            requesterUUID = requesterUUID,
                            requesterName = requester.name,
                            targetUUID = targetUUID,
                            targetName = targetName
                        )

                        addPendingRequest(request)
                        RedisManager.publish(FRIEND_REQUEST_CHANNEL, gson.toJson(request))

                        requester.sendMessage(Messages.SENT_FRIEND_REQUEST().replace("<player>", targetName))
                    }
            }
        }
    }

    /**
     * Called when a player executes /friend accept <requesterName>
     */
    fun acceptFriend(acceptor: Player, requesterName: String) {
        PlayerUtility.getUUID(requesterName).thenAccept { requesterUUID ->
            if (requesterUUID == null) {
                acceptor.sendMessage(Messages.FRIEND_FAILED())
                acceptor.noSound()
                return@thenAccept
            }

            val acceptorUUID = acceptor.uniqueId
            val request = getValidRequest(acceptorUUID, requesterUUID)

            if (request == null) {
                acceptor.sendMessage(Messages.NO_FRIEND_REQUESTS().replace("<player>", requesterName))
                acceptor.noSound()
                return@thenAccept
            }

            removePendingRequest(acceptorUUID, requesterUUID)

            DB_Friends.addFriend(acceptorUUID, requesterUUID).thenAccept { success ->
                if (success) {
                    val payload = FriendAcceptPayload(
                        acceptorUUID = acceptorUUID,
                        acceptorName = acceptor.name,
                        requesterUUID = requesterUUID,
                        requesterName = requesterName
                    )

                    RedisManager.publish(FRIEND_ACCEPT_CHANNEL, gson.toJson(payload))
                    acceptor.sendMessage(Messages.FRIEND_REQUEST_ACCEPTED().replace("<player>", requesterName))
                } else {
                    acceptor.sendMessage(Messages.FRIEND_FAILED())
                }
            }
        }
    }

    /**
     * Called when a player executes /friend remove <targetName>
     */
    fun removeFriend(remover: Player, targetName: String) {
        if (remover.name.equals(targetName, ignoreCase = true)) {
            remover.sendMessage(Messages.FRIEND_REMOVE_SELF())
            remover.noSound()
            return
        }

        PlayerUtility.getUUID(targetName).thenAccept { targetUUID ->
            if (targetUUID == null) {
                remover.sendMessage(Messages.FRIEND_FAILED())
                remover.noSound()
                return@thenAccept
            }

            val removerUUID = remover.uniqueId

            DB_Friends.isFriend(removerUUID, targetUUID).thenAccept { isFriend ->
                if (!isFriend) {
                    remover.sendMessage(Messages.NOT_FRIENDS().replace("<player>", targetName))
                    remover.noSound()
                    return@thenAccept
                }

                DB_Friends.removeFriend(removerUUID, targetUUID).thenAccept { success ->
                    if (success) {
                        val payload = FriendRemovePayload(
                            removerUUID = removerUUID,
                            removerName = remover.name,
                            targetUUID = targetUUID,
                            targetName = targetName
                        )

                        RedisManager.publish(FRIEND_REMOVE_CHANNEL, gson.toJson(payload))
                        remover.sendMessage(Messages.FRIEND_REMOVED().replace("<player>", targetName))
                    } else {
                        remover.sendMessage(Messages.FRIEND_FAILED())
                        remover.noSound()
                    }
                }
            }
        }
    }

    fun addPendingRequest(request: FriendRequest) {
        val userMap = pendingRequests.computeIfAbsent(request.targetUUID) { ConcurrentHashMap() }
        userMap[request.requesterUUID] = request
    }

    fun getValidRequest(targetUUID: UUID, requesterUUID: UUID): FriendRequest? {
        val targetMap = pendingRequests[targetUUID] ?: return null
        val request = targetMap[requesterUUID] ?: return null

        if (System.currentTimeMillis() - request.timestamp > requestExpiryMillis) {
            targetMap.remove(requesterUUID)
            return null
        }
        return request
    }

    fun removePendingRequest(targetUUID: UUID, requesterUUID: UUID) {
        pendingRequests[targetUUID]?.remove(requesterUUID)
    }

    fun friendActivityJoinNotify(uuid: UUID) {
        DB_Friends.getFriends(uuid).thenAccept { friends ->
            friends.forEach { friend ->
                Universum.playerSettingManager.getSetting(friend, PlayerSetting.SEE_FRIEND_ACTIVITY).thenAccept { value ->
                    if (value.lowercase() == "yes") {
                        PlayerUtility.getName(uuid).thenAccept { name ->
                            if (name != null) {
                                NetworkPlayerManager.attemptServerMessage(
                                    friend,
                                    Prefix.FRIEND().append(Prefix.ARROW3()).append(Messages.FRIEND_JOINED().replace("<player>", name))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun friendActivityLeaveNotify(uuid: UUID) {
        DB_Friends.getFriends(uuid).thenAccept { friends ->
            friends.forEach { friend ->
                Universum.playerSettingManager.getSetting(friend, PlayerSetting.SEE_FRIEND_ACTIVITY).thenAccept { value ->
                    if (value.lowercase() == "yes") {
                        PlayerUtility.getName(uuid).thenAccept { name ->
                            if (name != null) {

                                NetworkPlayerManager.attemptServerMessage(
                                    friend,
                                    Prefix.FRIEND().append(Prefix.ARROW3()).append(Messages.FRIEND_LEFT().replace("<player>", name))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}