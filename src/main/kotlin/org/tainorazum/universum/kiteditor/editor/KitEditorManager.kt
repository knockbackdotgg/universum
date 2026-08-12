package org.tainorazum.universum.kiteditor.editor

import org.bukkit.entity.Player
import org.tainorazum.universum.engine.kit.Kit
import java.util.UUID
import java.util.concurrent.CompletableFuture

class KitEditorManager {

    private val editors = mutableListOf<UUID>()

    fun addEditor(uuid: UUID): Boolean {
        val isEditor = isEditor(uuid)
        if(!isEditor) editors.add(uuid)
        return isEditor
    }

    fun removeEditor(uuid: UUID): Boolean {
        return editors.remove(uuid)
    }

    fun isEditor(uuid: UUID): Boolean {
        return editors.contains(uuid)
    }

    /**
     * @usage Set [isDefault] to true if the kit should be saved as a default kit.
     */
    fun editKit(kit: Kit, player: Player, itemAccess: Boolean = false, isDefault: Boolean = false): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val kitEditor = KitEditor(player, kit, itemAccess, isDefault)

        return future
    }

}