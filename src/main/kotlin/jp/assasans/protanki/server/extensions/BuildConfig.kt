package jp.assasans.protanki.server.extensions

import jp.assasans.protanki.server.BuildConfig

val BuildConfig.gitVersion: String
  get() {
    val gitSuffix = if(GIT_BRANCH.isNotEmpty() && GIT_COMMIT_HASH.isNotEmpty()) "/$GIT_BRANCH+${GIT_COMMIT_HASH.take(8)}" else ""
    return "$VERSION$gitSuffix"
  }
