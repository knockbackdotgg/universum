package org.tainorazum.universum.engine.game.games.parent

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.games.event.GameEvents
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameSpectator
import org.tainorazum.universum.engine.game.manager.fields.GameState
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.utility.player.PlayerUtility
import java.util.UUID

open class Game(

    val arena: Arena,
    val teams: List<GameTeam>,

    val gameType: GameType,
    val gameSettings: GameSettings,

) : GameEvents() {

    private var currentRound: Int = 1
    private var gameState: GameState = GameState.STARTING

    private val spectators = mutableListOf<GameSpectator>()

    init {
        prepare()
    }

    //
    // event setup
    //

    override fun isPlayerInContext(player: Player): Boolean {
        return hasPlayer(player.uniqueId) || isSpectator(player.uniqueId)
    }

    override fun isLocationInContext(location: Location): Boolean {
        return isInArena(location)
    }

    fun registerListeners() {
        register()
    }

    //
    // events
    //

    override fun onPlayerJoin(event: PlayerJoinEvent) {
        PlayerUtility.reset(event.player, GameMode.SPECTATOR)



        //PlayerUtility.blackout()

    }

    //
    // security
    //

    fun hasPlayer(uuid: UUID): Boolean {
        return getUUIDs().contains(uuid)
    }

    fun isInArena(location: Location): Boolean {
        return arena.isInBounds(location)
    }

    //
    // spectator management
    //

    fun isSpectator(uuid: UUID): Boolean {
        return spectators.any { it.uuid == uuid }
    }

    fun getSpectator(uuid: UUID): GameSpectator? {
        return spectators.find { it.uuid == uuid }
    }

    //
    // player management
    //

    fun getPlayers() : List<Player> {
        val players = ArrayList<Player>()

        teams.forEach { team -> team.uuids.forEach { Bukkit.getPlayer(it)?.let { p -> players.add(p) } } }
        return players
    }

    fun getUUIDs() : List<UUID> {
        val uuids = ArrayList<UUID>()

        teams.forEach { team -> team.uuids.forEach { uuid -> uuids.add(uuid) } }
        return uuids
    }

    fun getTeamPlayers(index: Int): List<Player> {
        val players = ArrayList<Player>()

        teams[index].uuids.forEach { Bukkit.getPlayer(it)?.let { p -> players.add(p) } }
        return players
    }

    //
    // pre-game management
    //

    fun readyCheck() : Boolean {
        val players = getPlayers()
        val uuids = getUUIDs()

        val ready = players.size == uuids.size

        if(ready) start()

        return ready
    }

    // game state management

    fun gameState(new: GameState) {
        gameState = new
    }

    fun gameState() = gameState

    //
    // game management
    //

    fun prepare() {
        arena.reset()
    }

    // game & round start
    fun start() {
        gameState(GameState.STARTING)
    }

    // round stop
    fun stop() {
        gameState(GameState.STOPPING)
    }

    // game stop
    fun end() {
        gameState(GameState.ENDING)
    }

}