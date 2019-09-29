package tech.favs.ebs.tradingplatforms.yamandberu

import khttp.get
import org.json.JSONObject
import tech.favs.ebs.model.ProductIdParseException
import tech.favs.ebs.model.ProductInformation
import tech.favs.ebs.model.ProductInformationExtractor
import tech.favs.ebs.util.OperationValueResult
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/*
 * https://market.yandex.ru/product--artesia-pa-88w/13016017
 * https://market.yandex.ru/product--artesia-pa-88w/13016017/asasdas
 * https://market.yandex.ru/product--artesia-pa-88w/13016017?asasdas
 * gives 13016017
 * https://beru.ru/product/100427321876
 * gives 100427321876
 * // TODO: add tests
 */
private val PRODUCT_ID_REGEX = "https?://.*?/.*?/(.*?)(?:\$|/|\\?)".toRegex()

private val priceFormatter = (NumberFormat.getInstance(Locale.US) as DecimalFormat).also {
    val symbols = it.decimalFormatSymbols
    symbols.groupingSeparator = ' '
    it.decimalFormatSymbols = symbols
}

private fun parseResponseJson(productIds: List<Pair<String, Int>>, resultJson: JSONObject): OperationValueResult<List<ProductInformation>> {
    val collectionsJson = resultJson.getJSONObject("data")
            .getJSONObject("data")
            .getJSONObject("collections")
    val modelJson = collectionsJson.getJSONObject("model")

    return OperationValueResult.mapCatching(productIds) { (url, productId) ->
        val productModelJson = modelJson.getJSONObject(productId.toString())
        val name = productModelJson.getString("name")
        val imageUrl = productModelJson.getJSONObject("photo").getString("url")

        val price = collectionsJson.getJSONObject("offer").let { offerJson ->
            val productOfferJson = offerJson.keySet().map { key -> offerJson.getJSONObject(key) }.singleOrNull { productOfferJson ->
                val id = productOfferJson.getJSONObject("model").getInt("id")
                id == productId
            } ?: error("")

            productOfferJson.getJSONObject("price").getString("value").toInt()
        }

        ProductInformation(url, imageUrl, name, "${priceFormatter.format(price)} ₽")
    }
}

abstract class YandexProductInformationExtractor(protected val widgetClid: Int) : ProductInformationExtractor {
    protected abstract fun getProductsInfo(productIds: List<Pair<String, Int>>): OperationValueResult<List<ProductInformation>>

    override fun extract(urls: List<String>): OperationValueResult<out List<ProductInformation>> {
        return OperationValueResult.mapCatching(urls) { url ->
            url to (PRODUCT_ID_REGEX.matchEntire(url)?.groupValues?.get(1)?.toIntOrNull() ?: throw ProductIdParseException(url))
        }.flatMap(::getProductsInfo)
    }
}

class BeruProductInformationExtractor(widgetClid: Int) : YandexProductInformationExtractor(widgetClid) {
    override fun getProductsInfo(productIds: List<Pair<String, Int>>): OperationValueResult<List<ProductInformation>> {
        return OperationValueResult.flatMapWithLift2(productIds) {
            val r = get("https://aflt.market.yandex.ru/widget/multi/api/initByType/beruOffers",
                    params = mapOf(
                            "searchSkuIds" to it.toString(),
                            "clid" to widgetClid.toString(),
                            "themeId" to "2",
                            "rotateMarketToBeru" to "false"
                    ))
            parseResponseJson(listOf(it), r.jsonObject)
        }
    }
}

class YAMProductInformationExtractor(widgetClid: Int) : YandexProductInformationExtractor(widgetClid) {
    override fun getProductsInfo(productIds: List<Pair<String, Int>>): OperationValueResult<List<ProductInformation>> {
        val r = get("https://aflt.market.yandex.ru/widget/multi/api/initByType/models",
                params = mapOf(
                        "searchModelIds" to productIds.joinToString(","),
                        "clid" to widgetClid.toString(),
                        "themeId" to "1",
                        "rotateMarketToBeru" to "false"
                ))
        return parseResponseJson(productIds, r.jsonObject)
    }
}
