package org.tainorazum.universum.engine.kit.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.inventory.ItemStack
import kotlin.collections.iterator

object KitSerializer {
    private val gson = Gson()

    /**
     * Serialize player inventory (contents, armor, offhand) to a JSON string.
     * Uses ItemStack.serialize() -> Map and lets Gson turn that into JSON.
     */
    fun inventoryToJson(contents: Array<ItemStack?>, armor: Array<ItemStack?>, offhand: ItemStack?): String {
        val contentsMaps = contents.map { it?.serialize() }
        val armorMaps = armor.map { it?.serialize() }
        val offMap = offhand?.serialize()
        val root = mapOf(
            "contents" to contentsMaps,
            "armor" to armorMaps,
            "offhand" to offMap,
            "schema_version" to 1
        )
        return gson.toJson(root)
    }

    /**
     * Deserialize JSON produced by inventoryToJson back into (contents, armor, offhand).
     */
    fun jsonToInventory(json: String): Triple<Array<ItemStack?>, Array<ItemStack?>, ItemStack?> {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        val raw = gson.fromJson<Map<String, Any?>>(json, type)

        @Suppress("UNCHECKED_CAST")
        val contentsList = raw["contents"] as? List<Any?> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val armorList = raw["armor"] as? List<Any?> ?: emptyList()
        val offObj = raw["offhand"]

        fun mapToItem(obj: Any?): ItemStack? {
            if (obj == null) return null
            val normalized = normalize(obj) as? Map<String, Any?> ?: return null
            @Suppress("UNCHECKED_CAST")
            return ItemStack.deserialize(normalized as Map<String, Any>)
        }

        val contents = Array(36) { i -> mapToItem(contentsList.getOrNull(i)) }
        val armor = Array(4) { i -> mapToItem(armorList.getOrNull(i)) }
        val off = mapToItem(offObj)
        return Triple(contents, armor, off)
    }

    // Convert Gson's generic maps/lists/numbers into types expected by Bukkit's ItemStack.deserialize
    private fun normalize(o: Any?): Any? {
        return when (o) {
            is Map<*, *> -> {
                val m = linkedMapOf<String, Any?>()
                for ((k, v) in o) {
                    if (k is String) m[k] = normalize(v)
                }
                m
            }
            is List<*> -> o.map { normalize(it) }
            is Double -> {
                // Gson parses numbers as Double. If it's a whole number, convert to Int.
                if (o % 1.0 == 0.0) return o.toInt()
                o
            }
            else -> o
        }
    }
}