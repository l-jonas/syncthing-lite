package net.syncthing.repository.android.database.converters

import android.arch.persistence.room.TypeConverter
import net.syncthing.repository.android.database.item.FileInfoItemType

class FileTypeConverter {
    companion object {
        private const val FILE = "file"
        private const val DIRECTORY = "directory"
    }

    @TypeConverter
    fun toString(type: FileInfoItemType) = when (type) {
        FileInfoItemType.Directory -> DIRECTORY
        FileInfoItemType.File -> FILE
    }

    @TypeConverter
    fun fromString(value: String)  = when (value) {
        FILE -> FileInfoItemType.File
        DIRECTORY -> FileInfoItemType.Directory
        else -> throw IllegalArgumentException()
    }
}
