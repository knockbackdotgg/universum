package org.tainorazum.universum.engine.kit.database

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.and
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.util.KitSerializer
import org.tainorazum.universum.utility.text.ComponentSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import org.tainorazum.universum.utility.database.DatabaseUtil.mta
import java.util.UUID
import java.util.concurrent.CompletableFuture

object DB_Kits : Table("db_kits") {
    private const val DEFAULT_OWNER_MARKER = "_"

    val id = varchar("id", 100)
    val name = text("name") // frontend visual component
    val ownerUuid = varchar("owner_uuid", 36).default(DEFAULT_OWNER_MARKER)
    val kitJson = text("kit_json")
    val schemaVersion = integer("schema_version").default(1)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id, ownerUuid)

    private data class KitSnapshot(
        val contents: Array<ItemStack?>,
        val armor: Array<ItemStack?>,
        val offhand: ItemStack?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as KitSnapshot

            if (!contents.contentEquals(other.contents)) return false
            if (!armor.contentEquals(other.armor)) return false
            if (offhand != other.offhand) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contents.contentHashCode()
            result = 31 * result + armor.contentHashCode()
            result = 31 * result + (offhand?.hashCode() ?: 0)
            return result
        }
    }

    private fun ownerKey(owner: UUID?): String {
        return owner?.toString() ?: DEFAULT_OWNER_MARKER
    }

    private fun snapshotPlayerInventory(player: Player): CompletableFuture<KitSnapshot> {
        return mta {
            KitSnapshot(
                contents = player.inventory.contents.map { it?.clone() }.toTypedArray(),
                armor = player.inventory.armorContents.map { it?.clone() }.toTypedArray(),
                offhand = player.inventory.itemInOffHand.clone()
            )
        }
    }

    private fun findKitRow(id: String, ownerUuid: UUID?): ResultRow? {
        if (ownerUuid != null) {
            selectAll()
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq ownerKey(ownerUuid)) }
                .limit(1)
                .firstOrNull()
                ?.let { return it }
        }

        return selectAll()
            .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER) }
            .limit(1)
            .limit(1)
            .firstOrNull()
    }

    private fun storeKit(id: String, name: Component, ownerUuid: UUID?, snapshot: KitSnapshot): Boolean {
        val json = KitSerializer.inventoryToJson(
            snapshot.contents,
            snapshot.armor,
            snapshot.offhand
        )
        val ownerValue = ownerKey(ownerUuid)

        deleteWhere { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq ownerValue) }
        insert {
            it[DB_Kits.id] = id
            it[DB_Kits.name] = ComponentSerializer.serialize(name)
            it[DB_Kits.ownerUuid] = ownerValue
            it[DB_Kits.kitJson] = json
            it[DB_Kits.schemaVersion] = 1
            it[DB_Kits.createdAt] = System.currentTimeMillis()
        }
        return true
    }

    /**
     * @Usage Save personal kit as server Player.
     */
    fun saveKitFromPlayer(id: String, name: Component, owner: UUID?, player: Player): CompletableFuture<Boolean> {
        return snapshotPlayerInventory(player).thenCompose { snapshot ->
            dba {
                storeKit(id, name, owner, snapshot)
            }
        }
    }

    /**
    * @Usage Save default kit as server Administrator.
     */
    fun saveDefaultKitFromPlayer(id: String, name: Component, player: Player): CompletableFuture<Boolean> {
        return saveKitFromPlayer(id, name, null, player)
    }

    fun getKit(id: String, ownerUuid: UUID?): CompletableFuture<Kit?> {
        return dba {
            val row = findKitRow(id, ownerUuid) ?: return@dba null
            Kit(
                id = row[DB_Kits.id]
            )
        }
    }

    fun getKit(id: String): CompletableFuture<Kit?> = getKit(id, null)

    fun getKitJson(id: String, ownerUuid: UUID?): CompletableFuture<String?> {
        return dba {
            val row = findKitRow(id, ownerUuid) ?: return@dba null
            row[DB_Kits.kitJson]
        }
    }

    fun getKitJson(id: String): CompletableFuture<String?> = getKitJson(id, null)

    fun getDefaultKitJson(id: String): CompletableFuture<String?> {
        return dba {
            selectAll()
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER) }
                .limit(1)
                .firstOrNull()
                ?.get(DB_Kits.kitJson)
        }
    }

    fun resetKit(id: String): CompletableFuture<Int> {
        return dba {
            deleteWhere { DB_Kits.id eq id }
        }
    }

    fun resetKit(id: String, player: UUID): CompletableFuture<Int> {
        return dba {
            deleteWhere { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq player.toString()) }
        }
    }

    fun resetKit(id: String, player: Player): CompletableFuture<Int> = resetKit(id, player.uniqueId)

    fun resetAllKitsForPlayer(player: UUID): CompletableFuture<Int> {
        return dba {
            deleteWhere { DB_Kits.ownerUuid eq player.toString() }
        }
    }

    fun resetAllKitsForPlayer(player: Player): CompletableFuture<Int> = resetAllKitsForPlayer(player.uniqueId)
}
