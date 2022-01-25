package jp.assasans.protanki.server.client

class User(
  val id: Int,
  var username: String,
  var rank: UserRank,
  var score: Int,
  var crystals: Int
)
