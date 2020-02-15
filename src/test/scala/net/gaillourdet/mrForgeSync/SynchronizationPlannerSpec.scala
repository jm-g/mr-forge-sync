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

import java.io.File
import java.net.URI
import java.nio.file.Path

import org.scalatest.{MustMatchers, WordSpec}


class SynchronizationPlannerSpec
  extends WordSpec
    with MustMatchers
{

  def path(path: String): Path = new File(path).toPath

  "SynchronizationPlanner" should {
    "synchronize one lonely remote project to an empty directory" in {
      val remoteProjects = Seq(
        Project("project",
          path("group/project"),
          Some("4711"),
          Seq(URI.create("http://gitlab.com/group/project"))))

      val localProjects = Seq()

      val result: Seq[Task] = new SynchronizationPlanner(remoteProjects, localProjects).tasks

      result must have size (1)
      result.head must be (CloneTask(remoteProjects.head))
    }

    "synchronize nothing for an already cloned project" in {
      val project = Project("project",
        path("group/project"),
        Some("4711"),
        Seq(URI.create("http://gitlab.com/group/project")))
      val remoteProjects = Seq(project)

      val localProjects = Seq(project)

      val result: Seq[Task] = new SynchronizationPlanner(remoteProjects, localProjects).tasks

      result must contain theSameElementsAs Seq(NoOpTask(project, project))
    }

    "synchronize nothing for an already cloned project without local id but with matching url" in {
      val project = Project("project",
        path("group/project"),
        Some("4711"),
        Seq(URI.create("http://gitlab.com/group/project")))
      val remoteProjects = Seq(project)

      val localProjects = Seq(project.copy(id = None))

      val result: Seq[Task] = new SynchronizationPlanner(remoteProjects, localProjects).tasks

      result must contain theSameElementsAs Seq(StoreIdTask(project, localProjects.head))
    }

    "ask what to do for a local and remote project with the same path and no other attributes" in {
      val project = Project("project",
        path("group/project"),
        Some("4711"),
        Seq(URI.create("http://gitlab.com/group/project")))
      val remoteProjects = Seq(project)

      val localProjects = Seq(project.copy(id = None, uris = Seq()))

      val result: Seq[Task] = new SynchronizationPlanner(remoteProjects, localProjects).tasks

      result must contain theSameElementsAs Seq(AskIsCloneTask(project, localProjects.head))
    }

    "move a local project to the remote path if identified by id" in {
      val project = Project("project",
        path("group/project"),
        Some("4711"),
        Seq(URI.create("http://gitlab.com/group/project")))
      val remoteProjects = Seq(project)

      val localProjects = Seq(project.copy(path = path("group/otherProject"), uris = Seq()))

      val result: Seq[Task] = new SynchronizationPlanner(remoteProjects, localProjects).tasks

      result must have size (1)
      result.head must be (MoveTask(project, localProjects.head))
    }

    "move a local project to the remote path if identified by url" in {
      val project = Project("project",
        path("group/project"),
        Some("4711"),
        Seq(URI.create("http://gitlab.com/group/project")))
      val remoteProjects = Seq(project)

      val localProjects = Seq(project.copy(path = path("group/otherProject"), id = None))

      val planner = new SynchronizationPlanner(remoteProjects, localProjects)
      val result = planner.tasks

      result must have size (1)
      result.head must be (MoveTask(project, localProjects.head))
    }
  }
}
