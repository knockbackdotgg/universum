package org.tainorazum.universum.chat

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.Component
import java.util.UUID

data class ChatMessage(
    val uuid: UUID? = null, // null = server sent message
    val messageJson: String
) {
    fun getComponent(): Component {
        return GsonComponentSerializer.gson().deserialize(messageJson)
    }
}