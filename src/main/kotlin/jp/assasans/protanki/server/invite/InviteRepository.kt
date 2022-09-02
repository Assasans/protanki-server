package jp.assasans.protanki.server.invite

import jakarta.persistence.EntityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.extensions.putIfAbsent

interface IInviteRepository {
  suspend fun getInvite(id: Int): Invite?
  suspend fun getInvite(code: String): Invite?
  suspend fun getInvites(): List<Invite>
  suspend fun getInviteCount(): Long

  suspend fun createInvite(code: String): Invite?
  suspend fun deleteInvite(code: String): Boolean
}

class InviteRepository : IInviteRepository {
  private val _entityManagers = ThreadLocal<EntityManager>()

  private val entityManager: EntityManager
    get() = _entityManagers.putIfAbsent { HibernateUtils.createEntityManager() }

  override suspend fun getInvite(id: Int): Invite? = withContext(Dispatchers.IO) {
    entityManager.find(Invite::class.java, id)
  }

  override suspend fun getInvite(code: String): Invite? = withContext(Dispatchers.IO) {
    entityManager
      .createQuery("FROM Invite WHERE code = :code", Invite::class.java)
      .setParameter("code", code)
      .resultList
      .singleOrNull()
  }

  override suspend fun getInvites(): List<Invite> = withContext(Dispatchers.IO) {
    entityManager
      .createQuery("FROM Invite", Invite::class.java)
      .resultList
      .toList()
  }

  override suspend fun getInviteCount(): Long = withContext(Dispatchers.IO) {
    entityManager
      .createQuery("SELECT COUNT(1) FROM Invite", Long::class.java)
      .singleResult
  }

  override suspend fun createInvite(code: String): Invite? = withContext(Dispatchers.IO) {
    getInvite(code)?.let { return@withContext null }

    val invite = Invite(
      id = 0,
      code = code,
      username = null
    )

    entityManager.transaction.begin()
    entityManager.persist(invite)
    entityManager.transaction.commit()

    invite
  }

  override suspend fun deleteInvite(code: String): Boolean = withContext(Dispatchers.IO) {
    entityManager.transaction.begin()
    val updates = entityManager
      .createQuery("DELETE FROM Invite WHERE code = :code")
      .setParameter("code", code)
      .executeUpdate()
    entityManager.transaction.commit()

    updates > 0
  }
}
