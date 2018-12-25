package net.syncthing.java.bep.index

import net.syncthing.java.core.beans.FileLastModifiedTime

class FolderStatsUpdateCollector (val folderId: String) {
    var deltaFileCount = 0L
    var deltaDirCount = 0L
    var deltaSize = 0L
    var lastModified: FileLastModifiedTime? = null

    fun isEmpty() = (
            deltaFileCount == 0L &&
                    deltaDirCount == 0L &&
                    deltaSize == 0L &&
                    lastModified == null
            )
}
