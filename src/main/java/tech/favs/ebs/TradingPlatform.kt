package tech.favs.ebs

import tech.favs.ebs.model.Deeplink
import tech.favs.ebs.model.DeeplinkGenerator
import tech.favs.ebs.model.ProductInformation
import tech.favs.ebs.model.ProductInformationExtractor
import tech.favs.ebs.tradingplatforms.yamandberu.BeruDeeplinkGenerator
import tech.favs.ebs.tradingplatforms.yamandberu.BeruProductInformationExtractor
import tech.favs.ebs.tradingplatforms.yamandberu.YAMDeeplinkGenerator
import tech.favs.ebs.tradingplatforms.yamandberu.YAMProductInformationExtractor
import tech.favs.ebs.util.ApplicationProperty
import tech.favs.ebs.util.OperationListResult
import tech.favs.ebs.util.OperationValueResult

private val HOST_REGEXP = "https?://(.*?)/.*".toRegex()

private val yamDeeplinkClid by ApplicationProperty<Int>("tp.yam.deeplink-clid")
private val yamWidgetClid by ApplicationProperty<Int>("tp.yam.widget-clid")
private val beruDeeplinkClid by ApplicationProperty<Int>("tp.beru.deeplink-clid")
private val beruWidgetClid by ApplicationProperty<Int>("tp.beru.widget-clid")

enum class TradingPlatform(private val host: String,
                           val deeplinkGenerator: DeeplinkGenerator,
                           val productInformationExtractor: ProductInformationExtractor) {
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
    override fun generate(streamerId: Int, urls: List<String>): OperationListResult<String, out Deeplink> {
        return OperationListResult.fromList(urls) { url ->
            OperationValueResult.fromNullable(TradingPlatform.values().find { it.test(url) }, "Адрес данного магазина не поддерживается или введен неверно")
        }.flatMapLift2 { tradingPlatformList ->
            tradingPlatformList.groupBy({ it.second }) { it.first }.map { (tradingPlatform, platformUrls) ->
                tradingPlatform.deeplinkGenerator.generate(streamerId, platformUrls)
            }
        }
    }
}

class GeneralProductInformationExtractor : ProductInformationExtractor {
    override fun extract(urls: List<String>): OperationListResult<String, out ProductInformation> {
        return OperationListResult.fromList(urls) { url ->
            OperationValueResult.fromNullable(TradingPlatform.values().find { it.test(url) }, "Адрес данного магазина не поддерживается или введен неверно")
        }.flatMapLift2 { tradingPlatformList ->
            tradingPlatformList.groupBy({ it.second }) { it.first }.map { (tradingPlatform, platformUrls) ->
                tradingPlatform.productInformationExtractor.extract(platformUrls)
            }
        }
    }
}
