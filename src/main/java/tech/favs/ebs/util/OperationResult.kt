package tech.favs.ebs.util

class OperationValueResult<V> private constructor(
        @PublishedApi internal val result: V?,
        @PublishedApi internal val errors: List<Throwable>?
) {
    companion object {
        private fun <T, R : Any> mapWithErrors(list: List<T>, transform: (T) -> R): Pair<List<R>, List<Throwable>> {
            val errors = mutableListOf<Throwable>()

            val result = list.mapNotNull {
                try {
                    transform(it)
                } catch (e: Throwable) {
                    errors += e
                    null
                }
            }

            return result to errors
        }

        fun <T, R : Any> mapCatching(list: List<T>, transform: (T) -> R): OperationValueResult<List<R>> {
            val (result, errors) = mapWithErrors(list, transform)

            return if (errors.isNotEmpty()) {
                OperationValueResult(null, errors)
            } else {
                OperationValueResult(result, null)
            }
        }

        fun <T, R : Any> groupByCatching(list: List<T>, transform: (T) -> R): OperationValueResult<Map<R, List<T>>> {
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
        }

        fun <V> success(result: V) = OperationValueResult(result, null)
    }

    val isSuccess
        get() = result != null

    val isFailure
        get() = !isSuccess

    inline fun ifSuccess(action: (V) -> Unit): OperationValueResult<V> {
        if (isSuccess) action(result!!)
        return this
    }

    inline fun ifFailure(action: (List<Throwable>) -> Unit): OperationValueResult<V> {
        if (isFailure) action(errors!!)
        return this
    }

    inline fun <R> fold(successAction: (V) -> R, failureAction: (List<Throwable>) -> R): R {
        return when {
            isSuccess -> successAction(result!!)
            else -> failureAction(errors!!)
        }
    }

    fun <R> map(transform: (V) -> R): OperationValueResult<out R> {
        return when {
            isSuccess -> try {
                success(transform(result!!))
            } catch (e: Throwable) {
                val newErrors = (errors ?: mutableListOf()) + e
                OperationValueResult(null, newErrors)
            }
            else -> OperationValueResult(null, errors)
        }
    }

    fun <R> flatMap(transform: (V) -> OperationValueResult<out R>): OperationValueResult<out R> {
        return when {
            isSuccess -> transform(result!!)
            else -> OperationValueResult(null, errors)
        }
    }
}
