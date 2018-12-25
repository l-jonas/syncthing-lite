package net.syncthing.repository.android.database.item

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.TypeConverters
import net.syncthing.java.core.beans.*
import net.syncthing.repository.android.database.converters.FileTypeConverter

@Entity(
        tableName = "file_info",
        primaryKeys = ["folder", "path"],
        indices = [
            Index(value = ["folder", "parent"])
        ]
)
@TypeConverters(
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
        @ColumnInfo(name = "last_modified_s")
        val lastModifiedSeconds: Long,
        @ColumnInfo(name = "last_modified_ns")
        val lastModifiedNanoseconds: Int,
        @ColumnInfo(name = "last_modified_by")
        val lastModifiedBy: Long,
        @ColumnInfo(name = "file_type")
        val fileType: FileInfoItemType,
        @ColumnInfo(name = "version_id")
        val versionId: Long,
        @ColumnInfo(name = "version_value")
        val versionValue: Long,
        @ColumnInfo(name = "is_deleted")
        val isDeleted: Boolean,
        @ColumnInfo(name = "symlink_target")
        val symlinkTarget: String,
        @ColumnInfo(name = "permissions")
        val permissions: Int,
        @ColumnInfo(name = "no_permissions")
        val noPermissions: Boolean,
        @ColumnInfo(name = "invalid")
        val invalid: Boolean,
        @ColumnInfo(name = "sequence")
        val sequence: Long
) {
    companion object {
        fun fromNative(item: FileInfo) = when (item) {
            is FileFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModifiedSeconds = item.lastModified.seconds,
                    lastModifiedNanoseconds = item.lastModified.nanoSeconds,
                    lastModifiedBy = item.lastModifiedBy,
                    fileType = FileInfoItemType.File,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = item.size,
                    hash = item.hash,
                    symlinkTarget = "",
                    invalid = item.invalid,
                    permissions = item.permissions,
                    noPermissions = item.noPermissions,
                    sequence = item.sequence
            )
            is DirectoryFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModifiedSeconds = item.lastModified.seconds,
                    lastModifiedNanoseconds = item.lastModified.nanoSeconds,
                    lastModifiedBy = item.lastModifiedBy,
                    fileType = FileInfoItemType.Directory,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = null,
                    hash = null,
                    symlinkTarget = "",
                    invalid = false,
                    permissions = item.permissions,
                    noPermissions = item.noPermissions,
                    sequence = item.sequence
            )
            is SymlinkFileInfo -> FileInfoItem(
                    folder = item.folder,
                    path = item.path,
                    fileName = item.fileName,
                    parent = item.parent,
                    lastModifiedSeconds = item.lastModified.seconds,
                    lastModifiedNanoseconds = item.lastModified.nanoSeconds,
                    lastModifiedBy = item.lastModifiedBy,
                    fileType = FileInfoItemType.Directory,
                    versionId = item.versionList.last().id,
                    versionValue = item.versionList.last().value,
                    isDeleted = item.isDeleted,
                    size = null,
                    hash = null,
                    symlinkTarget = item.symlinkTarget,
                    invalid = false,
                    permissions = item.permissions,
                    noPermissions = item.noPermissions,
                    sequence = item.sequence
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
                    lastModified = FileLastModifiedTime(
                            seconds = lastModifiedSeconds,
                            nanoSeconds = lastModifiedNanoseconds
                    ),
                    lastModifiedBy = lastModifiedBy,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted,
                    noPermissions = noPermissions,
                    permissions = permissions,
                    sequence = sequence
            )
            FileInfoItemType.File -> FileFileInfo(
                    folder = folder,
                    path = path,
                    lastModified = FileLastModifiedTime(
                            seconds = lastModifiedSeconds,
                            nanoSeconds = lastModifiedNanoseconds
                    ),
                    lastModifiedBy = lastModifiedBy,
                    size = size!!,
                    hash = hash!!,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted,
                    noPermissions = noPermissions,
                    permissions = permissions,
                    sequence = sequence,
                    invalid = invalid
            )
            FileInfoItemType.Symlink -> SymlinkFileInfo(
                    folder = folder,
                    path = path,
                    lastModified = FileLastModifiedTime(
                            seconds = lastModifiedSeconds,
                            nanoSeconds = lastModifiedNanoseconds
                    ),
                    lastModifiedBy = lastModifiedBy,
                    versionList = listOf(FileVersion(
                            id = versionId,
                            value = versionValue
                    )),
                    isDeleted = isDeleted,
                    symlinkTarget = symlinkTarget,
                    noPermissions = noPermissions,
                    permissions = permissions,
                    sequence = sequence
            )
        }
    }
}

data class FileInfoLastModified(
        @ColumnInfo(name = "last_modified_s")
        val lastModifiedSeconds: Long,
        @ColumnInfo(name = "last_modified_ns")
        val lastModifiedNanoseconds: Int
) {
    @delegate:Transient
    val native: FileLastModifiedTime by lazy {
        FileLastModifiedTime(
                seconds = lastModifiedSeconds,
                nanoSeconds = lastModifiedNanoseconds
        )
    }
}

enum class FileInfoItemType {
    File, Directory, Symlink
}
