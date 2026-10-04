package org.tainorazum.universum.engine.statistics

import org.tainorazum.universum.engine.game.manager.fields.GameType
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import java.util.UUID
import java.util.concurrent.CompletableFuture

object StatisticsManager {

    fun add(
        id: StatisticType,
        gameType: GameType,
        playerUuid: UUID,
        delta: Double,
        ranked: Boolean = false
    ): CompletableFuture<Boolean> {
        return DB_Stats.add(id, gameType, playerUuid.toString(), delta, ranked)
    }

}