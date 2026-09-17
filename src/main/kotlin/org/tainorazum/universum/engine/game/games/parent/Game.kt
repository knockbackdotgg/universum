package org.tainorazum.universum.engine.game.games.parent

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.games.event.GameEvents
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameSpectator
import org.tainorazum.universum.engine.game.manager.fields.GameState
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import java.util.UUID

open class Game(

    open val arena: Arena,
    open val teams: List<GameTeam>,

    open val gameType: GameType,
    open val gameSettings: GameSettings

) : GameEvents() {

    private var currentRound: Int = 1
    private var gameState: GameState = GameState.STARTING

    private val spectators = mutableListOf<GameSpectator>()

    init {
        prepare()
        register()
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

    //
    // events
    //

    override fun onPlayerJoin(event: PlayerJoinEvent) {
        val p = event.player

        p.sendMessage(Component.text("player joined", Colours.GREEN))

        PlayerUtility.reset(p, GameMode.SPECTATOR)
        PlayerUtility.blackout(
            p,
            Int.MAX_VALUE,
            50,
            Text.toSmallTextComponent("please wait for the game to start", Colours.YELLOW),
            Component.text("...", Colours.GREY)
        )

        check()

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

    fun addSpectator(spectator: GameSpectator) {
        spectators.add(spectator)
    }

    fun removeSpectator(spectator: GameSpectator) {
        spectators.remove(spectator)
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

    fun check() : Boolean {
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

    final fun prepare() {
        arena.reset().thenAcceptAsync({ success ->
            if (success) {
                NetworkPlayerManager.broadcast(
                    Component.text("Created a game instance on server: ${Universum.server.identifier}", Colours.GREEN)
                )

                val uuids = getUUIDs()
                if (uuids.isEmpty()) {
                    Universum.instance.logger.warning("Game created but getUUIDs() returned an empty list!")
                }

                uuids.forEach { uuid ->
                    NetworkPlayerManager.sendPlayer(uuid, Universum.server.identifier)
                }
            } else {
                NetworkPlayerManager.broadcast(
                    Component.text("Attempt to create a game instance failed on server: ${Universum.server.identifier}", Colours.RED)
                )
                end()
            }
        }, { runnable -> Bukkit.getScheduler().runTask(Universum.instance, runnable) })
            .exceptionally { throwable ->
                throwable.printStackTrace()
                null
            }
    }

    // game & round start
    fun start() {
        gameState(GameState.STARTING)

        getPlayers().forEach { player ->
            PlayerUtility.stopBlackout(player)

        }

    }

    // round stop
    fun stop() {
        gameState(GameState.STOPPING)
    }

    // game stop
    fun end() {
        gameState(GameState.ENDING)
        unregister()
    }

}