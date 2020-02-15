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

class GitAskIsCloneTask(
  rootDir: Path,
  metaDataRepository: GitRepositoryMetaDataRepository
) extends TaskExecutor[AskIsCloneTask]
{

  override def execute(task: AskIsCloneTask): Unit = {
    val confirmation = ConsoleTools.askForConfirmation(
      s"""Is the local repository at ${rootDir.resolve(task.local.path)}
         |a clone of the remote repository, available at the following urls:
         |${task.remote.uris.mkString("- ", "\n - ", "")}
         |""".stripMargin
    )
    if (confirmation) {
      metaDataRepository.store(
        task.local.path,
        VcsRepositoryMetaDataRepository.PROJECT_ID,
        task.remote.id.get)
    }
  }

}
