package jp.assasans.protanki.server.exceptions

class UnknownCommandException : Exception {
  val category: String
  val command: String

  constructor(category: String, command: String) : super() {
    this.category = category
    this.command = command
  }

  constructor(category: String, command: String, message: String) : super(message) {
    this.category = category
    this.command = command
  }

  constructor(category: String, command: String, message: String, cause: Throwable) : super(message, cause) {
    this.category = category
    this.command = command
  }

  constructor(category: String, command: String, cause: Throwable) : super(cause) {
    this.category = category
    this.command = command
  }
}
