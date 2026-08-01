package org.tainorazum.universum.server.punishment

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.tainorazum.universum.engine.game.gametypes.GameType
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object DB_Punishments : Table("punishments") {

    val player_uuid = varchar("player_uuid", 36)

    val reason = text("reason")
    val duration = long("duration")

    val punished_on = long("datetime")
    val punisher_uuid = varchar("punisher_uuid", 36)

    val archived = bool("archived")

    override val primaryKey = PrimaryKey(player_uuid, reason, duration, punished_on, punisher_uuid, archived)

}
