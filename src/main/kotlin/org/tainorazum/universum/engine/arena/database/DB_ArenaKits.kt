package org.tainorazum.universum.engine.arena.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.tainorazum.universum.engine.kit.database.DB_Kits

object DB_ArenaKits : Table("arena_kits") {

    val arenaId = varchar("arena_id", 20)
        .references(DB_Arenas.identifier, onDelete = ReferenceOption.CASCADE)
    val kitId = varchar("kit_id", 100)
        .references(DB_Kits.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(arenaId, kitId)
}
