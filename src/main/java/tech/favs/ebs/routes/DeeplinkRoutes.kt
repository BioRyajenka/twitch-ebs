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
import tech.favs.ebs.model.UrlAwareException
import tech.favs.ebs.util.ApplicationProperty
import tech.favs.ebs.util.splitBy

private val maxUrlsInRequest by ApplicationProperty<Int>("request.max-urls")

private val deeplinkGenerator = GeneralDeeplinkGenerator()
private val productInformationExtractor = GeneralProductInformationExtractor()

fun Route.deeplinkRoutes() {
    get("/deeplink") {
        val urls = call.parameters.getAll("url")
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
            val result = deeplinkGenerator.generate(streamerId.toInt(), urls).flatMap { deeplinks ->
                productInformationExtractor.extract(urls).map { productInfos ->
                    deeplinks to productInfos
                }
            }

            result.fold(
                    successAction = { (deeplinks, productInfos) ->
                        JSONObject().apply {
                            deeplinks.forEach { (originalUrl, deepUrl) ->
                                val productInfo = productInfos.find { it.productUrl == originalUrl }
                                        ?: error("There is no product with url $originalUrl")

                                val infoObject = JSONObject()
                                        .put("url", originalUrl)
                                        .put("deeplink", deepUrl)
                                        .put("name", productInfo.name)
                                        .put("formattedPrice", productInfo.formattedPrice)
                                        .put("imageUrl", productInfo.imageUrl)
                                append("deeplinks", infoObject)
                            }
                        }
                    },
                    failureAction = { errors ->
                        val (urlErrors, generalErrors) = errors.splitBy { it is UrlAwareException }
                        JSONObject().apply {
                            urlErrors.map { it as UrlAwareException }.forEach { error ->
                                val pairObject = JSONObject()
                                        .put("url", error.url)
                                        .put("error", error.message)
                                append("urlErrors", pairObject)
                            }
                            generalErrors.forEach {
                                append("generalErrors", it.message)
                            }
                        }
                    }
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            JSONObject().append("generalErrors", "Unknown error")
        }

        call.respondText(response.toString(), ContentType.Application.Json)
    }
}
