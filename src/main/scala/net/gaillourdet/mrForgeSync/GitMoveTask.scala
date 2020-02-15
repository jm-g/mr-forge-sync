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

import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import scala.compat.java8.StreamConverters._
import scala.jdk.CollectionConverters._
import scala.sys.process.Process


class GitMoveTask(
  rootDir: Path
) extends TaskExecutor[MoveTask] {

  override def execute(task: MoveTask): Unit = {
    new Execution(task).execute()
  }

  class Execution(task: MoveTask) {

    def execute(): Unit = {
      val confirmed = ConsoleTools.askForConfirmation(
        s"""Repository ${task.local.path}
           |should be at ${task.remote.path}
           |Please close all IDEs, build tools, etc. using that project and confirm the
           |move with y or n: """
          .stripMargin)
      if (confirmed) {
        moveRepository(task.local.path, task.remote.path)
      }
    }

    def moveRepository(oldPath: Path, newPath: Path): Unit = {
      Files.createDirectories(newPath.getParent)
      Files.move(rootDir.resolve(oldPath), rootDir.resolve(newPath))
    }

    def moveMrProject(): Unit = {
      val project = task.remote

      // name of directory that will contain the project, could also be derived from project.name
      val targetDirectory = project.path.getFileName

      changeSectionHeaderOfMrConfig()

      Process(
        Seq("mr", "config", project.path.toString, s"""checkout=git clone ${project.sshUrl} $targetDirectory"""),
        rootDir.toFile).!!
    }

    private lazy val SectionHeader = "^\\[([^]]*)\\]".r

    def changeSectionHeaderOfMrConfig(): Unit = {
      val mrConfigPath = rootDir.resolve(".mrconfig")
      val lines: LazyList[String] = Files.lines(mrConfigPath, Charset.defaultCharset()).toScala[LazyList](LazyList)

      val oldSectionTitle = task.local.path.toString
      val modifiedLines = lines.map {
        case SectionHeader(`oldSectionTitle`) => s"[${task.remote.path}]"
        case l => l
      }

      Files.write(mrConfigPath, modifiedLines.asJava, Charset.defaultCharset())
    }
  }
}
