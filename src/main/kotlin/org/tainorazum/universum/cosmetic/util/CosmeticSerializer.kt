package org.tainorazum.universum.cosmetic.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.tainorazum.universum.utility.text.Colours

object CosmeticSerializer {

    private val gson = Gson()
    private val miniMessage = MiniMessage.miniMessage()

    fun serialize(data: CosmeticData): ByteArray {
        val json = JsonObject()

        when (data) {
            is CosmeticData.ChatTag -> {
                json.addProperty("text", data.text)
                json.addProperty("color", data.color.asHexString())
            }
            is CosmeticData.ChatColor -> {
                json.addProperty("color", data.color.asHexString())
            }
            is CosmeticData.Pet -> {
                json.addProperty("entityType", data.entityType.name)
                json.addProperty("displayName", miniMessage.serialize(data.displayName))
                json.addProperty("isBaby", data.isBaby)
            }
            is CosmeticData.ArmorTrim -> {
                json.addProperty("material", data.material.name)
            }
            is CosmeticData.ArmorTrimColor -> {
                json.addProperty("material", data.material.name)
            }
            is CosmeticData.Trail -> {
                json.addProperty("name", data.name)
                json.addProperty("color", data.color.asHexString())
            }
            is CosmeticData.Aura -> {
                json.addProperty("name", data.name)
                json.addProperty("color", data.color.asHexString())
            }
            is CosmeticData.Glow -> {
                json.addProperty("color", data.color.asHexString())
            }
        }

        return json.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Deserializes a ByteArray back into the appropriate CosmeticData instance.
     */
    fun deserialize(type: CosmeticType, bytes: ByteArray): CosmeticData? {
        if (bytes.isEmpty()) return null

        val jsonString = String(bytes, Charsets.UTF_8)
        val json = gson.fromJson(jsonString, JsonObject::class.java) ?: return null

        return when (type) {
            CosmeticType.CHAT_TAG -> {
                val text = json.get("text").asString
                val color = parseColor(json.get("color")?.asString)
                CosmeticData.ChatTag(text, color)
            }
            CosmeticType.CHAT_COLOR -> {
                val color = parseColor(json.get("color")?.asString)
                CosmeticData.ChatColor(color)
            }
            CosmeticType.PET -> {
                val entityType = EntityType.valueOf(json.get("entityType").asString)
                val displayName = miniMessage.deserialize(json.get("displayName").asString)
                val isBaby = json.get("isBaby")?.asBoolean ?: false
                CosmeticData.Pet(entityType, displayName, isBaby)
            }
            CosmeticType.ARMOR_TRIMS -> {
                val mat = Material.valueOf(json.get("material").asString)
                CosmeticData.ArmorTrim(mat)
            }
            CosmeticType.ARMOR_TRIM_COLOR -> {
                val mat = Material.valueOf(json.get("material").asString)
                CosmeticData.ArmorTrimColor(mat)
            }
            CosmeticType.TRAIL -> {
                val color = parseColor(json.get("color")?.asString)
                CosmeticData.Trail(json.get("name").asString, color)
            }
            CosmeticType.AURA -> {
                val color = parseColor(json.get("color")?.asString)
                CosmeticData.Aura(json.get("name").asString, color)
            }
            CosmeticType.GLOW -> {
                val color = parseColor(json.get("color")?.asString)
                CosmeticData.Glow(color)
            }
        }
    }

    private fun parseColor(rawColor: String?): TextColor {
        if (rawColor == null) return Colours.WHITE

        // 1. Try parsing as a hex color (#RRGGBB)
        val hexColor = TextColor.fromHexString(rawColor)
        if (hexColor != null) return hexColor

        // 2. Fall back to named color lookup or default to WHITE
        return Colours.fromName(rawColor) ?: Colours.WHITE
    }
}