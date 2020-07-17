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

import java.net.URI
import java.nio.file.{Files, Path}
import java.util.Comparator
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{BeforeAndAfterAll, LoneElement, MustMatchers, WordSpec}

import scala.sys.process.Process

//noinspection TypeAnnotation
class VcsRepositoryFinderGetUrisSpec
  extends WordSpec
    with MustMatchers
    with LoneElement
    with BeforeAndAfterAll
{
  lazy val testBaseDirectory = Files.createTempDirectory("VcsRepositoryFinderGetUrisSpec-")

  lazy val repositoryCounter = new AtomicInteger(0)

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

  def withGitRepository(body : Path => Unit): Unit = {
    val repoPath: Path = Path.of(s"repo_${repositoryCounter.getAndIncrement()}")
    val path = testBaseDirectory.resolve(repoPath)
    Files.createDirectory(path)
    try {
      Process(Seq("git", "init"), path.toFile).!!
      body(path)
    } finally {
      Files
        .walk(path)
        .sorted(Comparator.reverseOrder())
        .forEach(Files.delete _)
    }
  }

  def addRemote(repository: Path, remote: String, url: String) = {
    Process(Seq("git", "remote", "add", remote, url), testBaseDirectory.resolve(repository).toFile).!!
  }

  "VcsRepositoryFinder.getUris" should {
    "work without remotes" in withGitRepository { repository =>
      val testee = new VcsRepositoryFinder(testBaseDirectory, null)

      val uris = testee.getUris(testBaseDirectory.relativize(repository))

      uris must have length 0
    }

    "work with one https remote" in withGitRepository { repository =>
      val url = "http://gitlab.com/some/thing"
      addRemote(repository, "origin", url)
      val testee = new VcsRepositoryFinder(testBaseDirectory, null)

      val uris: Seq[URI] = testee.getUris(testBaseDirectory.relativize(repository))

      uris must contain theSameElementsAs Seq(URI.create(url))
    }

    "work with one ssh remote" in withGitRepository { repository =>
      val url = "ssh://user@gitlab.com:/some/thing"
      addRemote(repository, "origin", url)
      val testee = new VcsRepositoryFinder(testBaseDirectory, null)

      val uris: Seq[URI] = testee.getUris(testBaseDirectory.relativize(repository))

      uris must contain theSameElementsAs Seq(URI.create(url))
    }

    "work with one multiple remotes" in withGitRepository { repository =>
      val url1 = "ssh://user@gitlab.com:/some/thing"
      val url2 = "http://gitlab.com/some/thing"
      addRemote(repository, "origin1", url1)
      addRemote(repository, "origin2", url2)
      val testee = new VcsRepositoryFinder(testBaseDirectory, null)

      val uris: Seq[URI] = testee.getUris(testBaseDirectory.relativize(repository))

      uris must contain theSameElementsAs Seq(URI.create(url1),URI.create(url2))
    }
  }
}
