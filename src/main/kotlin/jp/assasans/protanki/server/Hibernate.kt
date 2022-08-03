package jp.assasans.protanki.server

import jakarta.persistence.EntityManager
import jakarta.persistence.Persistence

object HibernateUtils {
  private val entityManagerFactory = Persistence.createEntityManagerFactory("jp.assasans.protanki.server")

  fun createEntityManager(): EntityManager = entityManagerFactory.createEntityManager()
  fun close() = entityManagerFactory.close()
}
