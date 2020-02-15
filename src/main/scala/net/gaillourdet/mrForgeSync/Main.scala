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
import java.nio.file.{Files, Path}
import java.util.regex.PatternSyntaxException

import scopt.OParser


case class Config(
  rootDir: Path = new File(".").toPath,
  includePattern: String = ".*",
  gitlabToken: String = "",
  gitlabUrl: URI = URI.create(""),
)
object Main extends App {

  def defaultConfig = Config()

  val builder = OParser.builder[Config]
  val parser: OParser[Unit, Config] = {
    import builder._
    OParser.sequence(
      programName("mr-forge-sync"),
      head("mr-forge-sync", "0.1-SNAPSHOT"),
      arg[File]("<directory>")
        .optional()
        .text("directory corresponding to the root directory of Gitlab")
        .action((f, c) => c.copy(rootDir = f.toPath))
        .validate(f => {
          val path = f.toPath
          if (!Files.exists(path)) Left(s"${f.toString} is not a directory")
          else if (!Files.isDirectory(path)) Left(s"$path is not a directory")
          else {
            val mrConfig = path.resolve(".mrconfig")
            if (!Files.isWritable(mrConfig)) Left(s"$mrConfig does not exist or is not writable")
            else Right(())
          }
        }),
      opt[URI]('u', "gitlab-url")
        .valueName("<URL>")
        .required()
        .text("url of the gitlab instance")
        .action((uri, c) => c.copy(gitlabUrl = uri)),
      opt[String]('t', "access-token")
        .valueName("<TOKEN>")
        .required()
        .text("a Gitlab access token")
        .action((token, c) => c.copy(gitlabToken = token)),
      opt[String]('o', "include-only")
        .valueName("<REGEXP>")
        .optional()
        .text("sync only projects whose server path matches")
        .action((pattern, c) => c.copy(includePattern = pattern))
        .validate(pattern => try {
          pattern.r
          Right(())
        } catch {
          case e: PatternSyntaxException => Left(e.getMessage)
        }),
      note(
      """mr-forge-sync synchronizes available projects in a Gitlab instance with the checked out projects
        |in a local directory managed by mr[1].
        |
        |mr-forge-sync  Copyright © 2020  Jean-Marie Gaillourdet
        |This program comes with ABSOLUTELY NO WARRANTY; for details see the LICENSE file.
        |This is free software.
        |
        |[1]: https://myrepos.branchable.com/
        |""".stripMargin)
    )
  }

  OParser.parse(parser, args, defaultConfig) match {
    case None =>
      System.exit(255)
    case Some(config) =>
      if (!Files.isDirectory(config.rootDir)) {

      }

      val gitLabRepository = new GitLabRepository(config.gitlabUrl, config.gitlabToken)

      val metaDataRepository = new GitRepositoryMetaDataRepository(config.rootDir)

      new Synchronizer(
        gitLabRepository,
        config.rootDir,
        config.includePattern.r.pattern  ,
        metaDataRepository,
        new VcsRepositoryFinder(config.rootDir, metaDataRepository),
        new GitCloneTask(config.rootDir, metaDataRepository),
        new GitMoveTask(config.rootDir),
        new GitStoreIdTask(metaDataRepository),
        new GitAskIsCloneTask(config.rootDir, metaDataRepository)
      ).synchronize()
  }

}
