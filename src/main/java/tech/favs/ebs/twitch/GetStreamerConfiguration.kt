package tech.favs.ebs.twitch

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import khttp.get
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

private val JWT_SECRET = Base64.getDecoder().decode("cR0N9qhMNok3ACgjwslvAo+A84HJzYsTRFwrNE8ObHU=")

@Suppress("UNCHECKED_CAST")
private fun JWTCreator.Builder.withClaimUnsafe(name: String, obj: Any): JWTCreator.Builder {
    val field = JWTCreator.Builder::class.java.getDeclaredField("payloadClaims")
    field.isAccessible = true
    val payloadClaims = field.get(this) as MutableMap<String, Any>
    payloadClaims[name] = obj
    return this
}

private fun createJWT(userId: String): String {
    return JWT.create()
            .withClaim("exp", Date.from(LocalDateTime.now().plusMinutes(3).toInstant(ZoneOffset.UTC)))
            .withClaim("user_id", userId)
            .withClaim("role", "external")
//            .withClaim("channel_id", userId)
//            .withClaimUnsafe("pubsub_perms", mapOf("send" to listOf("broadcast")))
            .sign(Algorithm.HMAC256(JWT_SECRET))
}

fun getBroadcasterConfiguration(extensionId: String, userId: String): JSONArray {
    val jwt = createJWT(userId)

    val r = get("https://api.twitch.tv/extensions/$extensionId/configurations/segments/broadcaster?channel_id=$userId",
//    val r = get("https://api.twitch.tv/extensions/$extensionId/configurations/channels/$userId",
            headers = mapOf(
                    "Authorization" to "Bearer $jwt",
                    "Client-Id" to extensionId,
                    "Content-Type" to "application/json"
            ))
    val resultJson = r.jsonObject

    return resultJson.keySet().single().let {
        val contentString = resultJson
                .getJSONObject(it)
                .getJSONObject("record")
                .getString("content")!!
        JSONArray(contentString)
    }
}

fun main() {
    // new api key is 1u611hgop22tuakn0f0xgntptgs3qb
    val CLIENT_ID = "azmhg8jirkn1v77txbk9fspqsorqd8"
    val userId = "218603102"

//    println(createJWT(userId))

    val res = getBroadcasterConfiguration(CLIENT_ID, userId)
    println(res)
}
