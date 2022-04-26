package jp.assasans.protanki.server.utils

import jp.assasans.protanki.server.ServerIdResource

class ResourceUtils private constructor() {
  companion object {
    // Client-side implementation: alternativa.utils:LoaderUtils/getResourcePath()
    fun encodeId(resource: ServerIdResource): List<String> {
      return listOf(
        (resource.id and 0xff000000 shr 24).toString(8),
        (resource.id and 0x00ff0000 shr 16).toString(8),
        (resource.id and 0x0000ff00 shr 8).toString(8),
        (resource.id and 0x000000ff shr 0).toString(8),
        resource.version.toString(8)
      )
    }

    fun decodeId(parts: List<String>): ServerIdResource {
      return ServerIdResource(
        id = (parts[0].toLong(8) shl 24) or
          (parts[1].toLong(8) shl 16) or
          (parts[2].toLong(8) shl 8) or
          (parts[3].toLong(8) shl 0),
        version = parts[4].toLong(8)
      )
    }
  }
}
