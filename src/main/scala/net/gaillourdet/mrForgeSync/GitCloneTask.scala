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

import java.nio.file.{Files, Path}

import scala.sys.process.Process

class GitCloneTask(
  rootDir: Path,
  metaDataRepository: GitRepositoryMetaDataRepository
) extends TaskExecutor[CloneTask] {

  override def execute(task: CloneTask): Unit = {
    new Execution(task).execute()
  }

  class Execution(task: CloneTask) {
    private def project = task.remote

    // name of directory that will contain the project, could also be derived from project.name
    val targetDirectory = project.path.getFileName

    def execute(): Unit = {
      registerProject()
      checkoutProject()
      storeMetaData()
    }

    def registerProject(): Unit = {
      Process(
        Seq("mr", "config", project.path.toString, s"""checkout=git clone ${project.sshUrl} $targetDirectory"""),
        rootDir.toFile).!!
    }

    def checkoutProject(): Unit = {
      Console.println(s"checking out ${project.path}")
      val targetDirectoryParent = rootDir.resolve(project.path).getParent
      Files.createDirectories(targetDirectoryParent)
      val exitCode = Process(
        Seq("git", "clone", project.sshUrl.toString, targetDirectory.toString),
        targetDirectoryParent.toFile).!
      if (exitCode != 0) {
        System.exit(1)
      }
    }

    def storeMetaData(): Unit = {
      project.id.foreach(projectId =>
        metaDataRepository.store(project.path, "gitlabProjectId", projectId)
      )
    }
  }
}
