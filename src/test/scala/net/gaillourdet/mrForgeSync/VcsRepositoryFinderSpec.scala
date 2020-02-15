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
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

import scala.sys.process.Process

class VcsRepositoryFinderSpec
  extends WordSpec
    with MustMatchers
    with BeforeAndAfterAll
{
  lazy val testBaseDirectory = Files.createTempDirectory("VcsRepositoryFinderSpec-")

  var repositoryCounter = new AtomicInteger(0)

  override protected def beforeAll(): Unit = {
    // force creation of test directory
    testBaseDirectory

    testBaseDirectory.toFile.deleteOnExit()
  }

  def withGitRepository(body : Path => Unit): Unit = {
    val repositoryIndex = repositoryCounter.getAndIncrement()
    val repoPath = Files.createDirectory(testBaseDirectory.resolve(s"repo_$repositoryIndex"))
    try {
      Process(Seq("git", "init"), testBaseDirectory.resolve(repoPath).toFile).!!
      body(repoPath)
    } finally {
      // TODO: implement cleanup
//      Files.delete(repoPath)
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
