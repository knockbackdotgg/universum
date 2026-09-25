package org.tainorazum.universum.engine.game.manager.fields

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