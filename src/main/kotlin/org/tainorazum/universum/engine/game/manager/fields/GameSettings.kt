package org.tainorazum.universum.engine.game.manager.fields

import org.bukkit.Material
import org.tainorazum.universum.engine.kit.Kit

data class GameSettings (

    val kit: Kit? = null,
    val rounds: Int = 1,
    val isRanked: Boolean = false,

    val canModifyBlocks: Boolean = false,
    val canModifyMap: Boolean = false,

    val changeFoodLevel: Boolean = false,
    val defaultFoodLevel: Int = 20,

    val maxHealth: Int = 20,
    val defaultHealth: Int = 20,

)