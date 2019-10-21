package tech.favs.ebs.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import org.json.JSONObject
import tech.favs.ebs.GeneralDeeplinkGenerator
import tech.favs.ebs.GeneralProductInformationExtractor
import tech.favs.ebs.util.ApplicationProperty

private val maxUrlsInRequest by ApplicationProperty<Int>("request.max-urls")

private val deeplinkGenerator = GeneralDeeplinkGenerator()
private val productInformationExtractor = GeneralProductInformationExtractor()

fun Route.processRoutes() {
    get("/process_url") {
        val urls = call.parameters.getAll("url")
        println("urls are ${urls?.joinToString()}")
        val streamerId = call.parameters["streamerId"]

        if (urls == null || streamerId == null) {
            call.respond(HttpStatusCode.BadRequest, "You need to provide streamerId and at least one url")
            return@get
        }

        if (urls.size > maxUrlsInRequest) {
            call.respond(HttpStatusCode.BadRequest, "Too many urls in request")
            return@get
        }

        val response = try {
            val deeplinks = deeplinkGenerator.generate(streamerId.toInt(), urls)
            val productInfos = productInformationExtractor.extract(urls)
            val result = deeplinks.zip(productInfos)

            val resultJson = JSONObject()
            result.data.forEach { (originalUrl, originalUrlResult) ->
                val urlResultJson = originalUrlResult.fold(
                        successAction = { (deeplink, productInfo) ->
                            JSONObject()
                                    .put("deeplink", deeplink.deeplink)
                                    .put("name", productInfo.name)
                                    .put("formattedPrice", productInfo.formattedPrice)
                                    .put("imageUrl", productInfo.imageUrl)
                        },
                        failureAction = { error ->
                            JSONObject().put("error", error)
                        }
                )
                resultJson.put(originalUrl, urlResultJson)
            }
            JSONObject().put("result", resultJson)
        } catch (e: Throwable) {
            e.printStackTrace()
            JSONObject().append("generalErrors", "Unknown error")
        }

        call.respondText(response.toString(), ContentType.Application.Json)
    }
}
