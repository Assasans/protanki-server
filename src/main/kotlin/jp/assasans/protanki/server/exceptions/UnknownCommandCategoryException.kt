package jp.assasans.protanki.server.exceptions

class UnknownCommandCategoryException : Exception {
  val category: String

  constructor(category: String) : super() {
    this.category = category
  }

  constructor(category: String, message: String) : super(message) {
    this.category = category
  }

  constructor(category: String, message: String, cause: Throwable) : super(message, cause) {
    this.category = category
  }

  constructor(category: String, cause: Throwable) : super(cause) {
    this.category = category
  }
}
