package jp.assasans.protanki.server

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

interface IResourceManager {
  fun get(path: String): Path
}

class ResourceManager : IResourceManager {
  private val resourceDirectory: Path

  init {
    var directory = Paths.get("data")
    if(!directory.exists()) directory = Paths.get("../data") // Gradle distribution / jar
    if(!directory.exists()) directory = Paths.get("src/main/resources/data") // Started from IntelliJ IDEA, default working directory
    if(!directory.exists()) directory = Paths.get("../src/main/resources/data") // Started from IntelliJ IDEA, 'out' working directory
    if(!directory.exists()) throw Exception("Cannot find runtime resources directory")

    resourceDirectory = directory
  }

  override fun get(path: String): Path {
    return resourceDirectory.resolve(path)
  }
}
