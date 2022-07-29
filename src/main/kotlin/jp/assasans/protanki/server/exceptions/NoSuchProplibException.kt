package jp.assasans.protanki.server.exceptions

class NoSuchProplibException(
  val proplib: String,
  val map: String?,
  cause: Throwable? = null
) : Exception(getMessage(proplib, map), cause) {
  private companion object {
    fun getMessage(proplib: String, map: String?): String = buildString {
      append("No such proplib: $proplib")
      if(map != null) append(", map: $map")
    }
  }
}
