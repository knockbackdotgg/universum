package org.tainorazum.universum.chat.manager

import com.google.gson.Gson
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.chat.ChatMessage
import org.tainorazum.universum.cosmetic.util.CosmeticType
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.utility.other.formatDate
import org.tainorazum.universum.utility.other.toHumanReadable
import org.tainorazum.universum.utility.permissions.LuckpermsUtil
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.ColorCodeParser
import org.tainorazum.universum.utility.text.Messages
import org.tainorazum.universum.utility.text.Messages.replace
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ChatManager {

    private val formatString = "%rank%&r&7%name%&r %tag%&r&7» &7%message%&r"
    private val gson = Gson()

    fun executeMessage(uuid: UUID, rawMessage: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val rankFuture = LuckpermsUtil.getRankComponent(uuid)
        val nameFuture = PlayerUtility.getName(uuid)
        val mutedFuture = Universum.punishmentManager.isMuted(uuid)
        val chatFilterFuture = Universum.chatFilterManager.filter(uuid, rawMessage)
        val tagFuture = Universum.cosmeticManager.get(uuid, CosmeticType.CHATTAG)

        CompletableFuture.allOf(rankFuture, nameFuture, mutedFuture, chatFilterFuture, tagFuture).thenAccept {
            try {
                var rank = rankFuture.get()
                val name = nameFuture.get() ?: run {
                    future.complete(false)
                    return@thenAccept
                }
                val muted = mutedFuture.get()
                val chatFilter = chatFilterFuture.get()
                val tag = tagFuture.get() as? Component ?: Component.empty()

                if (muted != null) {
                    val msg = Messages.MUTED_MSG()
                        .replace("<id>", muted.id.toString())
                        .replace("<type>", muted.type.toString())
                        .replace("<reason>", muted.reason)
                        .replace("<duration>", muted.duration?.toHumanReadable() ?: "/")
                        .replace("<muted>", formatDate(muted.punishedOn))
                        .replace("<unmuted>", muted.expiry()?.let { formatDate(it) } ?: "NEVER")

                    Bukkit.getPlayer(uuid)?.sendMessage(msg)
                    future.complete(false)
                    return@thenAccept
                }

                if (chatFilter == true) {
                    Bukkit.getPlayer(uuid)?.sendMessage(Messages.CHAT_FILTER_MSG())
                    future.complete(false)
                    return@thenAccept
                }

                val plainRank = PlainTextComponentSerializer.plainText().serialize(rank)

                if (plainRank.isNotBlank()) {
                    rank = rank.append(Component.text(" "))
                }

                val baseComponent = ColorCodeParser.parse(formatString)

                val finalComponent = baseComponent
                    .replace("%rank%", rank)
                    .replace("%message%", rawMessage)
                    .replace("%name%", name)
                    .replace("%tag%", tag)

                val jsonComponent = GsonComponentSerializer.gson().serialize(finalComponent)
                val chatMessage = ChatMessage(uuid, jsonComponent, Universum.server)

                sendMessage(chatMessage)

                RedisManager.publish("chat_message", gson.toJson(chatMessage))
                future.complete(true)
            } catch (e: Exception) {
                Universum.instance.logger.severe("Error processing chat message: ${e.message}")
                future.completeExceptionally(e)
            }
        }

        return future
    }

    fun sendMessage(chatMessage: ChatMessage) {
        if(chatMessage.uuid == null) { // server sent message
            // Broadcast formatted Adventure Component to all local players
            Bukkit.getOnlinePlayers().forEach { player ->
                player.sendMessage(chatMessage.getComponent())
            }
        } else {
            Bukkit.getPlayer(chatMessage.uuid)?.sendMessage(chatMessage.getComponent())
        }

    }

}