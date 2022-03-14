package jp.assasans.protanki.server

import java.nio.file.Path
import java.nio.file.Paths

interface IResourceManager {
  fun get(path: String): Path
}

class ResourceManager : IResourceManager {
  private val resourceDirectory: Path = Paths.get("data")

  override fun get(path: String): Path {
    return resourceDirectory.resolve(path)
  }
}
