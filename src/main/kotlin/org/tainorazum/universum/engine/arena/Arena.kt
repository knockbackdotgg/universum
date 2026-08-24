package org.tainorazum.universum.engine.arena

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
import org.tainorazum.universum.utility.maps.SchematicUtil
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
        return SchematicUtil.pasteSchematicTo(schematic, centerLocation)
    }
}