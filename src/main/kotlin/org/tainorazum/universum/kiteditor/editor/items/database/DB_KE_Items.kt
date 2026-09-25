package org.tainorazum.universum.kiteditor.editor.items.database

import org.bukkit.Material
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.kiteditor.editor.items.KitEditorItemsType
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DB_KE_Items : Table("kiteditor_items") {

    val type = enumerationByName<KitEditorItemsType>("type", 100)
    val material = enumerationByName<Material>("material", 100)

    override val primaryKey = PrimaryKey(type, material)

    private val cachedMaterials =
        ConcurrentHashMap<KitEditorItemsType, List<Material>>()

    private val mutationLocks =
        ConcurrentHashMap<KitEditorItemsType, Any>()

    private fun lockFor(type: KitEditorItemsType): Any =
        mutationLocks.computeIfAbsent(type) { Any() }


    fun getMaterials(
        type: KitEditorItemsType
    ): CompletableFuture<List<Material>> {

        cachedMaterials[type]?.let {
            return CompletableFuture.completedFuture(it)
        }

        return dba {
            val materials = transaction {
                select {
                    DB_KE_Items.type eq type
                }.map {
                    it[DB_KE_Items.material]
                }
            }

            cachedMaterials.putIfAbsent(type, materials)

            cachedMaterials[type] ?: materials
        }
    }


    fun addMaterial(
        type: KitEditorItemsType,
        material: Material
    ): CompletableFuture<Boolean> {

        cachedMaterials[type]?.let { cached ->
            if (material in cached) {
                return CompletableFuture.completedFuture(true)
            }
        }

        return dba {
            synchronized(lockFor(type)) {
                transaction {

                    val exists = select {
                        (DB_KE_Items.type eq type) and
                                (DB_KE_Items.material eq material)
                    }.limit(1).any()

                    if (!exists) {
                        insert {
                            it[DB_KE_Items.type] = type
                            it[DB_KE_Items.material] = material
                        }

                        cachedMaterials.computeIfPresent(type) { _, current ->
                            if (material in current) {
                                current
                            } else {
                                current + material
                            }
                        }
                    }

                    true
                }
            }
        }
    }

    fun addMaterials(
        type: KitEditorItemsType,
        materials: List<Material>
    ): CompletableFuture<Boolean> {

        if (materials.isEmpty()) {
            return CompletableFuture.completedFuture(true)
        }

        val uniqueMaterials = materials.distinct()

        cachedMaterials[type]?.let { cached ->

            val toAdd = uniqueMaterials.filterNot { it in cached }

            if (toAdd.isEmpty()) {
                return CompletableFuture.completedFuture(true)
            }

            return dba {
                synchronized(lockFor(type)) {
                    transaction {

                        val current =
                            cachedMaterials[type] ?: cached

                        val actuallyToAdd =
                            uniqueMaterials.filterNot { it in current }

                        if (actuallyToAdd.isNotEmpty()) {

                            batchInsert(
                                actuallyToAdd,
                                shouldReturnGeneratedValues = false
                            ) { material ->
                                this[DB_KE_Items.type] = type
                                this[DB_KE_Items.material] = material
                            }

                            cachedMaterials[type] =
                                (current + actuallyToAdd).distinct()
                        }
                    }
                }
            }.thenApply { true }
        }


        return dba {
            synchronized(lockFor(type)) {
                transaction {

                    val existing = select {
                        DB_KE_Items.type eq type
                    }
                        .map { it[DB_KE_Items.material] }
                        .toHashSet()

                    val toAdd =
                        uniqueMaterials.filterNot { it in existing }

                    if (toAdd.isNotEmpty()) {
                        batchInsert(
                            toAdd,
                            shouldReturnGeneratedValues = false
                        ) { material ->
                            this[DB_KE_Items.type] = type
                            this[DB_KE_Items.material] = material
                        }
                    }

                    cachedMaterials[type] =
                        (existing + toAdd).toList()
                }
            }

            true
        }
    }


    fun removeMaterial(
        type: KitEditorItemsType,
        material: Material
    ): CompletableFuture<Boolean> {

        cachedMaterials[type]?.let { cached ->
            if (material !in cached) {
                return CompletableFuture.completedFuture(false)
            }
        }

        return dba {
            synchronized(lockFor(type)) {
                transaction {

                    val deleted = deleteWhere {
                        (DB_KE_Items.type eq type) and
                                (DB_KE_Items.material eq material)
                    }

                    if (deleted > 0) {
                        cachedMaterials.computeIfPresent(type) { _, current ->
                            current.filterNot { it == material }
                        }
                    }

                    deleted > 0
                }
            }
        }
    }
}