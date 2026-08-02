package org.tainorazum.universum.punishment

import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.CompletableFuture

enum class PunishmentType {
    BAN,
    MUTE,
    KICK,
    WARN,
    NOTE
}

data class Punishment(
    val id: Int? = null,
    val uuid: UUID,
    val type: PunishmentType,
    val reason: String,
    val duration: Duration?,
    val punishedOn: Date,
    val punisherUuid: UUID,
    val archived: Boolean
) {

    fun expiry() : Date? {
        val start = punishedOn
        val duration = duration

        val unbanDate: Date? = if (duration != null) {
            Date.from(start.toInstant().plus(duration))
        } else {
            null
        }
        return unbanDate
    }
}

class PunishmentManager {

    fun punish(playerUUID: UUID, type: PunishmentType, reason: String, duration: Duration, punisherUUID: UUID): CompletableFuture<Boolean> {
        return DB_Punishments.punish(
            Punishment(
            uuid = playerUUID,
            type = type,
            reason = reason,
            duration = duration,
            punishedOn = Date(),
            punisherUuid = punisherUUID,
            archived = false
            )
        )
    }

    fun unPunish(playerUUID: UUID, id: Int) : CompletableFuture<Boolean> {
        return DB_Punishments.unPunish(playerUUID, id)
    }

    fun isBanned(uuid: UUID): CompletableFuture<Punishment?> {
        val future = CompletableFuture<Punishment?>()

        DB_Punishments.getPunishments(uuid).thenAccept { punishments ->
            punishments.forEach { p ->
                if(p.type == PunishmentType.BAN && !p.archived) future.complete(p)
            }
        }

        return future
    }

}