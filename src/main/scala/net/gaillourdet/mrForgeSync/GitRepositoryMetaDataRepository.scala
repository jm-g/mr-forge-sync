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

import java.nio.file.Path

import scala.sys.process._
class GitRepositoryMetaDataRepository(rootDir: Path) extends VcsRepositoryMetaDataRepository {

  override def store(repository: Path, key: String, value: String): Unit = {
    val exitCode = Process(
      s"""git config --local --add mr-forge-sync.$key $value""",
      rootDir.resolve(repository).toFile).!

    if (exitCode != 0) {
      throw new RuntimeException(s"Failed to store ($key, $value) with exit code $exitCode ")
    }
  }

  override def fetch(repository: Path, key: String): Option[String] = {
    val buffer = new StringBuffer()
    val exitCode = Process(
      s"""git config --local --get mr-forge-sync.$key""",
      rootDir.resolve(repository).toFile) ! ProcessLogger(buffer append _)
    if (exitCode == 0) Some(buffer.toString) else None
  }

}
