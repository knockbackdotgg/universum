package org.tainorazum.universum.server.npc

import com.google.gson.Gson
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.server.npc.database.DB_LobbyNPC
import org.tainorazum.universum.server.npc.database.DB_NPCAction
import org.tainorazum.universum.server.npc.database.DB_NPCHologram
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object NpcRepository {
    private val gson = Gson()

    fun loadAll(): CompletableFuture<List<NpcData>> {
        return dba {
            transaction {
                DB_LobbyNPC.selectAll().map {
                    NpcData(
                        id = it[DB_LobbyNPC.id],
                        name = it[DB_LobbyNPC.name],
                        world = it[DB_LobbyNPC.world],
                        x = it[DB_LobbyNPC.x],
                        y = it[DB_LobbyNPC.y],
                        z = it[DB_LobbyNPC.z],
                        yaw = it[DB_LobbyNPC.yaw],
                        pitch = it[DB_LobbyNPC.pitch],
                        skinTexture = it[DB_LobbyNPC.skinTexture],
                        skinSignature = it[DB_LobbyNPC.skinSignature],
                        lookAtPlayer = it[DB_LobbyNPC.lookAtPlayer],
                        copyViewerSkin = it[DB_LobbyNPC.copyViewerSkin]
                    )
                }
            }
        }

    }

    fun loadActionsFor(npcId: Long): CompletableFuture<List<NpcActionModel>> {
        return dba {
            transaction {
                DB_NPCAction.select { DB_NPCAction.npcId eq npcId }.map {
                    NpcActionModel(
                        id = it[DB_NPCAction.id],
                        npcId = it[DB_NPCAction.npcId],
                        actionType = it[DB_NPCAction.actionType],
                        actionData = it[DB_NPCAction.actionData],
                        order = it[DB_NPCAction.order]
                    )
                }
            }
        }
    }



    fun loadHologramsFor(npcId: Long): CompletableFuture<List<HologramLine>> {
        return dba {
            transaction {
                DB_NPCHologram.select { DB_NPCHologram.npcId eq npcId }
                    .orderBy(DB_NPCHologram.lineIndex)
                    .map {
                        HologramLine(
                            id = it[DB_NPCHologram.id],
                            npcId = it[DB_NPCHologram.npcId],
                            lineIndex = it[DB_NPCHologram.lineIndex],
                            type = it[DB_NPCHologram.type],
                            content = it[DB_NPCHologram.content],
                            offsetY = it[DB_NPCHologram.offsetY]
                        )
                    }
            }
        }
    }
}
