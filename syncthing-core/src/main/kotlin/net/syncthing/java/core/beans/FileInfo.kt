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
import java.util.*

sealed class FileInfo {
    abstract val folder: String
    abstract val path: String
    abstract val lastModified: Date
    abstract val isDeleted: Boolean
    abstract val versionList: List<FileVersion>
    abstract val fileName: String
    abstract val parent: String
}

data class DirectoryFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: Date,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>
): FileInfo() {
    override val fileName = PathUtils.getFileName(path)
    override val parent = if (PathUtils.isRoot(path))
        PathUtils.ROOT_PATH
    else
        PathUtils.getParentPath(path)
}

data class FileFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: Date,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>,
        val hash: String,
        val size: Long
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
}

data class SymlinkFileInfo(
        override val folder: String,
        override val path: String,
        override val lastModified: Date,
        override val isDeleted: Boolean,
        override val versionList: List<FileVersion>,
        val symlinkTarget: String
): FileInfo() {
    override val fileName = PathUtils.getFileName(path)
    override val parent = PathUtils.getParentPath(path)
}

data class FileVersion(val id: Long, val value: Long)
