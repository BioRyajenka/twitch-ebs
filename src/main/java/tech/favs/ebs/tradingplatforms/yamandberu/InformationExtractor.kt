package tech.favs.ebs.tradingplatforms.yamandberu

import khttp.get
import org.json.JSONException
import org.json.JSONObject
import tech.favs.ebs.model.ProductInformation
import tech.favs.ebs.model.ProductInformationExtractor
import tech.favs.ebs.util.OperationListResult
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

typealias ProductId = Long

private val YAM_PRODUCT_ID_REGEX = "https?://[^/]+/[^/]+/(\\d+).*".toRegex()
private val BERU_PRODUCT_ID_REGEX = "https?://[^/]+/[^/]+(?:/[^/]+)?/(\\d+).*".toRegex()

private val priceFormatter = (NumberFormat.getInstance(Locale.US) as DecimalFormat).also {
    val symbols = it.decimalFormatSymbols
    symbols.groupingSeparator = ' '
    it.decimalFormatSymbols = symbols
}

private fun parseResponseJson(modelIds: List<Pair<String, ProductId>>,
                              resultJson: JSONObject): OperationListResult<String, ProductInformation> {
    val collectionsJson = try {
        resultJson.getJSONObject("data")
                .getJSONObject("data")
                .getJSONObject("collections")
    } catch (e: JSONException) {
        System.err.println("In $resultJson")
        throw e
    }
    val modelJson = try {
        collectionsJson.getJSONObject("model")
    } catch (e: JSONException) {
        System.err.println("In $resultJson")
        throw e
    }

    return OperationListResult.fromListWithSpecificKey(modelIds) { (url, productId) ->
        val productModelJson = try {
            modelJson.getJSONObject(productId.toString())
        } catch (e: JSONException) {
            System.err.println("In $modelJson")
            throw e
        }
        val name = productModelJson.getString("name")

        val productOfferJson = collectionsJson.getJSONObject("offer").let { offerJson ->
            offerJson.keySet()
                    .map { key -> offerJson.getJSONObject(key) }
                    .findLast { productOfferJson ->
                        val id = productOfferJson.getJSONObject("model").getLong("id")
                        id == productId
                    } ?: error("")
        }

        val price = productOfferJson.getJSONObject("price").getString("value").toInt()

        val imageUrl = try {
            // YAM
            productModelJson.getJSONObject("photo").getString("url")
        } catch (ignored: JSONException) {
            // YAM and beru
            productOfferJson.getJSONObject("photo").getString("url")
        }

        url to OperationValueResult.success(ProductInformation(url, imageUrl, name, "${priceFormatter.format(price)} ₽"))
    }
}

abstract class YandexProductInformationExtractor(protected val widgetClid: Int,
                                                 private val productToIdRegex: Regex) : ProductInformationExtractor {
    protected abstract fun getProductsInfo(productIds: List<Pair<String, ProductId>>): OperationListResult<String, ProductInformation>

    override fun extract(urls: List<String>): OperationListResult<String, out ProductInformation> {
        return OperationListResult.fromList(urls) { url ->
            val productId = productToIdRegex.matchEntire(url)?.groupValues?.get(1)?.toLongOrNull()
            OperationValueResult.fromNullable(productId, "URL указан неправильно. Обратитесь в техподдержку.")
        }.flatMap(::getProductsInfo)
    }
}

class BeruProductInformationExtractor(widgetClid: Int) : YandexProductInformationExtractor(widgetClid, BERU_PRODUCT_ID_REGEX) {
    companion object {
        private fun convertSkuIdsToModelIds(resultJson: JSONObject, skuIds: List<Pair<String, ProductId>>): List<Pair<String, ProductId>> {
            val offersJson = resultJson.getJSONObject("data")
                    .getJSONObject("data")
                    .getJSONObject("collections")
                    .getJSONObject("offer")
            val skuToModelMap = offersJson.keySet().map { offersJson.getJSONObject(it) }.map {
                it.getString("sku").toLong() to it.getJSONObject("model").getInt("id").toLong()
            }.toMap()
            return skuIds.map { (url, skuId) ->
                url to (skuToModelMap[skuId] ?: error("There is no sku with id $skuId in resultJson"))
            }
        }
    }

    override fun getProductsInfo(productIds: List<Pair<String, ProductId>>): OperationListResult<String, ProductInformation> {
        val r = get("https://aflt.market.yandex.ru/widget/multi/api/initByType/beruOffers",
                params = mapOf(
                        "searchSkuIds" to productIds.map { it.second }.joinToString(","),
                        "clid" to widgetClid.toString(),
                        "themeId" to "2",
                        "rotateBeruToMarket" to "false"
                ))
        println(r.request.url)
        val modelIds = convertSkuIdsToModelIds(r.jsonObject, productIds)
        return parseResponseJson(modelIds, r.jsonObject)
    }
}

class YAMProductInformationExtractor(widgetClid: Int) : YandexProductInformationExtractor(widgetClid, YAM_PRODUCT_ID_REGEX) {
    override fun getProductsInfo(productIds: List<Pair<String, ProductId>>): OperationListResult<String, ProductInformation> {
        val r = get("https://aflt.market.yandex.ru/widget/multi/api/initByType/models",
                params = mapOf(
                        "searchModelIds" to productIds.map { it.second }.joinToString(","),
                        "clid" to widgetClid.toString(),
                        "themeId" to "1",
                        "rotateMarketToBeru" to "false"
                ))
        println(r.request.url)
        return parseResponseJson(productIds, r.jsonObject)
    }
}
