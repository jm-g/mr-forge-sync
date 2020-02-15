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
import java.util.regex.Pattern

import scala.concurrent.Await
import scala.concurrent.duration._

class Synchronizer(
  forgeRepository: ForgeRepository,
  rootDir: Path,
  includePattern: Pattern,
  metaDataRepository: VcsRepositoryMetaDataRepository,
  vcsRepositoryFinder: VcsRepositoryFinder,
  gitCloneTask: GitCloneTask,
  gitMoveTask: GitMoveTask,
  gitStoreIdTask: GitStoreIdTask,
  gitAskIsCloneTask: GitAskIsCloneTask
) {


  // TODO: path: normalize and resolve symlinks
  // TODO: trim project id
  // TODO: unicode normalization

  def synchronize(): Unit = {
    val projects: Seq[Project] = Await.result(forgeRepository.fetchAllProjects(), 1.minute)
    val includedProjects: Seq[Project] = projects.filter(project => includePattern.matcher(project.path.toString).matches())

    // only git repositories
    val localProjects: Seq[Project] = vcsRepositoryFinder.find()


    val planner = new SynchronizationPlanner(includedProjects, localProjects)

    Console.println(planner.ignoredLocalProjects
      .view
      .map(_.path)
      .mkString("Ignoring the following local projects: \n - ", "\n - ", ""))

    for (task <- planner.tasks) {
      task match {
        case t@CloneTask(_) => gitCloneTask.execute(t)
        case t@MoveTask(_, _) => gitMoveTask.execute(t)
        case t@AskIsCloneTask(_, _) => gitAskIsCloneTask.execute(t)
        case t@StoreIdTask(_, _) => gitStoreIdTask.execute(t)
        case NoOpTask(_, _) => ()
      }
    }
  }
}
