package jp.assasans.protanki.server.client

import kotlinx.coroutines.flow.MutableStateFlow

class UserSubscription(user: User) {
  val rank = MutableStateFlow(user.rank)
}

interface IUserSubscriptionManager {
  fun add(user: User)

  fun getOrNull(id: Int): UserSubscription?
  fun get(id: Int): UserSubscription
}

class UserSubscriptionManager : IUserSubscriptionManager {
  private val subscriptions = mutableMapOf<Int, UserSubscription>()

  override fun add(user: User) {
    if(subscriptions.containsKey(user.id)) return
    subscriptions[user.id] = UserSubscription(user)
  }

  override fun getOrNull(id: Int): UserSubscription? = subscriptions[id]

  override fun get(id: Int): UserSubscription =
    getOrNull(id) ?: throw IllegalStateException("User $id not found")
}
