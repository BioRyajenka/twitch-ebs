package tech.favs.ebs.util

fun <T> List<T>.splitBy(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    return filter(predicate) to filterNot(predicate)
}
