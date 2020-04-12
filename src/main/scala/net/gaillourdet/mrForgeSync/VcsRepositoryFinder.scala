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
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitOption, Files, Path}
import java.util.function.BiPredicate

import scala.collection.JavaConverters._
import scala.sys.process.Process


class VcsRepositoryFinder(
  rootDir: Path,
  metaDataRepository: VcsRepositoryMetaDataRepository
) {

  private def terminalAbort(errorMessage: String): Nothing = {
    Console.err.println(errorMessage)
    System.exit(1)
    throw new IllegalStateException()
  }

  val pattern = "^remote\\.[^.]*\\.url (.*)$".r

  private val matcher = new BiPredicate[Path, BasicFileAttributes] {

    override def test(t: Path, u: BasicFileAttributes): Boolean = {
      Files.isDirectory(t) /* symbolic links are followed */ &&
        Files.exists(t.resolve(".git"))
    }

  }

  def find(): Seq[Project] = {
    Files.find(rootDir, Int.MaxValue, matcher, FileVisitOption.FOLLOW_LINKS)
      .iterator()
      .asScala
      .map(rootDir.relativize)
      .map(p => { println(s"found local repository ${p.toString}"); p})
      .map(loadProject)
      .toSeq
  }

  def getUris(repository: Path): Seq[URI] = {

    val command = Seq("git", "config", "--get-regexp", "remote\\.[^.]*\\.url")
    try {
      Process(
        command,
        Some(rootDir.resolve(repository).toFile))
        .!!
        .linesIterator
        .toSeq
        .map {
          case pattern(uri) => if (uri.startsWith("git@")) URI.create("ssh://" + uri) else URI.create(uri)
          case output => terminalAbort(s"unexpected output `$output` of `${command.mkString(" ")}`")
        }
    } catch {
      case e: RuntimeException if e.getMessage == "Nonzero exit value: 1" => Seq.empty
    }
  }

  def loadProject(path: Path) = {
    Project(path.getFileName.toString, path, metaDataRepository.fetch(path, "gitlabProjectId"), getUris(path), false)
  }
}

case class VcsRepository(path: Path, projectId: String)
