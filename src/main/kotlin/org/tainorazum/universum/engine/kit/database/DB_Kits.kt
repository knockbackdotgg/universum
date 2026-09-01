package org.tainorazum.universum.engine.kit.database

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.util.KitSerializer
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import org.tainorazum.universum.utility.database.DatabaseUtil.mta
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.ComponentSerializer
import java.util.UUID
import java.util.concurrent.CompletableFuture

object DB_Kits : Table("kits") {
    private const val DEFAULT_OWNER_MARKER = "_"

    val id = varchar("id", 100).uniqueIndex()
    val name = text("name") // frontend visual component
    val ownerUuid = varchar("owner_uuid", 36).default(DEFAULT_OWNER_MARKER)
    val material = text("material").default(Material.PAPER.name)
    val canCustomize = bool("can_customize").default(true)
    val itemAccess = bool("item_access").default(false)
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

    /**
     * Finds a custom kit for a given player, falling back to default in a single query.
     */
    private fun findKitRow(id: String, ownerUuid: UUID?): ResultRow? {
        val ownerValue = ownerKey(ownerUuid)
        val allowedOwners = if (ownerValue != DEFAULT_OWNER_MARKER) {
            listOf(ownerValue, DEFAULT_OWNER_MARKER)
        } else {
            listOf(DEFAULT_OWNER_MARKER)
        }

        return selectAll()
            .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid inList allowedOwners) }
            .limit(2)
            .sortedByDescending { it[DB_Kits.ownerUuid] != DEFAULT_OWNER_MARKER }
            .firstOrNull()
    }

    private fun storeKit(
        id: String,
        name: Component?,
        ownerUuid: UUID?,
        snapshot: KitSnapshot,
        material: Material?,
        canCustomize: Boolean? = true,
        itemAccess: Boolean? = false
    ): Boolean {
        return try {
            val json = KitSerializer.inventoryToJson(
                snapshot.contents,
                snapshot.armor,
                snapshot.offhand
            )
            val ownerValue = ownerKey(ownerUuid)

            // Query database ONCE if fallback values are required
            val existingRow = if (name == null || material == null) findKitRow(id, ownerUuid) else null

            val finalNameComponent = name ?: existingRow?.get(DB_Kits.name)?.let {
                ComponentSerializer.deserialize(it)
            } ?: Component.text(id.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })

            val finalMaterial = material?.name ?: existingRow?.get(DB_Kits.material) ?: Material.PAPER.name

            // Delete and re-insert for universal compatibility across Exposed versions
            deleteWhere { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq ownerValue) }
            insert {
                it[DB_Kits.id] = id
                it[DB_Kits.name] = ComponentSerializer.serialize(finalNameComponent)
                it[DB_Kits.ownerUuid] = ownerValue
                it[DB_Kits.kitJson] = json
                it[DB_Kits.material] = finalMaterial
                it[DB_Kits.canCustomize] = canCustomize ?: true
                it[DB_Kits.itemAccess] = itemAccess ?: false
                it[DB_Kits.schemaVersion] = 1
                it[DB_Kits.createdAt] = System.currentTimeMillis()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * @Usage Save personal or default kit from a player's inventory.
     */
    fun saveKitFromPlayer(id: String, player: Player, owner: UUID? = null): CompletableFuture<Boolean> {
        return snapshotPlayerInventory(player).thenCompose { snapshot ->
            dba {
                storeKit(id, null, owner, snapshot, null)
            }
        }
    }

    /**
     * @Usage Save default kit as server Administrator.
     */
    fun saveDefaultKitFromPlayer(
        id: String,
        player: Player,
        name: Component? = null,
        material: Material? = null,
        canCustomize: Boolean? = true,
        itemAccess: Boolean? = false
    ): CompletableFuture<Boolean> {
        return snapshotPlayerInventory(player).thenCompose { snapshot ->
            dba {
                storeKit(id, name, null, snapshot, material, canCustomize, itemAccess)
            }
        }
    }

    fun getKit(id: String, ownerUuid: UUID?): CompletableFuture<Kit?> {
        return dba {
            val ownerValue = ownerKey(ownerUuid)
            val allowedOwners = if (ownerValue != DEFAULT_OWNER_MARKER) {
                listOf(ownerValue, DEFAULT_OWNER_MARKER)
            } else {
                listOf(DEFAULT_OWNER_MARKER)
            }

            // Projection: ignore kitJson BLOB to improve speeds
            val cols = listOf(DB_Kits.id, DB_Kits.material, DB_Kits.canCustomize, DB_Kits.itemAccess, DB_Kits.ownerUuid)
            val row = select(cols)
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid inList allowedOwners) }
                .limit(2)
                .sortedByDescending { it[DB_Kits.ownerUuid] != DEFAULT_OWNER_MARKER }
                .firstOrNull() ?: return@dba null

            val matName = row[DB_Kits.material]
            val mat = runCatching { Material.valueOf(matName) }.getOrNull() ?: Material.PAPER

            Kit(
                id = row[DB_Kits.id],
                material = mat,
                canCustomize = row[DB_Kits.canCustomize],
                itemAccess = row[DB_Kits.itemAccess],
            )
        }
    }

    fun getKit(id: String): CompletableFuture<Kit?> = getKit(id, null)

    fun getKitJson(id: String, ownerUuid: UUID?): CompletableFuture<String?> {
        return dba {
            val ownerValue = ownerKey(ownerUuid)
            val allowedOwners = if (ownerValue != DEFAULT_OWNER_MARKER) {
                listOf(ownerValue, DEFAULT_OWNER_MARKER)
            } else {
                listOf(DEFAULT_OWNER_MARKER)
            }

            select(listOf(kitJson, DB_Kits.ownerUuid))
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid inList allowedOwners) }
                .limit(2)
                .sortedByDescending { it[DB_Kits.ownerUuid] != DEFAULT_OWNER_MARKER }
                .firstOrNull()
                ?.get(kitJson)
        }
    }

    fun getKitJson(id: String): CompletableFuture<String?> = getKitJson(id, null)

    fun getDefaultKitJson(id: String): CompletableFuture<String?> {
        return dba {
            select(listOf(kitJson))
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER) }
                .limit(1)
                .firstOrNull()
                ?.get(kitJson)
        }
    }

    fun resetKit(id: String): CompletableFuture<Int> {
        return dba {
            deleteWhere { DB_Kits.id eq id and (DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER) }
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

    /**
     * @Usage Create a new default kit entry using only an identifier.
     * Sets the visual display name to the ID in grey.
     */
    fun createKit(id: String): CompletableFuture<Boolean> {
        val emptySnapshot = KitSnapshot(
            contents = arrayOfNulls(41),
            armor = arrayOfNulls(4),
            offhand = null
        )

        val nameComponent = Component.text(id, Colours.GREY)

        return dba {
            storeKit(
                id = id,
                name = nameComponent,
                ownerUuid = null,
                snapshot = emptySnapshot,
                material = Material.PAPER,
                canCustomize = true,
                itemAccess = false
            )
        }
    }

    fun getAllDefaultKits(): CompletableFuture<List<Kit>> {
        return dba {
            val cols = listOf(id, material, canCustomize, itemAccess)
            select(cols)
                .where { DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER }
                .map { row ->
                    val matName = row[DB_Kits.material]
                    val mat = runCatching { Material.valueOf(matName) }.getOrNull() ?: Material.PAPER

                    Kit(
                        id = row[DB_Kits.id],
                        material = mat,
                        canCustomize = row[DB_Kits.canCustomize],
                        itemAccess = row[DB_Kits.itemAccess],
                    )
                }
        }
    }

    fun getMaterial(id: String): CompletableFuture<Material> {
        return dba {
            select(listOf(material))
                .where { (DB_Kits.id eq id) and (DB_Kits.ownerUuid eq DEFAULT_OWNER_MARKER) }
                .limit(1)
                .firstOrNull()
                ?.get(DB_Kits.material)
                ?.runCatching { Material.valueOf(this) }
                ?.getOrNull() ?: Material.BARRIER
        }
    }
}