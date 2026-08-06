package org.tainorazum.universum.engine.arena

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CompletableFuture

open class Arena(
    val identifier: String,
    val templateIdentifier: String,
    val schematic: ByteArray,
    val centerLocation: Location,
    val radius: Double,
) {

    fun create() : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        build().thenAccept { success ->
            if(success) {
                future.complete(true)
            } else {
                future.complete(false)
            }
        }
        return future
    }

    /**
     * @usage First attempt to build the arena using the given schematic.
     * @return A CompletableFuture that completes on the main thread when done.
     */
    protected fun build() : CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        if (schematic.isEmpty()) {
            future.completeExceptionally(IllegalArgumentException("Schematic bytes are empty."))
            return future
        }
        return reset()
    }

    /**
     * @usage Resets the arena to its original schematic.
     * @return A CompletableFuture that completes when done.
     */
    fun reset() : CompletableFuture<Boolean> {
        return pasteSchematicTo(schematic, centerLocation)
    }

    /**
    * Pastes a schematic file asynchronously using FastAsyncWorldEdit.
    * @param schematicFile The .schem or .schematic file on disk.
    * @param targetLocation The center of where the schematic should be pasted.
    * @param ignoreAir If true, air blocks in the schematic won't overwrite existing blocks.
    * @return A CompletableFuture<Boolean> that completes on the main thread when done.
    */
    private fun pasteSchematicTo(
        schematicFile: File,
        targetLocation: Location,
        ignoreAir: Boolean = false
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        if (!schematicFile.exists()) {
            future.completeExceptionally(IllegalArgumentException("Schematic file does not exist: ${schematicFile.path}"))
            return future
        }

        val worldRef = BukkitAdapter.adapt(targetLocation.world)
        val format = ClipboardFormats.findByFile(schematicFile)
            ?: throw IllegalArgumentException("Unknown schematic format for file: ${schematicFile.name}")

        CompletableFuture.runAsync {
            try {
                format.getReader(FileInputStream(schematicFile)).use { reader ->
                    val clipboard = reader.read() ?: throw NullPointerException("Clipboard returned null")

                    val region = clipboard.region
                    val schemCenter = region.center
                    val originToCenterOffset = schemCenter.subtract(clipboard.origin.toVector3())

                    val bukkitTargetVector = BlockVector3.at(targetLocation.x, targetLocation.y, targetLocation.z)
                    val adjustedPastePosition = bukkitTargetVector.subtract(originToCenterOffset.toBlockPoint())

                    WorldEdit.getInstance().newEditSession(worldRef).use { editSession ->

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
                future.complete(true)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Pastes a schematic from a byte array asynchronously using FastAsyncWorldEdit.
     * @param schematicBytes The raw binary data of the schematic kept in memory.
     * @param targetLocation The center of where the schematic should be pasted.
     * @param ignoreAir If true, air blocks in the schematic won't overwrite existing blocks.
     * @return A CompletableFuture<Boolean> that completes when done.
     */
    private fun pasteSchematicTo(
        schematicBytes: ByteArray,
        targetLocation: Location,
        ignoreAir: Boolean = false
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val world = targetLocation.world ?: run {
            future.completeExceptionally(IllegalArgumentException("Target location world cannot be null."))
            return future
        }

        val worldRef = BukkitAdapter.adapt(world)

        CompletableFuture.runAsync {
            try {
                ByteArrayInputStream(schematicBytes).use { inputStream ->
                    BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(inputStream).use { reader ->
                        val clipboard = reader.read() ?: throw NullPointerException("Clipboard returned null")

                        val region = clipboard.region
                        val schemCenter = region.center
                        val originToCenterOffset = schemCenter.subtract(clipboard.origin.toVector3())

                        val bukkitTargetVector = BlockVector3.at(targetLocation.x, targetLocation.y, targetLocation.z)
                        val adjustedPastePosition = bukkitTargetVector.subtract(originToCenterOffset.toBlockPoint())

                        WorldEdit.getInstance().newEditSession(worldRef).use { editSession ->
                            val operation = ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(adjustedPastePosition)
                                .ignoreAirBlocks(ignoreAir)
                                .copyEntities(false)
                                .build()

                            Operations.complete(operation)
                            editSession.close() // Close flushes queue and commits changes safely
                        }
                    }
                }
                future.complete(true)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

}