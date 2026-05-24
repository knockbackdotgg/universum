package org.tainorazum.universum.game.arena.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.bukkit.Bukkit
import org.bukkit.Location

object DB_ArenaLocations : Table("arena_locations") {
    val id = integer("id").autoIncrement()

    val arenaIdentifier = varchar("arena_identifier", 20)
        .references(DB_Arenas.identifier, onDelete = ReferenceOption.CASCADE)

    val locationKey = varchar("location_key", 32)

    val locationData = varchar("location_data", 100)

    override val primaryKey = PrimaryKey(id)
}