package org.tainorazum.universum.engine.game.games.child

import org.bukkit.event.entity.PlayerDeathEvent
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.games.parent.Game
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.engine.game.manager.fields.GameType

class Ffa(

    override val arena: Arena,
    override val teams: List<GameTeam>,

    override val gameType: GameType,
    override val gameSettings: GameSettings

) : Game(arena, teams, gameType, gameSettings) {

    override fun onPlayerDeath(event: PlayerDeathEvent) {

    }

}