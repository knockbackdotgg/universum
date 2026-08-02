package org.tainorazum.universum.cosmetic.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.EntityType

object CosmeticSerializer {

    private val gson = Gson()
    private val miniMessage = MiniMessage.miniMessage()

    fun serialize(data: CosmeticData): ByteArray {
        val json = JsonObject()

        when (data) {
            is CosmeticData.Tag -> {
                json.addProperty("component", miniMessage.serialize(data.component))
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
                json.addProperty("particle", data.particleName)
            }
            is CosmeticData.Aura -> {
                json.addProperty("particle", data.particleName)
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
            CosmeticType.TAG -> {
                val compString = json.get("component").asString
                CosmeticData.Tag(miniMessage.deserialize(compString))
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
                CosmeticData.Trail(json.get("particle").asString)
            }
            CosmeticType.AURA -> {
                CosmeticData.Aura(json.get("particle").asString)
            }
        }
    }
}