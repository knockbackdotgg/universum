package org.tainorazum.universum.utility.maps

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.util.SideEffect
import com.sk89q.worldedit.util.SideEffectSet
import org.bukkit.Bukkit
import org.bukkit.Location
import org.tainorazum.universum.Universum
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture

object SchematicUtil {

    /**
     * Pastes a schematic so that the CENTER of the schematic
     * is positioned at targetLocation.
     *
     * Bukkit world/chunk operations and the WorldEdit paste
     * are executed on the Minecraft server thread.
     */
    fun pasteSchematicTo(
        schematicBytes: ByteArray,
        targetLocation: Location,
        ignoreAir: Boolean = false
    ): CompletableFuture<Boolean> {

        if (schematicBytes.isEmpty()) {
            return CompletableFuture.failedFuture(
                IllegalArgumentException(
                    "Schematic bytes cannot be empty."
                )
            )
        }

        val world = targetLocation.world
            ?: return CompletableFuture.failedFuture(
                IllegalArgumentException(
                    "Target location world cannot be null."
                )
            )

        val result = CompletableFuture<Boolean>()

        /*
         * Bukkit world/chunk operations MUST run on the
         * Minecraft server thread.
         */
        Bukkit.getScheduler().runTask(
            Universum.instance,
            Runnable {

                try {

                    val chunkX = targetLocation.blockX shr 4
                    val chunkZ = targetLocation.blockZ shr 4

                    /*
                     * Make sure the target chunk is loaded.
                     *
                     * This is safe here because runTask() puts us
                     * on the main server thread.
                     */
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {

                        world.getChunkAt(
                            chunkX,
                            chunkZ
                        ).load()
                    }

                    val worldRef = BukkitAdapter.adapt(world)

                    /*
                     * The exact block that represents the desired
                     * geometric center of the schematic.
                     */
                    val targetCenter = BlockVector3.at(
                        targetLocation.blockX,
                        targetLocation.blockY,
                        targetLocation.blockZ
                    )

                    ByteArrayInputStream(schematicBytes).use { input ->

                        val clipboard =
                            BuiltInClipboardFormat
                                .SPONGE_SCHEMATIC
                                .getReader(input)
                                .use { reader ->

                                    reader.read()
                                        ?: throw IllegalStateException(
                                            "WorldEdit returned a null clipboard."
                                        )
                                }

                        val region = clipboard.region
                        val origin = clipboard.origin

                        /*
                         * Calculate the geometric center of the
                         * schematic's actual copied region.
                         */
                        val regionCenter =
                            region.center.toBlockPoint()

                        /*
                         * WorldEdit pastes relative to the
                         * clipboard origin.
                         *
                         * Therefore:
                         *
                         *     centerOffset =
                         *         regionCenter - clipboardOrigin
                         *
                         * and:
                         *
                         *     pasteOrigin =
                         *         targetCenter - centerOffset
                         */
                        val centerOffset =
                            regionCenter.subtract(origin)

                        val pasteOrigin =
                            targetCenter.subtract(centerOffset)

                        Universum.instance.logger.info(
                            "[KitEditor] Pasting schematic: " +
                                    "world=${world.name}, " +
                                    "targetCenter=$targetCenter, " +
                                    "regionMin=${region.minimumPoint}, " +
                                    "regionMax=${region.maximumPoint}, " +
                                    "regionCenter=$regionCenter, " +
                                    "clipboardOrigin=$origin, " +
                                    "centerOffset=$centerOffset, " +
                                    "pasteOrigin=$pasteOrigin"
                        )

                        /*
                         * Create the WorldEdit edit session.
                         *
                         * This is intentionally NOT wrapped in
                         * CompletableFuture.supplyAsync().
                         */
                        WorldEdit.getInstance()
                            .newEditSessionBuilder()
                            .world(worldRef)
                            .fastMode(true)
                            .build()
                            .use { editSession ->

                                editSession.setSideEffectApplier(
                                    SideEffectSet.defaults()
                                        .with(
                                            SideEffect.LIGHTING,
                                            SideEffect.State.OFF
                                        )
                                )

                                val operation =
                                    ClipboardHolder(clipboard)
                                        .createPaste(editSession)
                                        .to(pasteOrigin)
                                        .ignoreAirBlocks(ignoreAir)
                                        .copyEntities(false)
                                        .build()

                                /*
                                 * FAWE/WorldEdit handles the actual
                                 * operation from here.
                                 */
                                Operations.complete(operation)
                            }
                    }

                    Universum.instance.logger.info(
                        "[KitEditor] Schematic paste completed at " +
                                "${world.name}:" +
                                "${targetCenter.x()}, " +
                                "${targetCenter.y()}, " +
                                "${targetCenter.z()}"
                    )

                    result.complete(true)

                } catch (ex: Exception) {

                    Universum.instance.logger.severe(
                        "[KitEditor] Failed to paste schematic into " +
                                "'${world.name}' at " +
                                "${targetLocation.blockX}, " +
                                "${targetLocation.blockY}, " +
                                "${targetLocation.blockZ}: " +
                                "${ex.message}"
                    )

                    ex.printStackTrace()

                    result.completeExceptionally(
                        RuntimeException(
                            "Failed to paste schematic into '${world.name}'",
                            ex
                        )
                    )
                }
            }
        )

        return result
    }
}
