package tech.favs.ebs.util

class OperationValueResult<V> private constructor(
        @PublishedApi internal val result: V?,
        @PublishedApi internal val error: String?
) {
    companion object {
        /*fun <T, R : Any> mapCatching(list: List<T>, transform: (T) -> R): OperationValueResult<List<R>> {
            val (result, errors) = mapWithErrors(list, transform)

            return if (errors.isNotEmpty()) {
                OperationValueResult(null, error)
            } else {
                OperationValueResult(result, null)
            }
        }*/

        /*fun <T, R : Any> groupByCatching(list: List<T>, transform: (T) -> R): OperationValueResult<Map<R, List<T>>> {
            val (result, errors) = mapWithErrors(list) {
                it to transform(it)
            }

            return if (errors.isNotEmpty()) {
                OperationValueResult(null, errors)
            } else {
                val groups = result.groupBy { it.second }.mapValues { (_, v) -> v.map { it.first } }
                OperationValueResult(groups, null)
            }
        }

        private fun <T, R : Any, R2> flatMapWithLift(list: List<T>,
                                                     transform: (T) -> OperationValueResult<out R>,
                                                     unpack: (List<OperationValueResult<out R>>) -> List<R2>): OperationValueResult<List<R2>> {
            val (result, errors) = mapWithErrors(list, transform)
            val subErrors = result.filter { it.isFailure }.flatMap {
                it.errors!!
            }
            val allErrors = errors + subErrors
            return if (allErrors.isNotEmpty()) {
                OperationValueResult(null, allErrors)
            } else {
                val trueResult = unpack(result.filter { it.isSuccess })
                OperationValueResult(trueResult, null)
            }
        }

        fun <T, R : Any> flatMapWithLift2(list: List<T>,
                                          transform: (T) -> OperationValueResult<out List<R>>): OperationValueResult<List<R>> {
            return flatMapWithLift(list, transform) { value ->
                value.flatMap { it.result!! }
            }
        }

        fun <T, R : Any> flatMap(list: List<T>,
                                 transform: (T) -> OperationValueResult<R>): OperationValueResult<List<R>> {
            return flatMapWithLift(list, transform) { value ->
                value.map { it.result!! }
            }
        }*/

        fun <V> success(result: V) = OperationValueResult(result, null)

        fun <V> failure(error: String) = OperationValueResult<V>(null, error)

        fun <V> fromNullable(result: V?, error: String) = result?.let { success(it) } ?: failure(error)
    }

    val isSuccess
        get() = result != null

    val isFailure
        get() = !isSuccess

    inline fun ifSuccess(action: (V) -> Unit): OperationValueResult<V> {
        if (isSuccess) action(result!!)
        return this
    }

    inline fun ifFailure(action: (String) -> Unit): OperationValueResult<V> {
        if (isFailure) action(error!!)
        return this
    }

    inline fun <R> fold(successAction: (V) -> R, failureAction: (String) -> R): R {
        return when {
            isSuccess -> successAction(result!!)
            else -> failureAction(error!!)
        }
    }

    fun <R> map(transform: (V) -> R): OperationValueResult<out R> {
        return when {
            isSuccess -> success(transform(result!!))
            else -> failure(error!!)
        }
    }

    fun <R> flatMap(transform: (V) -> OperationValueResult<out R>): OperationValueResult<out R> {
        return when {
            isSuccess -> transform(result!!)
            else -> failure(error!!)
        }
    }
}

class OperationListResult<V, R> private constructor(val data: List<Pair<V, OperationValueResult<out R>>>) {

    companion object {
        public fun <T, R : Any> fromList(list: List<T>, transform: (T) -> OperationValueResult<out R>): OperationListResult<T, R> {
            return fromListWithSpecificKey(list) { it to transform(it) }
        }

        public fun <T, K, R : Any> fromListWithSpecificKey(list: List<T>, transform: (T) -> Pair<K, OperationValueResult<out R>>): OperationListResult<K, R> {
            return OperationListResult(list.map(transform))
        }

    }

    private fun <R2, Z> flatMapLift(transform: (List<Pair<V, R>>) -> Z, unpack: (Z) -> OperationListResult<V, out R2>): OperationListResult<V, out R2> {
        val (successes, failures) = data.splitBy { it.second.isSuccess }
        val filteredData = successes.map { (k, v) ->
            k to v.result!!
        }

        val resultData = unpack(transform(filteredData)).data
        val oldFailures = failures.map { (k, v) ->
            k to OperationValueResult.failure<R2>(v.error!!)
        }

        return OperationListResult(oldFailures + resultData)
    }

    fun <R2> flatMap(transform: (List<Pair<V, R>>) -> OperationListResult<V, out R2>): OperationListResult<V, out R2> {
        return flatMapLift(transform) { it }
    }

    fun <R2> flatMapLift2(transform: (List<Pair<V, R>>) -> List<OperationListResult<V, out R2>>): OperationListResult<V, out R2> {
        return flatMapLift(transform) { resultList: List<OperationListResult<V, out R2>> ->
            OperationListResult(resultList.flatMap { it.data })
        }
    }

    fun <R2> zip(rhs: OperationListResult<V, out R2>): OperationListResult<V, Pair<R, R2>> {
        return zip(rhs) { v1, v2 -> v1 to v2 }
    }

    fun <R2, R3> zip(rhs: OperationListResult<V, out R2>, transform: (R, R2) -> R3): OperationListResult<V, R3> {
        val rhsMap = rhs.data.toMap()
        val newData = data.map { (k, v) ->
            val rhsR = rhsMap[k] ?: error("Can't zip operation list results with different key sets")
            val res = v.flatMap { v1 ->
                rhsR.map { v2 -> transform(v1, v2) }
            }
            k to res
        }
        return OperationListResult(newData)
    }
}
