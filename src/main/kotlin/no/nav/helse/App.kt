package no.nav.helse

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.nais.nais
import no.nav.helse.ws.Clients
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.infotrygdberegningsgrunnlag.v1.binding.InfotrygdBeregningsgrunnlagV1
import no.nav.tjeneste.virksomhet.infotrygdsak.v1.binding.InfotrygdSakV1
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2
import no.nav.tjeneste.virksomhet.meldekortutbetalingsgrunnlag.v1.binding.MeldekortUtbetalingsgrunnlagV1
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sykepenger.v2.binding.SykepengerV2
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

private val collectorRegistry = CollectorRegistry.defaultRegistry
private val log = LoggerFactory.getLogger("App")

fun main() {
    val env = Environment()

    DefaultExports.initialize()

    val stsClient = stsClient(env.securityTokenServiceEndpointUrl,
            env.securityTokenUsername to env.securityTokenPassword)

    val timers = listOf(
            Webservice("ArbeidsfordelingV1", Clients.ArbeidsfordelingV1(env.arbeidsfordelingEndpointUrl), ArbeidsfordelingV1::ping),
            Webservice("ArbeidsforholdV3", Clients.ArbeidsforholdV3(env.arbeidsforholdEndpointUrl), ArbeidsforholdV3::ping),
            Webservice("InfotrygdBeregningsgrunnlagV1", Clients.InfotrygdBeregningsgrunnlagConsumerConfig(env.infotrygdBeregningsgrunnlagEndpointUrl), InfotrygdBeregningsgrunnlagV1::ping),
            Webservice("InfotrygdSakV1", Clients.InfotrygdSakV1(env.infotrygdSakEndpointUrl), InfotrygdSakV1::ping),
            Webservice("InntektV3", Clients.InntektV3(env.inntektEndpointUrl), InntektV3::ping),
            Webservice("MedlemskapV2", Clients.MedlemskapV2(env.medlemskapEndpointUrl), MedlemskapV2::ping),
            Webservice("MeldekortUtbetalingsgrunnlagV1", Clients.MeldekortUtbetalingsgrunnlagV1(env.meldekortUtbetalingsgrunnlagEndpointUrl), MeldekortUtbetalingsgrunnlagV1::ping),
            Webservice("OrganisasjonV5", Clients.OrganisasjonV5(env.organisasjonEndpointUrl), OrganisasjonV5::ping),
            Webservice("PersonV3", Clients.PersonV3(env.personEndpointUrl), PersonV3::ping),
            Webservice("SykepengerV2", Clients.SykepengerV2(env.sykepengerEndpointUrl), SykepengerV2::ping)
    ).onEach {
        stsClient.configureFor(it.port)
    }.map {
        timer(period = 10000) {
            it.ping()
        }
    }

    val app = embeddedServer(Netty, 8080) {
        routing {
            nais(collectorRegistry)
        }
    }

    app.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        timers.forEach {
            it.cancel()
        }
        app.stop(5, 60, TimeUnit.SECONDS)
    })
}

data class Webservice<T>(val serviceName: String, val port: T, private val pingFunc: T.() -> Unit) {

    companion object {
        private val wsTimer: Histogram = Histogram.build()
                .name("webservice_ping_seconds")
                .labelNames("name")
                .help("latency for ping requests").register()

        private val wsErrorCounter: Counter = Counter.build()
                .name("webservice_ping_error_counter")
                .labelNames("name")
                .help("error counter for failed ping requests").register()
    }

    init {
        wsErrorCounter.labels(serviceName).inc(0.0)
    }

    fun ping() {
        wsTimer.labels(serviceName).time {
            try {
                port.pingFunc()
            } catch (err: Exception) {
                wsErrorCounter.labels(serviceName).inc()
                log.error("Failed to ping webservice", err)
            }
        }
    }
}
