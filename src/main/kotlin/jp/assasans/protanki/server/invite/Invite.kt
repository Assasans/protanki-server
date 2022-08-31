package jp.assasans.protanki.server.invite

import jakarta.persistence.*

@Entity
@Table(
  name = "invites",
  indexes = [
    Index(name = "idx_invites_code", columnList = "code")
  ]
)
data class Invite(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(nullable = false, unique = true, length = 64) var code: String
)
