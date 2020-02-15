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

package net.gaillourdet.mrForgeSync.mr

import java.nio.file.Files

import net.gaillourdet.mrForgeSync.FileSystemRepositoryMetaDataRepository
import org.scalatest.{Ignore, MustMatchers, WordSpec}

@Ignore
class FileSystemRepositoryMetaDataRepositorySpec
  extends WordSpec with MustMatchers
{

  "FileSystemRepositoryMetaDataRepositorySpec" should {
    "be able to store a key value pair" in {
      val baseDir = Files.createTempDirectory("mr-forge-sync-test-")

      val testDirectory = baseDir.resolve("testRepository")
      Files.createFile(testDirectory)

      val testee = new FileSystemRepositoryMetaDataRepository()

      testee.store(testDirectory, "testkey", "some value")
    }

    "be able to fetch a stored key value pair" in {
      val baseDir = Files.createTempDirectory("mr-forge-sync-test-")

      val testDirectory = baseDir.resolve("testRepository")
      Files.createFile(testDirectory)

      val testee = new FileSystemRepositoryMetaDataRepository()

      testee.store(testDirectory, "test-key", "some value")

      val result = testee.fetch(testDirectory, "test-key")

      result must be ("some value")
    }
  }


}
