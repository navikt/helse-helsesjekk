package no.nav.helse

data class Environment(val map: Map<String, String> = System.getenv()) {

    val securityTokenServiceEndpointUrl: String = envVar("SECURITY_TOKEN_SERVICE_URL")
    val securityTokenUsername: String = envVar("SECURITY_TOKEN_SERVICE_USERNAME")
    val securityTokenPassword: String = envVar("SECURITY_TOKEN_SERVICE_PASSWORD")

    val arbeidsfordelingEndpointUrl: String = envVar("ARBEIDSFORDELING_ENDPOINTURL")
    val arbeidsforholdEndpointUrl:String = envVar("AAREG_ENDPOINTURL")
    val infotrygdBeregningsgrunnlagEndpointUrl: String = envVar("INFOTRYGD_BEREGNINGSGRUNNLAG_ENDPOINTURL")
    val infotrygdSakEndpointUrl: String = envVar("INFOTRYGD_SAK_ENDPOINTURL")
    val inntektEndpointUrl: String = envVar("INNTEKT_ENDPOINTURL")
    val medlemskapEndpointUrl: String = envVar("MEDLEMSKAP_ENDPOINT_URL")
    val meldekortUtbetalingsgrunnlagEndpointUrl: String = envVar("MELDEKORT_UTBETALINGSGRUNNLAG_ENDPOINTURL")
    val organisasjonEndpointUrl: String = envVar("ORGANISASJON_ENDPOINTURL")
    val personEndpointUrl: String = envVar("PERSON_ENDPOINTURL")
    val sykepengerEndpointUrl: String = envVar("SYKEPENGER_ENDPOINTURL")

    private fun envVar(key: String): String {
        return map[key] ?: throw RuntimeException("Missing required variable \"$key\"")
    }
}
