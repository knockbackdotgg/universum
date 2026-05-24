package org.tainorazum.universum.utility

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object ComponentSerializer {

    /**
     * Converts an Adventure Component into a raw JSON string suitable for database storage.
     */
    fun serialize(component: Component?): String {
        if (component == null) return "{}"
        return GsonComponentSerializer.gson().serialize(component)
    }

    /**
     * Restores a raw JSON string back into an active Adventure Component.
     * Returns an empty component if the string is blank or invalid.
     */
    fun deserialize(json: String?): Component {
        if (json.isNullOrBlank()) return Component.empty()
        return try {
            GsonComponentSerializer.gson().deserialize(json)
        } catch (e: Exception) {
            Component.text(json)
        }
    }
}