/* 
 * Copyright (C) 2016 Davide Imbriaco
 * Copyright (C) 2018 Jonas Lochmann
 *
 * This Java file is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.syncthing.java.core.beans

import net.syncthing.java.core.utils.PathUtils

sealed class FileInfo {
    abstract val folder: String
    abstract val path: String
    abstract val lastModified: FileLastModifiedTime
    abstract val lastModifiedBy: Long
    abstract val isDeleted: Boolean
    abstract val versionList: List<FileVersion>
    abstract val fileName: String
    abstract val parent: String
    abstract val permissions: Int
    abstract val noPermissions: Boolean
    abstract val sequence: Long

    abstract fun withSequenceNumber(sequence: Long): FileInfo
}

data class DirectoryFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: FileLastModifiedTime,
        override val lastModifiedBy: Long,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>,
        override val permissions: Int,
        override val noPermissions: Boolean,
        override val sequence: Long
): FileInfo() {
    override val fileName = PathUtils.getFileName(path)
    override val parent = if (PathUtils.isRoot(path))
        PathUtils.ROOT_PATH
    else
        PathUtils.getParentPath(path)

    override fun withSequenceNumber(sequence: Long): DirectoryFileInfo = copy(sequence = sequence)

    companion object {
        fun createFolderRootDummyInfo(folder: String) = DirectoryFileInfo(
                folder = folder,
                path = PathUtils.ROOT_PATH,
                isDeleted = false,
                versionList = emptyList(),
                lastModified = FileLastModifiedTime.empty,
                lastModifiedBy = 0,
                noPermissions = true,
                permissions = "066".toInt(8),
                sequence = 0
        )
    }
}

data class FileFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: FileLastModifiedTime,
        override val lastModifiedBy: Long,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>,
        override val permissions: Int,
        override val noPermissions: Boolean,
        val hash: String,
        val size: Long,
        val invalid: Boolean,
        override val sequence: Long
): FileInfo() {
    companion object {
        fun checkBlocks(fileInfo: FileFileInfo, fileBlocks: FileBlocks) {
            assert(fileBlocks.folder == fileInfo.folder) {"file info folder not match file block folder"}
            assert(fileBlocks.path == fileInfo.path) {"file info path does not match file block path"}
            assert(fileBlocks.size == fileInfo.size) {"file info size does not match file block size"}
            assert(fileBlocks.hash == fileInfo.hash) {"file info hash does not match file block hash"}
        }
    }

    override val fileName = PathUtils.getFileName(path)
    override val parent = PathUtils.getParentPath(path)

    override fun withSequenceNumber(sequence: Long): FileFileInfo = copy(sequence = sequence)
}

data class SymlinkFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: FileLastModifiedTime,
        override val lastModifiedBy: Long,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>,
        override val permissions: Int,
        override val noPermissions: Boolean,
        val symlinkTarget: String,
        override val sequence: Long
): FileInfo() {
    override val fileName = PathUtils.getFileName(path)
    override val parent = PathUtils.getParentPath(path)

    override fun withSequenceNumber(sequence: Long): SymlinkFileInfo = copy(sequence = sequence)
}

data class FileVersion(val id: Long, val value: Long)

data class FileLastModifiedTime(
        val seconds: Long,
        val nanoSeconds: Int
): Comparable<FileLastModifiedTime> {
    val roundedMilliseconds = seconds * 1000 + nanoSeconds / 1000000

    override fun compareTo(other: FileLastModifiedTime) = if (seconds > other.seconds) {
        1
    } else if (seconds < other.seconds) {
        -1
    } else {
        if (nanoSeconds > other.nanoSeconds) {
            1
        } else if (nanoSeconds < other.nanoSeconds) {
            -1
        } else {
            0
        }
    }

    companion object {
        val empty = FileLastModifiedTime(0, 0)
    }
}
