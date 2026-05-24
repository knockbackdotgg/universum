package org.tainorazum.universum.game.arena.database

import org.jetbrains.exposed.sql.Table
import org.bukkit.Bukkit
import org.bukkit.Location

object DB_Arenas : Table("arenas") {
    val identifier = varchar("identifier", 20) // backend

    val name = text("name") // frontend
    val description = text("description") // frontend

    val schematic = blob("schematic") // backend
    val centerLocation = varchar("center_location", 100)

    override val primaryKey = PrimaryKey(identifier)
}