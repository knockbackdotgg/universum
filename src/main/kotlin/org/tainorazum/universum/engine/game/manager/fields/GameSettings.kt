package org.tainorazum.universum.engine.game.manager.fields

import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class GameSettings(
    val rounds: Int = 1,
    val isRanked: Boolean = false,

    val canModifyBlocks: Boolean = false,
    val canModifyMap: Boolean = false,

    val changeFoodLevel: Boolean = false,
    val defaultFoodLevel: Int = 20,

    val maxHealth: Int = 20,
    val defaultHealth: Int = 20
)

object GameSettingsSerializer {

    private val gson: Gson = GsonBuilder()
        .create()

    fun serialize(settings: GameSettings): String {
        return gson.toJson(settings)
    }

    fun deserialize(json: String): GameSettings {
        return runCatching {
            gson.fromJson(json, GameSettings::class.java)
        }.getOrElse {
            GameSettings()
        }
    }
}