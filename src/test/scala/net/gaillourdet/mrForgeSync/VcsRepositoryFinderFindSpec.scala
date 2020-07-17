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

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Path}
import java.util.Comparator
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{BeforeAndAfterAll, LoneElement, MustMatchers, WordSpec}

import scala.sys.process.Process

//noinspection TypeAnnotation
class VcsRepositoryFinderFindSpec
  extends WordSpec
    with MustMatchers
    with LoneElement
    with BeforeAndAfterAll
{
  lazy val testBaseDirectory = Files.createTempDirectory("VcsRepositoryFinderFindSpec-")

  var repositoryCounter = new AtomicInteger(0)

  override protected def beforeAll(): Unit = {
    // force creation of test directory
    testBaseDirectory

    testBaseDirectory.toFile.deleteOnExit()
  }

  override protected def afterAll(): Unit = {
    Files
      .walk(testBaseDirectory)
      .sorted(Comparator.reverseOrder())
      .forEach(Files.delete _)

    super.afterAll()
  }


  def withDirectory(
    body: Path => Unit
  ): Unit = {
    val path = testBaseDirectory.resolve(s"test_case_${repositoryCounter.incrementAndGet()}")
    Files.createDirectory(path)
    try {
      body(path)
    } finally {
      Files
        .walk(path)
        .sorted(Comparator.reverseOrder())
        .forEach(Files.delete _)
    }
  }

  def createGitRepository(base: Path, path: String): Unit = {
    val fullPath = base.resolve(path)
    Files.createDirectories(fullPath)
    Process(Seq("git", "init"), fullPath.toFile).!!
  }

  def createDirectory(base: Path, path: String): Unit = {
    val fullPath = base.resolve(path)
    Files.createDirectories(fullPath)
  }


  def addRemote(repository: Path, remote: String, url: String) = {
    Process(Seq("git", "remote", "add", remote, url), testBaseDirectory.resolve(repository).toFile).!!
  }

  "VcsRepositoryFinder.find" should {
    "return a sole git repository" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "testRepo")
      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.loneElement must have ('path (Path.of("testRepo")))
    }

    "return two git repositories" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "testRepo1")
      createGitRepository(testBaseDirectory, "testRepo2")
      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.map(_.path.getFileName.toString) must contain allOf ("testRepo1", "testRepo2")
    }

    "return two git repositories and skip decoys" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "testRepo1")
      createGitRepository(testBaseDirectory, "testRepo2")
      createDirectory(testBaseDirectory, "decoy1")
      createDirectory(testBaseDirectory, "decoy2")

      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.map(_.path.getFileName.toString) must contain allOf ("testRepo1", "testRepo2")
    }

    "return two git repositories and when contained in decoys" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "decoy1/testRepo1")
      createGitRepository(testBaseDirectory, "decoy2/testRepo2")

      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.map(_.path.getFileName.toString) must contain allOf ("testRepo1", "testRepo2")
    }

    "skip unreadable directories" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "decoy1/testRepo1")
      Files.setPosixFilePermissions(testBaseDirectory.resolve("decoy1"), java.util.EnumSet.noneOf(classOf[PosixFilePermission]))
      createGitRepository(testBaseDirectory, "decoy2/testRepo2")

      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.map(_.path.getFileName.toString) must not contain "testRepo1"
      found.map(_.path.getFileName.toString) must contain ("testRepo2")

      Files.setPosixFilePermissions(testBaseDirectory.resolve("decoy1"), java.util.EnumSet.allOf(classOf[PosixFilePermission]))
    }

    "skip git repos in git repos" in withDirectory { testBaseDirectory =>
      createGitRepository(testBaseDirectory, "testRepo1")
      createGitRepository(testBaseDirectory, "testRepo1/testRepo2")

      val testee = new VcsRepositoryFinder(testBaseDirectory, new GitRepositoryMetaDataRepository(testBaseDirectory))

      val found = testee.find()

      found.map(_.path.getFileName.toString) must contain ("testRepo1")
      found.map(_.path.getFileName.toString) must not contain "testRepo2"
    }
  }
}
