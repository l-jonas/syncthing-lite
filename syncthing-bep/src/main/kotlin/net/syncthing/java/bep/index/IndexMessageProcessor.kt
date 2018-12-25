package net.syncthing.java.bep.index

import net.syncthing.java.bep.BlockExchangeProtos
import net.syncthing.java.core.beans.DeviceId
import net.syncthing.java.core.beans.FileInfo
import net.syncthing.java.core.beans.FolderStats
import net.syncthing.java.core.beans.IndexInfo
import net.syncthing.java.core.interfaces.IndexTransaction
import org.slf4j.LoggerFactory
import java.util.*

object IndexMessageProcessor {
    private val logger = LoggerFactory.getLogger(IndexMessageProcessor::class.java)

    fun doHandleIndexMessageReceivedEvent(
            message: BlockExchangeProtos.IndexUpdate,
            peerDeviceId: DeviceId,
            transaction: IndexTransaction
    ): Result {
        val folderId = message.folder
        val oldIndexInfo = transaction.findIndexInfoByDeviceAndFolder(peerDeviceId, folderId)
                ?: throw IndexInfoNotFoundException()

        logger.debug("processing {} index records for folder {}", message.filesList.size, folderId)

        val oldRecords = transaction.findFileInfo(folderId, message.filesList.map { it.name })
        val folderStatsUpdateCollector = FolderStatsUpdateCollector(message.folder)

        val newRecords = IndexElementProcessor.pushRecords(
                transaction = transaction,
                oldRecords = oldRecords,
                folder = folderId,
                folderStatsUpdateCollector = folderStatsUpdateCollector,
                updates = message.filesList
        )

        val newIndexInfo = if (message.filesList.isEmpty()) {
            oldIndexInfo
        } else {
            var sequence: Long = -1

            for (newRecord in message.filesList) {
                sequence = Math.max(newRecord.sequence, sequence)
            }

            handleFolderStatsUpdate(transaction, folderStatsUpdateCollector)

            UpdateIndexInfo.updateIndexInfoFromIndexElementProcessor(transaction, oldIndexInfo, sequence)
        }

        return Result(newIndexInfo, newRecords.toList(), transaction.findFolderStats(folderId) ?: FolderStats.createDummy(folderId))
    }

    fun handleFolderStatsUpdate(transaction: IndexTransaction, folderStatsUpdateCollector: FolderStatsUpdateCollector) {
        if (folderStatsUpdateCollector.isEmpty()) {
            return
        }

        transaction.updateOrInsertFolderStats(
                folder = folderStatsUpdateCollector.folderId,
                deltaSize = folderStatsUpdateCollector.deltaSize,
                deltaFileCount = folderStatsUpdateCollector.deltaFileCount,
                deltaDirCount = folderStatsUpdateCollector.deltaDirCount
        )

        folderStatsUpdateCollector.lastModified.let { fileLastModifiedTime ->
            if (fileLastModifiedTime != null) {
                transaction.updateOrInsertFolderStats(
                        folder = folderStatsUpdateCollector.folderId,
                        lastUpdate = Date(fileLastModifiedTime.roundedMilliseconds)
                )
            }
        }
    }

    data class Result(val newIndexInfo: IndexInfo, val updatedFiles: List<FileInfo>, val newFolderStats: FolderStats)
    class IndexInfoNotFoundException: RuntimeException()
}
