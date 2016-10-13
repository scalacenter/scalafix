package scalafix.config

import java.io.File

case class ProjectFiles(
    targetFiles: Seq[File] = Nil,
    files: Seq[File] = Nil
)
