package net.syncthing.java.bep.index

import net.syncthing.java.bep.BlockExchangeProtos
import net.syncthing.java.core.beans.*
import net.syncthing.java.core.interfaces.IndexTransaction
import org.bouncycastle.util.encoders.Hex
import org.slf4j.LoggerFactory

object IndexElementProcessor {
    private val logger = LoggerFactory.getLogger(IndexElementProcessor::class.java)

    fun pushRecords(
            transaction: IndexTransaction,
            folder: String,
            updates: List<BlockExchangeProtos.FileInfo>,
            oldRecords: Map<String, FileInfo>,
            folderStatsUpdateCollector: FolderStatsUpdateCollector
    ): List<FileInfo> {
        // this always keeps the last version per path
        val filesToProcess = updates
                .sortedBy { it.sequence }
                .reversed()
                .distinctBy { it.name /* this is the whole path */ }
                .reversed()

        val preparedUpdates = filesToProcess.mapNotNull { prepareUpdate(folder, it, 0) }
        val updatesToApply = preparedUpdates.filter { shouldUpdateRecord(oldRecords[it.first.path], it.first) }
        val sequenceNumbers = transaction.getSequencer().nextSequences(updatesToApply.size).iterator()
        val updatesWithSequenceNumbers = updatesToApply.map { it.first.withSequenceNumber(sequenceNumbers.next()) to it.second }

        transaction.updateFileInfoAndBlocks(
                fileInfos = updatesWithSequenceNumbers.map { it.first },
                fileBlocks = updatesWithSequenceNumbers.mapNotNull { it.second }
        )

        for ((newRecord) in updatesWithSequenceNumbers) {
            updateFolderStatsCollector(oldRecords[newRecord.path], newRecord, folderStatsUpdateCollector)
        }

        return updatesWithSequenceNumbers.map { it.first }
    }

    fun pushRecord(
            transaction: IndexTransaction,
            folder: String,
            bepFileInfo: BlockExchangeProtos.FileInfo,
            folderStatsUpdateCollector: FolderStatsUpdateCollector,
            oldRecord: FileInfo?
    ): FileInfo? {
        val update = prepareUpdate(folder, bepFileInfo, transaction.getSequencer().nextSequence())

        return if (update != null) {
            addRecord(
                    transaction = transaction,
                    newRecord = update.first,
                    fileBlocks = update.second,
                    folderStatsUpdateCollector = folderStatsUpdateCollector,
                    oldRecord = oldRecord
            )
        } else {
            null
        }
    }

    private fun prepareUpdate(
            folder: String,
            bepFileInfo: BlockExchangeProtos.FileInfo,
            sequenceNumber: Long
    ): Pair<FileInfo, FileBlocks?>? {
        val path = bepFileInfo.name
        val isDeleted = bepFileInfo.deleted
        val lastModified = FileLastModifiedTime(
                seconds = bepFileInfo.modifiedS,
                nanoSeconds = bepFileInfo.modifiedNs
        )
        val versionList = (bepFileInfo.version?.countersList ?: emptyList())
                .map { record -> FileVersion(record.id, record.value) }

        return when (bepFileInfo.type) {
            BlockExchangeProtos.FileInfoType.FILE -> {
                val fileBlocks = FileBlocks(
                        folder,
                        path,
                        ((bepFileInfo.blocksList ?: emptyList())).map { record ->
                            BlockInfo(record.offset, record.size, Hex.toHexString(record.hash.toByteArray()))
                        }
                )

                val fileInfo = FileFileInfo(
                        folder = folder,
                        path = path,
                        isDeleted = isDeleted,
                        lastModified = lastModified,
                        versionList = versionList,
                        hash = fileBlocks.hash,
                        size = bepFileInfo.size,
                        lastModifiedBy = bepFileInfo.modifiedBy,
                        invalid = true,  // we don't serve this file
                        permissions = bepFileInfo.permissions,
                        noPermissions = bepFileInfo.noPermissions,
                        sequence = sequenceNumber
                )

                fileInfo to fileBlocks
            }
            BlockExchangeProtos.FileInfoType.DIRECTORY -> {
                val fileInfo = DirectoryFileInfo(
                        folder = folder,
                        path = path,
                        isDeleted = isDeleted,
                        versionList = versionList,
                        lastModified = lastModified,
                        lastModifiedBy = bepFileInfo.modifiedBy,
                        permissions = bepFileInfo.permissions,
                        noPermissions = bepFileInfo.noPermissions,
                        sequence = sequenceNumber
                )

                fileInfo to null
            }
            BlockExchangeProtos.FileInfoType.SYMLINK -> {
                val fileInfo = SymlinkFileInfo(
                        folder = folder,
                        path = path,
                        isDeleted = isDeleted,
                        versionList = versionList,
                        lastModified = lastModified,
                        lastModifiedBy = bepFileInfo.modifiedBy,
                        symlinkTarget = bepFileInfo.symlinkTarget,
                        sequence = bepFileInfo.sequence,
                        permissions = bepFileInfo.permissions,
                        noPermissions = bepFileInfo.noPermissions
                )

                fileInfo to null
            }
            else -> {
                logger.warn("unsupported file type = {}, discarding file info", bepFileInfo.type)

                null
            }
        }
    }

    private fun shouldUpdateRecord(
            oldRecord: FileInfo?,
            newRecord: FileInfo
    ) = oldRecord == null || newRecord.lastModified >= oldRecord.lastModified

    private fun addRecord(
            transaction: IndexTransaction,
            newRecord: FileInfo,
            oldRecord: FileInfo?,
            fileBlocks: FileBlocks?,
            folderStatsUpdateCollector: FolderStatsUpdateCollector
    ): FileInfo? {
        return if (shouldUpdateRecord(oldRecord, newRecord)) {
            logger.trace("discarding record = {}, modified before local record", newRecord)
            null
        } else {
            logger.trace("loaded new record = {}", newRecord)

            transaction.updateFileInfo(newRecord, fileBlocks)
            updateFolderStatsCollector(oldRecord, newRecord, folderStatsUpdateCollector)

            newRecord
        }
    }

    private fun updateFolderStatsCollector(
            oldRecord: FileInfo?,
            newRecord: FileInfo,
            folderStatsUpdateCollector: FolderStatsUpdateCollector
    ) {
        val oldMissing = oldRecord == null || oldRecord.isDeleted
        val newMissing = newRecord.isDeleted

        if (oldRecord is FileFileInfo && !oldMissing) {
            folderStatsUpdateCollector.deltaSize -= oldRecord.size
        }

        if (newRecord is FileFileInfo && !newMissing) {
            folderStatsUpdateCollector.deltaSize += newRecord.size
        }

        if (!oldMissing) {
            when (oldRecord!!) {
                is FileFileInfo -> folderStatsUpdateCollector.deltaFileCount--
                is DirectoryFileInfo -> folderStatsUpdateCollector.deltaDirCount--
                is SymlinkFileInfo -> null /* ignore it */
            }.let { /* require handling all paths */ }
        }

        if (!newMissing) {
            when (newRecord) {
                is FileFileInfo -> folderStatsUpdateCollector.deltaFileCount++
                is DirectoryFileInfo -> folderStatsUpdateCollector.deltaDirCount++
                is SymlinkFileInfo -> null /* ignore it */
            }.let { /* require handling all paths */ }
        }

        folderStatsUpdateCollector.lastModified = newRecord.lastModified
    }
}
