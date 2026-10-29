package org.tainorazum.universum.engine.game.games.child

import org.bukkit.event.entity.PlayerDeathEvent
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.games.parent.Game
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.kit.Kit

class FFA(

    override val arena: Arena,
    override val teams: List<GameTeam>,

    override val kit: Kit?,

    override val gameType: GameType,
    override val gameSettings: GameSettings

) : Game(arena, teams, kit, gameType, gameSettings) {

    override fun onPlayerDeath(event: PlayerDeathEvent) {

    }

}