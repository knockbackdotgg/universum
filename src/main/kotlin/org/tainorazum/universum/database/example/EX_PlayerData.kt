package org.tainorazum.universum.database.example

import org.jetbrains.exposed.sql.Table

object EX_PlayerData : Table("player_data") {
    val uuid = varchar("uuid", 36)
    val coins = integer("coins").default(0)
    val currentRank = varchar("current_rank", 16).default("DEFAULT")

    override val primaryKey = PrimaryKey(uuid)
}