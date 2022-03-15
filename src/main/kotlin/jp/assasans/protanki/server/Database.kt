package jp.assasans.protanki.server

import jp.assasans.protanki.server.client.User

interface IDatabase {
  val users: MutableList<User>
}

class Database : IDatabase {
  override val users: MutableList<User> = mutableListOf()
}
