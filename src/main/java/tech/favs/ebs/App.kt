package tech.favs.ebs

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import tech.favs.ebs.routes.deeplinkRoutes
import tech.favs.ebs.routes.userRoutes


const val CLIENT_SECRET = "54fd5cf72c7ef823c6a639717f07ee"

val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

class App : CliktCommand() {
    private val port: Int by option(help = "EBS port").int().default(8080)

    override fun run() {
        embeddedServer(Netty, port) {
            install(CORS) {
                anyHost()
            }

            routing {
                route("/api") {
                    deeplinkRoutes()
                    userRoutes()
                }
            }
        }.start(wait = true)
    }
}

fun main(args: Array<String>) = App().main(args)
