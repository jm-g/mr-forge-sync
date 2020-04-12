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

import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.ProjectFilter
import org.gitlab4j.{api => gl}

import scala.collection.JavaConverters._
import scala.concurrent.Future


class GitLabRepository(uri: URI, token: String) extends ForgeRepository {

  override def fetchAllProjects(): Future[Seq[Project]] = {

    val api =
      new GitLabApi(uri.toString, token)
        .withRequestResponseLogging()

    val projectFilter = new ProjectFilter().withMembership(true)
    val projects = api.getProjectApi.getProjects(projectFilter).asScala.map(toProject)

    Future.successful(projects.toSeq)
  }

  private def toProject(project: gl.models.Project): Project = {
    Project(
      project.getName,
      new File(project.getPathWithNamespace).toPath,
      Some(project.getId.toString),
      URI.create(project.getSshUrlToRepo) :: URI.create(project.getHttpUrlToRepo) :: Nil,
      project.getArchived
    )
  }
}

