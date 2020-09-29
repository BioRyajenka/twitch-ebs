package tech.favs.ebs.tradingplatforms.amazon

import tech.favs.ebs.model.Deeplink
import tech.favs.ebs.model.DeeplinkGenerator
import tech.favs.ebs.util.OperationListResult
import tech.favs.ebs.util.OperationValueResult

class AmazonDeeplinkGenerator : DeeplinkGenerator {
    override fun generate(streamerId: Int, urls: List<String>): OperationListResult<String, out Deeplink> {
        return OperationListResult.fromList(urls) { url ->
            val deeplink = Deeplink(url, url, 0, streamerId)
            OperationValueResult.success(deeplink)
        }
    }

}
