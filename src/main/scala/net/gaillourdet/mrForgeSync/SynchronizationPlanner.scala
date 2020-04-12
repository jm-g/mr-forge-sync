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

import net.gaillourdet.mrForgeSync.AdditionalPredef.neverReached


class SynchronizationPlanner(
  remoteProjects: Seq[Project],
  localProjects: Seq[Project]
) {

  private lazy val localProjectsByUri = localProjects.view.flatMap(p => p.uris.map(uri => uri -> p)).toMap
  private lazy val localProjectsById = localProjects.view.collect { case p@Project(_, _, Some(id), _, _) => (id, p) }.toMap
  private lazy val localProjectsByPath = localProjects.view.map(p => p.path -> p).toMap

  def planTasks(remote: Project): Seq[Task] = remote match {
    case Project(_, rPath, Some(id), _, false) if localProjectsById.contains(id) =>
      localProjectsById.get(id) match {
        case l@Some(Project(_, path, _, _, _)) if rPath == path => Seq(NoOpTask(remote, l))
        case Some(l@Project(_, _, _, _, _)) => Seq(MoveTask(remote, l))
      }
    case Project(_, rPath, _, rUris, false) if rUris.exists(localProjectsByUri.contains) =>
      rUris.view.flatMap(uri => localProjectsByUri.get(uri)).headOption match {
        case Some(l@Project(_, path, _, _, _)) if rPath == path => Seq(StoreIdTask(remote, l))
        case Some(l@Project(_, _, _, _, _)) => Seq(MoveTask(remote, l))
      }
    case Project(_, rPath, _, _, false) if localProjectsByPath.contains(rPath) =>
      localProjectsByPath.get(rPath) match {
        case Some(l@Project(_, _, _, _, false)) => Seq(AskIsCloneTask(remote, l))
        case None => neverReached()
      }
    case Project(_, _, _, _, false) => Seq(CloneTask(remote))
    case Project(_, _, _, _, true) => Seq(NoOpTask(remote, None))
  }

  lazy val tasks: Seq[Task] = remoteProjects.flatMap(planTasks)
  lazy val ignoredLocalProjects: Seq[Project] = localProjects.filterNot(lp => tasks.flatMap(_.localProject).contains(lp))
}

sealed trait Task {
  def remote: Project
  def localProject: Option[Project]
}
case class CloneTask(remote: Project) extends Task {
  override def localProject: Option[Project] = None
}
case class MoveTask(remote: Project, local: Project) extends Task {
  override def localProject: Option[Project] = Some(local)
}
case class AskIsCloneTask(remote: Project, local: Project) extends Task {
  override def localProject: Option[Project] = Some(local)
}
case class StoreIdTask(remote: Project, local: Project) extends Task {
  override def localProject: Option[Project] = Some(local)
}
case class NoOpTask(remote: Project, local: Option[Project]) extends Task {
  override def localProject: Option[Project] = local
}

