package org.tainorazum.universum.chat

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.Component
import org.tainorazum.universum.server.server.Server
import java.util.UUID

data class ChatMessage(
    val uuid: UUID? = null, // null = server sent message
    val messageJson: String,
    val server: Server
) {
    fun getComponent(): Component {
        return GsonComponentSerializer.gson().deserialize(messageJson)
    }
}