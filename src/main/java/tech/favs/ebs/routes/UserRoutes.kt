package tech.favs.ebs.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import tech.favs.ebs.admitad.UserProfitGenerator
import tech.favs.ebs.objectMapper

private val userProfitGenerator = UserProfitGenerator()

fun Route.userRoutes() {
    get("/user/{user_id}/profit") {
        val userId = call.parameters["user_id"]!!
        val userProfit = userProfitGenerator.calcProfit(userId)

        val response = objectMapper.writeValueAsString(userProfit)
        call.respondText(response, ContentType.Application.Json)
    }
}
