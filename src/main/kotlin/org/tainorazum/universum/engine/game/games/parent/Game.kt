package org.tainorazum.universum.engine.game.games.parent

import gg.flyte.twilight.scheduler.delay
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scoreboard.Team
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.arena.Arena
import org.tainorazum.universum.engine.game.games.event.GameEvents
import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.game.manager.fields.GameSpectator
import org.tainorazum.universum.engine.game.manager.fields.GameState
import org.tainorazum.universum.engine.game.manager.fields.GameTeam
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.statistics.StatisticType
import org.tainorazum.universum.engine.statistics.StatisticsManager
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.utility.player.PlayerUtility
import org.tainorazum.universum.utility.text.Colours
import java.util.UUID
import java.util.concurrent.CompletableFuture

open class Game(

    open val arena: Arena,
    open val teams: List<GameTeam>,

    open val kit: Kit? = null,

    open val gameType: GameType,
    open val gameSettings: GameSettings

) : GameEvents() {

    private var currentRound: Int = 1
    private var gameState: GameState = GameState.STARTING

    private val spectators = mutableListOf<GameSpectator>()

    private val stats = StatisticsManager

    init {
        register()
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

    //
    // events
    //

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        val p = event.player

        if(teams.size < 2) {

            // 1v1
            if(getPlayers().size == 2) {
                val winnerTeam = getOpponentTeams(p)
                end(winner = winnerTeam.firstOrNull())
            }
            else {
                // game continues
                removePlayer(p)
            }

        }

    }

    override fun onPlayerJoin(event: PlayerJoinEvent) {
        val p = event.player

        p.sendMessage(Component.text("player joined", Colours.GREEN))

        PlayerUtility.reset(p, GameMode.SPECTATOR)

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

    fun getOpponentTeams(player: Player): List<GameTeam?> {
        val opponents = mutableListOf<GameTeam>()

        teams.forEach { team ->
            if(!team.uuids.contains(player.uniqueId)) {
                opponents.add(team)
            }
        }
        return opponents
    }

    fun getOpponentTeams(team: GameTeam): List<GameTeam?> {
        val opponents = mutableListOf<GameTeam>()

        teams.forEach { t ->
            if(t != team) {
                opponents.add(team)
            }
        }
        return opponents
    }

    fun getTeam(player: Player): GameTeam? {
        return teams.find { team -> team.uuids.contains(player.uniqueId) }
    }

    fun getTeamPlayers(id: String): List<Player> {
        val players = ArrayList<Player>()

        teams.forEach { team ->
            if(team.id == id) {
                team.uuids.forEach {
                    Bukkit.getPlayer(it)?.let { p -> players += p }
                }
            }
        }
        return players
    }

    fun removePlayer(player: Player) {
        teams.find { team -> team.uuids.contains(player.uniqueId) }?.uuids?.remove(player.uniqueId)
    }

    //
    // pre-game management
    //

    fun check() : Boolean {
        val players = getPlayers()
        val uuids = getUUIDs()

        val ready = players.size == uuids.size

        if(ready) start()

        Universum.instance.logger.info("Checking to start game. Result: $ready")

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
        Bukkit.getScheduler().runTask(Universum.instance, Runnable {
            try {
                NetworkPlayerManager.broadcast(
                    Component.text("Created a game instance on server: ${Universum.server.identifier}", Colours.GREEN)
                )

                val uuids = getUUIDs()
                Universum.instance.logger.info("[Game] Found ${uuids.size} UUIDs for player dispatch.")

                if (uuids.isEmpty()) {
                    Universum.instance.logger.warning("[Game] Game created but getUUIDs() returned an empty list!")
                }

                uuids.forEach { uuid ->
                    Universum.instance.logger.info("[Game] Sending player $uuid to server ${Universum.server.identifier}")
                    NetworkPlayerManager.sendPlayer(uuid, Universum.server.identifier)
                }
            } catch (e: Exception) {
                Universum.instance.logger.severe("[Game] Failed during player dispatch:")
                e.printStackTrace()
                end()
            }
        })
    }

    // teleportation

    fun tpToStart(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        arena.getLocations().thenAccept { locationMap ->
            Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                try {
                    teams.forEach { team ->
                        val players = getTeamPlayers(team.id)

                        players.forEachIndexed { index, player ->
                            val playerNumber = index + 1
                            val key = "${team.id}.$playerNumber"
                            val location = locationMap[key]

                            if (location != null) {
                                player.teleport(location)
                            } else {
                                Universum.instance.logger.warning("[Game] Missing start location for key: $key")
                            }
                        }
                    }
                    future.complete(true)
                } catch (e: Exception) {
                    Universum.instance.logger.severe("[Game] Error during teleportation: ${e.message}")
                    e.printStackTrace()
                    future.complete(false)
                }
            })
        }.exceptionally { throwable ->
            Universum.instance.logger.severe("[Game] Failed to fetch arena locations: ${throwable.message}")
            future.complete(false)
            null
        }

        return future
    }

    // kits

    /**
     * @return Whether the function completed. (Will return true even if there is no kit required for this game.)
     * @return Returns false if an error has occurred.
     */
    fun supplyKit(player: Player): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        if(kit != null) {

            kit!!.applyToPlayer(player).thenAccept { success ->
                future.complete(success)
            }

        } else future.complete(true)

        return future

    }

    // game & round start
    fun start() {
        gameState(GameState.STARTING)

        val tpFuture = tpToStart()

        CompletableFuture.allOf(tpFuture).thenAccept {
            val tp = tpFuture.get()

            if(tp) {
                getPlayers().forEach { player ->
                    supplyKit(player).thenAccept { success ->
                        if(success) {
                            PlayerUtility.stopBlackout(player)
                        } else {
                            end(error = true)
                        }
                    }
                }
            } else {
                end(error = true)
            }
        }
    }

    // round stop
    fun stop() {
        gameState(GameState.STOPPING)
    }

    // game stop
    fun end(winner: GameTeam? = null, error: Boolean = false) {
        gameState(GameState.ENDING)

        // statistics
        if(winner != null) {
            getTeamPlayers(winner.id).forEach { player ->
                stats.add(StatisticType.WINS, gameType, player.uniqueId, 1.0, gameSettings.isRanked)
            }
            getOpponentTeams(winner).forEach { team ->
                if(team != null) {
                    getTeamPlayers(team.id).forEach { player ->
                        stats.add(StatisticType.LOSSES, gameType, player.uniqueId, 1.0, gameSettings.isRanked)
                    }
                }
            }
        }

        // error
        if(error) {
            Universum.instance.logger.warning("[Game] Game stopped due to an error.")
            // alert players & admin
        }

        // tp back to lobby
        delay(20*5) {
            getPlayers().forEach { player ->
                NetworkPlayerManager.sendPlayerToLobby(player.uniqueId)
            }
        }

        delay(20*1) {
            unregister()
            arena.release()
        }

    }

}