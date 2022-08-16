package jp.assasans.protanki.server.client

import kotlinx.coroutines.flow.MutableStateFlow

class UserSubscription(user: User) {
  val rank = MutableStateFlow(user.rank)
}

interface IUserSubscriptionManager {
  fun add(user: User): UserSubscription

  fun getOrNull(id: Int): UserSubscription?
  fun get(id: Int): UserSubscription
  fun getOrAdd(user: User): UserSubscription
}

class UserSubscriptionManager : IUserSubscriptionManager {
  private val subscriptions = mutableMapOf<Int, UserSubscription>()

  override fun add(user: User): UserSubscription {
    subscriptions[user.id]?.let { return it }
    return UserSubscription(user).also { subscriptions[user.id] = it }
  }

  override fun getOrNull(id: Int): UserSubscription? = subscriptions[id]
  override fun get(id: Int): UserSubscription = getOrNull(id) ?: throw IllegalStateException("User $id not found")
  override fun getOrAdd(user: User): UserSubscription = getOrNull(user.id) ?: add(user)
}
