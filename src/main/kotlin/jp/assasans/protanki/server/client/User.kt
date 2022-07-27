package jp.assasans.protanki.server.client

import jakarta.persistence.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hibernate.annotations.Parent
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.garage.ServerGarageUserItem
import jp.assasans.protanki.server.garage.ServerGarageUserItemHull
import jp.assasans.protanki.server.garage.ServerGarageUserItemPaint
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon
import jp.assasans.protanki.server.quests.ServerDailyQuest

@Embeddable
data class UserEquipment(
  @Column(name = "equipment_hull", nullable = false) var hullId: String,
  @Column(name = "equipment_weapon", nullable = false) var weaponId: String,
  @Column(name = "equipment_paint", nullable = false) var paintId: String
) {
  @Suppress("JpaAttributeTypeInspection")
  @Parent
  lateinit var user: User // IntelliJ IDEA still shows error for this line.

  @get:Transient
  var hull: ServerGarageUserItemHull
    get() = user.items.single { item -> item.id.itemName == hullId } as ServerGarageUserItemHull
    set(value) {
      hullId = value.id.itemName
    }

  @get:Transient
  var weapon: ServerGarageUserItemWeapon
    get() = user.items.single { item -> item.id.itemName == weaponId } as ServerGarageUserItemWeapon
    set(value) {
      weaponId = value.id.itemName
    }

  @get:Transient
  var paint: ServerGarageUserItemPaint
    get() = user.items.single { item -> item.id.itemName == paintId } as ServerGarageUserItemPaint
    set(value) {
      paintId = value.id.itemName
    }
}

interface IUserRepository {
  suspend fun getUser(id: Int): User?
  suspend fun getUser(username: String): User?
}

class UserRepository : IUserRepository {
  private val entityManager = HibernateUtils.createEntityManager()

  override suspend fun getUser(id: Int): User? = withContext(Dispatchers.IO) {
    entityManager.find(User::class.java, id)
  }

  override suspend fun getUser(username: String): User? {
    return try {
      withContext(Dispatchers.IO) {
        entityManager
          .createQuery("FROM User WHERE username = :username", User::class.java)
          .setParameter("username", username)
          .singleResult
      }
    } catch(exception: NoResultException) {
      null
    }
  }
}

@Entity
@Table(
  name = "users",
  indexes = [
    Index(name = "idx_users_username", columnList = "username")
  ]
)
class User(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(nullable = false, unique = true, length = 64) var username: String,
  @Column(nullable = false) var password: String,
  @Column(nullable = false) var score: Int,
  @Column(nullable = false) var crystals: Int,

  @OneToMany(targetEntity = ServerGarageUserItem::class, mappedBy = "id.user")
  val items: MutableList<ServerGarageUserItem>,

  @OneToMany(targetEntity = ServerDailyQuest::class, mappedBy = "user")
  val dailyQuests: MutableList<ServerDailyQuest>
) {
  @AttributeOverride(name = "hullId", column = Column(name = "equipment_hull_id"))
  @AttributeOverride(name = "weaponId", column = Column(name = "equipment_weapon_id"))
  @AttributeOverride(name = "paintId", column = Column(name = "equipment_paint_id"))
  @Embedded lateinit var equipment: UserEquipment

  val rank: UserRank
    get() {
      var rank = UserRank.Recruit
      var nextRank: UserRank = rank.nextRank ?: return rank
      while(score >= nextRank.score) {
        rank = nextRank
        nextRank = rank.nextRank ?: return rank
      }
      return rank
    }

  val currentRankScore: Int
    get() {
      val nextRank = rank.nextRank ?: return score
      return nextRank.score - score
    }
}
