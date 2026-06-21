package org.tainorazum.universum.server.npc

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.bukkit.Location
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.DatabaseUtil
import org.tainorazum.universum.server.npc.database.DB_LobbyNPC
import org.tainorazum.universum.server.npc.database.DB_NPCAction
import org.tainorazum.universum.server.npc.database.DB_NPCHologram

/**
 * Service to create, modify, and delete NPCs and their holograms.
 * DB operations run async; respawn happens on main thread afterward.
 */
object NpcModifier {

    /**
     * Create a new NPC in the database and spawn it.
     */
    fun createNpc(
        name: String,
        location: Location,
        skinTexture: String? = null,
        skinSignature: String? = null,
        lookAtPlayer: Boolean = false,
        copyViewerSkin: Boolean = false
    ) {
        DatabaseUtil.dba {
            transaction {
                val id = DB_LobbyNPC.insert {
                    it[DB_LobbyNPC.name] = name
                    it[DB_LobbyNPC.world] = location.world?.name ?: "world"
                    it[DB_LobbyNPC.x] = location.x
                    it[DB_LobbyNPC.y] = location.y
                    it[DB_LobbyNPC.z] = location.z
                    it[DB_LobbyNPC.yaw] = location.yaw
                    it[DB_LobbyNPC.pitch] = location.pitch
                    it[DB_LobbyNPC.skinTexture] = skinTexture
                    it[DB_LobbyNPC.skinSignature] = skinSignature
                    it[DB_LobbyNPC.lookAtPlayer] = lookAtPlayer
                    it[DB_LobbyNPC.copyViewerSkin] = copyViewerSkin
                } get DB_LobbyNPC.id

                Universum.instance.logger.info("NPC '$name' created with ID $id")
            }
        }.thenRun {
            // reload and respawn
            NpcManager.reloadAndRespawn()
        }
    }

    /**
     * Update an existing NPC and respawn it.
     */
    fun updateNpc(
        npcId: Long,
        name: String? = null,
        location: Location? = null,
        skinTexture: String? = null,
        skinSignature: String? = null,
        lookAtPlayer: Boolean? = null,
        copyViewerSkin: Boolean? = null
    ) {
        DatabaseUtil.dba {
            transaction {
                DB_LobbyNPC.update({ DB_LobbyNPC.id eq npcId }) {
                    name?.let { n -> it[DB_LobbyNPC.name] = n }
                    location?.let { l ->
                        it[DB_LobbyNPC.world] = l.world?.name ?: "world"
                        it[DB_LobbyNPC.x] = l.x
                        it[DB_LobbyNPC.y] = l.y
                        it[DB_LobbyNPC.z] = l.z
                        it[DB_LobbyNPC.yaw] = l.yaw
                        it[DB_LobbyNPC.pitch] = l.pitch
                    }
                    skinTexture?.let { s -> it[DB_LobbyNPC.skinTexture] = s }
                    skinSignature?.let { s -> it[DB_LobbyNPC.skinSignature] = s }
                    lookAtPlayer?.let { b -> it[DB_LobbyNPC.lookAtPlayer] = b }
                    copyViewerSkin?.let { b -> it[DB_LobbyNPC.copyViewerSkin] = b }
                }
                Universum.instance.logger.info("NPC ID $npcId updated")
            }
        }.thenRun {
            // reload and respawn only this NPC
            NpcManager.reloadAndRespawn(npcId)
        }
    }

    /**
     * Delete an NPC and all its holograms/actions.
     */
    fun deleteNpc(npcId: Long) {
        DatabaseUtil.dba {
            transaction {
                DB_NPCHologram.deleteWhere { DB_NPCHologram.npcId eq npcId }
                DB_NPCAction.deleteWhere { DB_NPCAction.npcId eq npcId }
                DB_LobbyNPC.deleteWhere { DB_LobbyNPC.id eq npcId }
                Universum.instance.logger.info("NPC ID $npcId deleted along with holograms and actions")
            }
        }.thenRun {
            // despawn
            NpcManager.despawnNpc(npcId)
        }
    }

    /**
     * Add a hologram line to an NPC.
     */
    fun addHologramLine(
        npcId: Long,
        lineIndex: Int,
        type: String = "TEXT",
        content: String,
        offsetY: Double = 0.0
    ) {
        DatabaseUtil.dba {
            transaction {
                DB_NPCHologram.insert {
                    it[DB_NPCHologram.npcId] = npcId
                    it[DB_NPCHologram.lineIndex] = lineIndex
                    it[DB_NPCHologram.type] = type
                    it[DB_NPCHologram.content] = content
                    it[DB_NPCHologram.offsetY] = offsetY
                }
                Universum.instance.logger.info("Hologram line added to NPC $npcId at index $lineIndex")
            }
        }.thenRun {
            // respawn holograms for this NPC
            NpcManager.reloadAndRespawn(npcId)
        }
    }

    /**
     * Clear all holograms for an NPC.
     */
    fun clearHolograms(npcId: Long) {
        DatabaseUtil.dba {
            transaction {
                DB_NPCHologram.deleteWhere { DB_NPCHologram.npcId eq npcId }
                Universum.instance.logger.info("Holograms cleared for NPC $npcId")
            }
        }.thenRun {
            NpcManager.reloadAndRespawn(npcId)
        }
    }

    /**
     * Add an action to an NPC.
     */
    fun addAction(
        npcId: Long,
        actionType: String,
        actionData: String,
        order: Int = 0
    ) {
        DatabaseUtil.dba {
            transaction {
                DB_NPCAction.insert {
                    it[DB_NPCAction.npcId] = npcId
                    it[DB_NPCAction.actionType] = actionType
                    it[DB_NPCAction.actionData] = actionData
                    it[DB_NPCAction.order] = order
                }
                Universum.instance.logger.info("Action '$actionType' added to NPC $npcId")
            }
        }
    }

    /**
     * Clear all actions for an NPC.
     */
    fun clearActions(npcId: Long) {
        DatabaseUtil.dba {
            transaction {
                DB_NPCAction.deleteWhere { DB_NPCAction.npcId eq npcId }
                Universum.instance.logger.info("Actions cleared for NPC $npcId")
            }
        }
    }
}
