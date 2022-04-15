package jp.assasans.protanki.server.extensions

import jp.assasans.protanki.server.BuildConfig

val BuildConfig.gitVersion: String
  get() = "$GIT_BRANCH+${GIT_COMMIT_HASH.take(8)}${if(GIT_IS_DIRTY) "-dirty" else ""}"
