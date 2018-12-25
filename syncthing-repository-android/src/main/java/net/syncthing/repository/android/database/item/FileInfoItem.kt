package net.syncthing.repository.android.database.item

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.TypeConverters
import net.syncthing.java.core.beans.*
import net.syncthing.repository.android.database.converters.DateConverter
import net.syncthing.repository.android.database.converters.FileTypeConverter
import java.util.*

@Entity(
        tableName = "file_info",
        primaryKeys = ["folder", "path"],
        indices = [
            Index(value = ["folder", "parent"])
        ]
)
@TypeConverters(
        DateConverter::class,
        FileTypeConverter::class
)
data class FileInfoItem(
        @ColumnInfo(index = true)
        val folder: String,
        val path: String,
        @ColumnInfo(name = "file_name")
        val fileName: String,
        val parent: String,
        val size: Long?,
        val hash: String?,
        @ColumnInfo(name = "last_modified")
        val lastModified: Date,
        @ColumnInfo(name = "file_type")
        val fileType: FileInfoItemType,
        @ColumnInfo(name = "version_id")
        val versionId: Long,
        @ColumnInfo(name = "version_value")
        val versionValue: Long,
        @ColumnInfo(name = "is_deleted")
        val isDeleted: Boolean,
        @ColumnInfo(name = "symlink_target")
        val symlinkTarget: String
) {
    companion object {
        fun fromNative(item: FileInfo) = when (item) {
            is FileFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModified = item.lastModified,
                    fileType = FileInfoItemType.File,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = item.size,
                    hash = item.hash,
                    symlinkTarget = ""
            )
            is DirectoryFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModified = item.lastModified,
                    fileType = FileInfoItemType.Directory,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = null,
                    hash = null,
                    symlinkTarget = ""
            )
            is SymlinkFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModified = item.lastModified,
                    fileType = FileInfoItemType.Directory,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = null,
                    hash = null,
                    symlinkTarget = item.symlinkTarget
            )
        }
    }

    init {
        when (fileType) {
            FileInfoItemType.Directory -> {
                if (size != null || hash != null) {
                    throw IllegalArgumentException()
                }
            }
            FileInfoItemType.File -> {
                if (size == null || hash == null) {
                    throw IllegalArgumentException()
                }
            }
            FileInfoItemType.Symlink -> {
                if (size != null || hash != null) {
                    throw IllegalArgumentException()
                }
            }
        }
    }

    @delegate:Transient
    val native: FileInfo by lazy {
        when (fileType) {
            FileInfoItemType.Directory -> DirectoryFileInfo(
                    folder = folder,
                    path = path,
                    lastModified = lastModified,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted
            )
            FileInfoItemType.File -> FileFileInfo(
                    folder = folder,
                    path = path,
                    lastModified = lastModified,
                    size = size!!,
                    hash = hash!!,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted
            )
            FileInfoItemType.Symlink -> SymlinkFileInfo(
                    folder = folder,
                    path = path,
                    lastModified = lastModified,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted,
                    symlinkTarget = symlinkTarget
            )
        }
    }
}

@TypeConverters(DateConverter::class)
data class FileInfoLastModified(
        @ColumnInfo(name = "last_modified")
        val lastModified: Date
)

enum class FileInfoItemType {
    File, Directory, Symlink
}
