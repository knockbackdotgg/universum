package org.tainorazum.universum.engine.arena.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object DB_ArenaGametypes : Table("arena_gametypes") {

    val arenaId = varchar("arena_id", 20)
        .references(DB_Arenas.identifier, onDelete = ReferenceOption.CASCADE)
    val gametype = varchar("gametype", 100)

    override val primaryKey = PrimaryKey(arenaId, gametype)
}
