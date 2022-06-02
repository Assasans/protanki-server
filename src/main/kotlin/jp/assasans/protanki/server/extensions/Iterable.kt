package jp.assasans.protanki.server.extensions

// TODO(Assasans): Find a way to not specify the type of the Iterable<T> in calls
inline fun <T, reified I : T> Iterable<T>.singleOf(): I? = this.single { it is I }?.cast()
inline fun <T, reified I : T> Iterable<T>.singleOrNullOf(): I? = this.singleOrNull { it is I }?.cast()
