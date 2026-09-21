package org.tainorazum.universum.engine.arena.database

import org.jetbrains.exposed.sql.Table
import org.tainorazum.universum.database.other.longBlob

object DB_Arenas : Table("arenas") {

    val identifier = varchar("identifier", 20) // backend

    val name = text("name") // frontend
    val description = text("description") // frontend

    val schematic = longBlob("schematic") // backend
    val centerLocation = varchar("center_location", 100)
    val radius = double("radius")

    override val primaryKey = PrimaryKey(identifier)
}