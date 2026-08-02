package org.tainorazum.universum.chat

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.cosmetic.util.CosmeticType
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

    private val formatString = "%rank%&7%name% %tag%&7» &7%message%"

    fun executeMessage(uuid: UUID, rawMessage: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val rankFuture = LuckpermsUtil.getRankComponent(uuid)
        val nameFuture = PlayerUtility.getName(uuid)
        val mutedFuture = Universum.punishmentManager.isMuted(uuid)
        val chatFilterFuture = Universum.chatFilterManager.filter(uuid, rawMessage)
        val tagFuture = Universum.cosmeticManager.get(uuid, CosmeticType.TAG)

        CompletableFuture.allOf(rankFuture, nameFuture, mutedFuture, chatFilterFuture, tagFuture).thenAccept {
            val rank = rankFuture.get()
            val name = nameFuture.get() ?: return@thenAccept
            val muted = mutedFuture.get()
            val chatFilter = chatFilterFuture.get()
            val tag = tagFuture.get() as? Component ?: Component.empty()

            val baseComponent = ColorCodeParser.parse(formatString)

            if(muted != null) {
                val msg = Messages.MUTED_MSG()
                    .replace("<id>", muted.id.toString())
                    .replace("<type>", muted.type.toString())
                    .replace("<reason>", muted.reason)
                    .replace("<duration>", muted.duration?.toHumanReadable() ?: "/")
                    .replace("<muted>", formatDate(muted.punishedOn))
                    .replace("<unmuted>", muted.expiry()?.let { formatDate(it) } ?: "NEVER")

                Bukkit.getPlayer(uuid)?.sendMessage(msg)
                //Universum.logManager.log()

                return@thenAccept
            }

            if(chatFilter == true) {
                Bukkit.getPlayer(uuid)?.sendMessage(Messages.CHAT_FILTER_MSG())
                //Universum.logManager.log()
                return@thenAccept
            }

            val final = baseComponent
                .replace("%rank%", rank)
                .replace("%message%", rawMessage)
                .replace("%name%", name)
                .replace("%tag%", tag)



            future.complete(true)
        }

        return future
    }
}