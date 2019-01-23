package no.nav.helse

import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Histogram
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.nais.nais
import no.nav.helse.ws.Clients
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

private val collectorRegistry = CollectorRegistry.defaultRegistry
private val log = LoggerFactory.getLogger("App")

fun main() {
    val env = Environment()

    DefaultExports.initialize()

    val app = embeddedServer(Netty, 8080) {
        webserviceSjekker(env)
    }

    app.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop(5, 60, TimeUnit.SECONDS)
    })
}

fun Application.webserviceSjekker(env: Environment) {
    val stsClient = stsClient(env.securityTokenServiceEndpointUrl,
            env.securityTokenUsername to env.securityTokenPassword)

    val arbeidsfordelingPingTimer = Histogram.build()
            .name("arbeidsfordeling_v1_ping_seconds")
            .help("latency for ArbeidsfordelingV1.ping()").register()
    val arbeidsforholdPingTimer = Histogram.build()
            .name("arbeidsforhold_v3_ping_seconds")
            .help("latency for ArbeidsforholdV3.ping()").register()
    val infotrygdBeregningsgrunnlagPingTimer = Histogram.build()
            .name("infotrygd_beregningsgrunnlag_v1_ping_seconds")
            .help("latency for InfotrygdBeregningsgrunnlagV1.ping()").register()
    val infotrygdSakPingTimer = Histogram.build()
            .name("infotrygd_sak_v1_ping_seconds")
            .help("latency for InfotrygdSakV1.ping()").register()
    val inntektPingTimer = Histogram.build()
            .name("inntekt_v3_ping_seconds")
            .help("latency for InntektV3.ping()").register()
    val medlemskapPingTimer = Histogram.build()
            .name("medlemskap_v2_ping_seconds")
            .help("latency for MedlemskapV2.ping()").register()
    val meldekortUtbetalingsgrunnlagPingTimer = Histogram.build()
            .name("meldekort_utbetalingsgrunnlag_v1_ping_seconds")
            .help("latency for MeldekortUtbetalingsgrunnlagV1.ping()").register()
    val organisasjonPingTimer = Histogram.build()
            .name("organisasjon_v5_ping_seconds")
            .help("latency for OrganisasjonV5.ping()").register()
    val personPingTimer = Histogram.build()
            .name("person_v3_ping_seconds")
            .help("latency for PersonV3.ping()").register()
    val sykepengerPingTimer = Histogram.build()
            .name("sykepenger_v2_ping_seconds")
            .help("latency for SykepengerV2.ping()").register()

    val arbeidsfordelingV1 = Clients.ArbeidsfordelingV1(env.arbeidsfordelingEndpointUrl)
            .apply(stsClient::configureFor)
    val arbeidsforholdV3 = Clients.ArbeidsforholdV3(env.arbeidsforholdEndpointUrl)
            .apply(stsClient::configureFor)
    val infotrygdBeregningsgrunnlagV1 = Clients.InfotrygdBeregningsgrunnlagConsumerConfig(env.infotrygdBeregningsgrunnlagEndpointUrl)
            .apply(stsClient::configureFor)
    val infotrygdSakV1 = Clients.InfotrygdSakV1(env.infotrygdSakEndpointUrl)
            .apply(stsClient::configureFor)
    val inntektV3 = Clients.InntektV3(env.inntektEndpointUrl)
            .apply(stsClient::configureFor)
    val medlemskapV2 = Clients.MedlemskapV2(env.medlemskapEndpointUrl)
            .apply(stsClient::configureFor)
    val meldekortUtbetalingsgrunnlagV1 = Clients.MeldekortUtbetalingsgrunnlagV1(env.meldekortUtbetalingsgrunnlagEndpointUrl)
            .apply(stsClient::configureFor)
    val organisasjonV5 = Clients.OrganisasjonV5(env.organisasjonEndpointUrl)
            .apply(stsClient::configureFor)
    val personV3 = Clients.PersonV3(env.personEndpointUrl)
            .apply(stsClient::configureFor)
    val sykepengerV2 = Clients.SykepengerV2(env.sykepengerEndpointUrl)
            .apply(stsClient::configureFor)

    checkWebservice(arbeidsfordelingPingTimer, arbeidsfordelingV1::ping)
    checkWebservice(arbeidsforholdPingTimer, arbeidsforholdV3::ping)
    checkWebservice(infotrygdBeregningsgrunnlagPingTimer, infotrygdBeregningsgrunnlagV1::ping)
    checkWebservice(infotrygdSakPingTimer, infotrygdSakV1::ping)
    checkWebservice(inntektPingTimer, inntektV3::ping)
    checkWebservice(medlemskapPingTimer, medlemskapV2::ping)
    checkWebservice(meldekortUtbetalingsgrunnlagPingTimer, meldekortUtbetalingsgrunnlagV1::ping)
    checkWebservice(organisasjonPingTimer, organisasjonV5::ping)
    checkWebservice(personPingTimer, personV3::ping)
    checkWebservice(sykepengerPingTimer, sykepengerV2::ping)

    routing {
        nais(collectorRegistry)
    }
}

fun checkWebservice(timer: Histogram, pingFunc: () -> Unit) {
    timer(daemon = true, period = 10000) {
        timer.time {
            try {
                pingFunc()
            } catch (err: Exception) {
                log.error("Failed to ping webservice", err)
            }
        }
    }
}
