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

import java.io.IOException
import java.net.URI
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util

import scala.sys.process.Process
import scala.util.matching.Regex


class VcsRepositoryFinder(
  rootDir: Path,
  metaDataRepository: VcsRepositoryMetaDataRepository
) {

  //noinspection SameParameterValue
  private def terminalAbort(errorMessage: String): Nothing = {
    Console.err.println(errorMessage)
    System.exit(1)
    throw new IllegalStateException()
  }

  private val pattern: Regex = "^remote\\.[^.]*\\.url (.*)$".r

  def find(): Seq[Project] = {
    val found = collection.mutable.ListBuffer.empty[Project]

    Files.walkFileTree(rootDir, util.EnumSet.of(FileVisitOption.FOLLOW_LINKS), Int.MaxValue, new FileVisitor[Path] {

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (Files.exists(dir.resolve(".git"))) {
          val relativeDir = rootDir.relativize(dir)
          found += loadProject(relativeDir)
          println(s"found local repository $relativeDir")
          FileVisitResult.SKIP_SUBTREE
        } else if (Files.exists(dir.resolve(".svn")) || Files.exists(dir.resolve(".hg"))) {
          FileVisitResult.SKIP_SUBTREE
        } else {
          FileVisitResult.CONTINUE
        }
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        exc match {
          case _: AccessDeniedException =>
            println(s"could not traverse $file")
            FileVisitResult.SKIP_SUBTREE
          case  _ => throw exc
        }
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        FileVisitResult.CONTINUE
      }
    })

    found.to(List)
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

  def loadProject(path: Path): Project = {
    Project(
      path.getFileName.toString,
      path,
      metaDataRepository.fetch(path, "gitlabProjectId"),
      getUris(path),
      archived = false)
  }
}

case class VcsRepository(path: Path, projectId: String)
