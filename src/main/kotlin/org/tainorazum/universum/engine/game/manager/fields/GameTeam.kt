package org.tainorazum.universum.engine.game.manager.fields

import java.util.UUID

data class GameTeam(
    val id: String, // 1,2,3,4,..
    val uuids: MutableList<UUID>,

)