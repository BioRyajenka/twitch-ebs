package tech.favs.ebs.model

import tech.favs.ebs.util.OperationValueResult

interface DeeplinkGenerator {
    fun generate(streamerId: Int, urls: List<String>): OperationValueResult<out List<Deeplink>>
}

data class Deeplink(val originalUrl: String, val deeplink: String)
