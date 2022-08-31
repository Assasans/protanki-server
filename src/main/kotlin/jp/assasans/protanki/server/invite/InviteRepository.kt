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
}
