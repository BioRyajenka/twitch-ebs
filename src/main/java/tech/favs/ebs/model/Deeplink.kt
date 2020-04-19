package tech.favs.ebs.model

import tech.favs.ebs.util.OperationListResult

interface DeeplinkGenerator {
    fun generate(streamerId: Int, urls: List<String>): OperationListResult<String, out Deeplink>
}

data class Deeplink(val originalUrl: String, val deeplink: String, val subId: Int, val streamerId: Int)
