package net.syncthing.repository.android.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

object Migrations {
    val toV2 = object: Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE file_info ADD COLUMN symlink_target TEXT NOT NULL DEFAULT \"\"")
        }
    }

    val migrations = arrayOf(toV2)
}
