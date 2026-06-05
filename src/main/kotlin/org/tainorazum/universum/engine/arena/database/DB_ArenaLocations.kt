package org.tainorazum.universum.engine.arena.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object DB_ArenaLocations : Table("arena_locations") {

    val id = integer("id").autoIncrement()

    val identifier = varchar("arena_id", 20)
        .references(DB_Arenas.identifier, onDelete = ReferenceOption.CASCADE)

    val locationKey = varchar("location_key", 32)
    val locationData = varchar("location_data", 100)

    override val primaryKey = PrimaryKey(id)
}