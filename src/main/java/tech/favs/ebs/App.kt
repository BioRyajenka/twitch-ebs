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
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tech.favs.ebs.dao.Deeplinks
import tech.favs.ebs.routes.processRoutes
import tech.favs.ebs.routes.userRoutes

val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

fun initDb() {
    Database.connect("jdbc:sqlite:test.db", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.createMissingTablesAndColumns(Deeplinks)
    }
}

class App : CliktCommand() {
    private val port: Int by option(help = "EBS port").int().default(8080)

    override fun run() {
        initDb()

        embeddedServer(Netty, port) {
            install(CORS) {
                anyHost()
            }

//            install(DefaultHeaders)
//            install(CallLogging)
            routing {
                route("/api") {
                    processRoutes()
                    userRoutes()
                }
            }
        }.start(wait = true)
    }
}

fun main(args: Array<String>) = App().main(args)
