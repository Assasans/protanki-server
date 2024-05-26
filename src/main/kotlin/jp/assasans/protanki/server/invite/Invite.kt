package jp.assasans.protanki.server.invite

import jakarta.persistence.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jp.assasans.protanki.server.HibernateUtils

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

  @Column(nullable = false, unique = true, length = 64) var code: String,
  @Column(nullable = true, length = 64) var username: String?
) {
  suspend fun updateUsername() {
    HibernateUtils.createEntityManager().let { entityManager ->
      entityManager.transaction.begin()

      withContext(Dispatchers.IO) {
        entityManager
          .createQuery("UPDATE Invite SET username = :username WHERE id = :id")
          .setParameter("id", id)
          .setParameter("username", username)
          .executeUpdate()
      }

      entityManager.transaction.commit()
      entityManager.close()
    }
  }
}
