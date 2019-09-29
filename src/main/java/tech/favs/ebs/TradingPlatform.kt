package tech.favs.ebs

import tech.favs.ebs.model.*
import tech.favs.ebs.tradingplatforms.yamandberu.BeruDeeplinkGenerator
import tech.favs.ebs.tradingplatforms.yamandberu.BeruProductInformationExtractor
import tech.favs.ebs.tradingplatforms.yamandberu.YAMDeeplinkGenerator
import tech.favs.ebs.tradingplatforms.yamandberu.YAMProductInformationExtractor
import tech.favs.ebs.util.ApplicationProperty
import tech.favs.ebs.util.OperationValueResult

private val HOST_REGEXP = "https?://(.*?)/.*".toRegex()

private val yamDeeplinkClid by ApplicationProperty<Int>("tp.yam.deeplink-clid")
private val yamWidgetClid by ApplicationProperty<Int>("tp.yam.widget-clid")
private val beruDeeplinkClid by ApplicationProperty<Int>("tp.beru.deeplink-clid")
private val beruWidgetClid by ApplicationProperty<Int>("tp.beru.widget-clid")

enum class TradingPlatform(private val host: String, val deeplinkGenerator: DeeplinkGenerator, val productInformationExtractor: ProductInformationExtractor) {
    YAM("market.yandex.ru", YAMDeeplinkGenerator(yamDeeplinkClid), YAMProductInformationExtractor(yamWidgetClid)),
    BERU("beru.ru", BeruDeeplinkGenerator(beruDeeplinkClid), BeruProductInformationExtractor(beruWidgetClid));

    fun test(url: String): Boolean {
        val mr = HOST_REGEXP.find(url)
        if (mr == null || mr.groupValues.size < 2) return false
        val urlHost = mr.groupValues[1]
        return urlHost == host || urlHost.endsWith(".$host")
    }
}

class GeneralDeeplinkGenerator : DeeplinkGenerator {
    override fun generate(streamerId: Int, urls: List<String>): OperationValueResult<out List<Deeplink>> {
        return OperationValueResult.groupByCatching(urls) { url ->
            TradingPlatform.values().find { it.test(url) } ?: throw UnknownTradingPlatformException(url)
        }.flatMap { urlGroups ->
            OperationValueResult.flatMapWithLift2(urlGroups.entries.toList()) { (tradingPlatform, platformUrls) ->
                tradingPlatform.deeplinkGenerator.generate(streamerId, platformUrls)
            }
        }
    }
}

class GeneralProductInformationExtractor : ProductInformationExtractor {
    override fun extract(urls: List<String>): OperationValueResult<out List<ProductInformation>> {
        return OperationValueResult.groupByCatching(urls) { url ->
            TradingPlatform.values().find { it.test(url) } ?: throw UnknownTradingPlatformException(url)
        }.flatMap { urlGroups ->
            OperationValueResult.flatMapWithLift2(urlGroups.entries.toList()) { (tradingPlatform, platformUrls) ->
                tradingPlatform.productInformationExtractor.extract(platformUrls)
            }
        }
    }
}
