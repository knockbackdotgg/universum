package org.tainorazum.universum.utility.maps

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.util.SideEffect
import com.sk89q.worldedit.util.SideEffectSet
import org.bukkit.Location
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture

object SchematicUtil {

    fun pasteSchematicTo(
        schematicBytes: ByteArray,
        targetLocation: Location,
        ignoreAir: Boolean = false
    ): CompletableFuture<Boolean> {
        val world = targetLocation.world ?: return CompletableFuture.failedFuture(
            IllegalArgumentException("Target location world cannot be null.")
        )

        val worldRef = BukkitAdapter.adapt(world)

        return CompletableFuture.supplyAsync {
            try {
                ByteArrayInputStream(schematicBytes).use { inputStream ->
                    val format = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                    val reader = format.getReader(inputStream)
                    val clipboard = reader.read() ?: throw NullPointerException("Clipboard read returned null")

                    val region = clipboard.region
                    val schemCenter = region.center
                    val originToCenterOffset = schemCenter.subtract(clipboard.origin.toVector3())

                    val bukkitTargetVector = BlockVector3.at(
                        targetLocation.blockX,
                        targetLocation.blockY,
                        targetLocation.blockZ
                    )
                    val adjustedPastePosition = bukkitTargetVector.subtract(originToCenterOffset.toBlockPoint())

                    WorldEdit.getInstance().newEditSessionBuilder()
                        .world(worldRef)
                        .fastMode(true)
                        .build().use { editSession ->

                            editSession.setSideEffectApplier(
                                SideEffectSet.defaults().with(SideEffect.LIGHTING, SideEffect.State.OFF)
                            )

                            val operation = ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(adjustedPastePosition)
                                .ignoreAirBlocks(ignoreAir)
                                .copyEntities(false)
                                .build()

                            Operations.complete(operation)

                            editSession.flushQueue()
                        }
                }
                true
            } catch (e: Exception) {
                throw RuntimeException("Failed to paste schematic", e)
            }
        }
    }
}