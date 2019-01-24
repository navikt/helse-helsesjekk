package no.nav.helse.nais

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import java.util.Collections

fun Routing.nais(collectorRegistry: CollectorRegistry) {
    get("/isalive") {
        application.log.trace("/isalive called")
        call.respondText("ALIVE", ContentType.Text.Plain)
    }

    get("/isready") {
        application.log.trace("/isready called")
        call.respondText("READY", ContentType.Text.Plain)
    }

    get("/metrics") {
        application.log.trace("/metrics called")
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
        }
    }
}
