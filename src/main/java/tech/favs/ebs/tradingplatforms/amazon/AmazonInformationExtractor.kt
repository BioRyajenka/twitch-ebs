package tech.favs.ebs.tradingplatforms.amazon

import khttp.get
import org.json.JSONObject
import tech.favs.ebs.model.ProductInformation
import tech.favs.ebs.model.ProductInformationExtractor
import tech.favs.ebs.util.OperationListResult
import tech.favs.ebs.util.OperationValueResult

// https://www.amazon.com/Nintendo-Switch-Pro-Controller/dp/B01NAWKYZ0/...
private val ASIN_REGEXP = "https?://[^/]*/[^/]*/[^/]*/(.*?)(/|\\?|\$)".toRegex()
// https://www.amazon.com/dp/B07ZPRPFQY/...
private val ASIN_REGEXP_SHORT = "https?://[^/]*/dp/(.*?)(/|\\?|$)".toRegex()

class AmazonInformationExtractor : ProductInformationExtractor {
    override fun extract(urls: List<String>): OperationListResult<String, out ProductInformation> {
        return OperationListResult.fromList(urls) { url ->
            val mr = ASIN_REGEXP_SHORT.find(url).let {
                if (it == null || it.groupValues.size < 2) {
                    ASIN_REGEXP.find(url)
                } else it
            }
            if (mr == null || mr.groupValues.size < 2) {
                return@fromList OperationValueResult.failure<ProductInformation>("Wrong url. Please use URLs like this: https://www.amazon.com/gp/product/B0795Z97K7")
            }
            val asin = mr.groupValues[1]
            val r = get("https://amazon-products1.p.rapidapi.com/product?country=US&asin=$asin",
                    headers = mapOf(
                            "x-rapidapi-host" to "amazon-products1.p.rapidapi.com",
                            "x-rapidapi-key" to "2f0ea2183fmshad1158466ce17cep1cd070jsn209332013f7d"
                    )
            )
            println(r.request.url)
            println("response: ${r.text}")

            val jsonResponse = if (r.text.startsWith("\"")) {
                JSONObject(r.text.trim('\"').replace("\\\"", "\""))
            } else {
                JSONObject(r.text)
            }

            with(jsonResponse) {
                if (getBoolean("error")) {
                    val error = if (optString("message").isNullOrEmpty()) {
                        "Error getting response"
                    } else getString("message")
                    OperationValueResult.failure(error)
                } else {
                    val imageUrl = getJSONArray("images").getString(0)
                    val name = getString("title")
                    val formattedPrice = getJSONObject("prices").let { pricesObject ->
                        "${pricesObject.getDouble("current_price")} ${pricesObject.getString("currency")}"
                    }
                    OperationValueResult.success(
                            ProductInformation(url, imageUrl, name, formattedPrice)
                    )
                }
            }
        }
    }
}
