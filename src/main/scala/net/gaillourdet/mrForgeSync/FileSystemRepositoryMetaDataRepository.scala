/*
 * Copyright 2020 Jean-Marie Gaillourdet
 *
 * This file is part of mr-forge-sync.
 *
 * mr-forge-sync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mr-forge-sync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mr-forge-sync.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.gaillourdet.mrForgeSync

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.attribute.UserDefinedFileAttributeView
import java.nio.file.{Files, Path}


class FileSystemRepositoryMetaDataRepository extends VcsRepositoryMetaDataRepository {

  override def store(repository: Path, key: String, value: String): Unit = {
    val attributesView = Files.getFileAttributeView(repository, classOf[UserDefinedFileAttributeView])
    attributesView.write(key, Charset.defaultCharset().encode(value))
  }

  override def fetch(repository: Path, key: String): Option[String] = {
    val attributesView = Files.getFileAttributeView(repository, classOf[UserDefinedFileAttributeView])
    val valueSize = attributesView.size(key)

    // Restrict the necessary buffer size to avoid resource exhaustion
    require(valueSize < 16 * 1024, "extended attribute is to large")

    val buffer = ByteBuffer.allocate(valueSize)
    val readBytes = attributesView.read(key, buffer)
    if (readBytes > 0 && readBytes <= valueSize) {
      buffer.flip()
      val readString = Charset.defaultCharset().decode(buffer)
      Some(readString.toString)
    } else None
  }

}
