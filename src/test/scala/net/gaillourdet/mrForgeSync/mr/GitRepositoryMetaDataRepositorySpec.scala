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

import java.io.File
import java.nio.file.Files

import net.gaillourdet.mrForgeSync.GitRepositoryMetaDataRepository
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

import scala.sys.process.Process

class GitRepositoryMetaDataRepositorySpec
  extends WordSpec with MustMatchers with OptionValues
  {

    "GitRepositoryMetaDataRepositorySpec" should {
      "be able to store a key value pair" in {
        val baseDir = Files.createTempDirectory("mr-forge-sync-test-")

        val testRepository = new File("testRepository").toPath

        val testDirectory = baseDir.resolve(testRepository)
        Files.createDirectory(testDirectory)
        Process("git init", testDirectory.toFile).!!

        val testee = new GitRepositoryMetaDataRepository(baseDir)

        testee.store(testRepository, "testkey", "somevalue")
      }

      "be able to fetch a stored key value pair" in {
        val baseDir = Files.createTempDirectory("mr-forge-sync-test-")

        val testRepository = new File("testRepository").toPath

        val testDirectory = baseDir.resolve(testRepository)
        Files.createDirectory(testDirectory)
        Process("git init", testDirectory.toFile).!!

        val testee = new GitRepositoryMetaDataRepository(baseDir)

        testee.store(testRepository, "test-key", "somevalue")

        val result = testee.fetch(testRepository, "test-key")

        result.value must be ("somevalue")
      }
    }


  }
