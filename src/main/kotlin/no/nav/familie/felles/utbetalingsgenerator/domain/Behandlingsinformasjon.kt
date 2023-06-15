package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate
import java.time.YearMonth

/**
 * @param utbetalesTil I tilfeller der eks mottaker er institusjon, så kan man sende med en annen ident som beløpet utbetales til
 */
data class Behandlingsinformasjon(
    val saksbehandlerId: String,
    val behandlingId: Long,
    val fagsakId: Long,
    val fagsystem: Ytelsestype,
    val personIdent: String,
    val vedtaksdato: LocalDate,
    val erSimulering: Boolean,
    val opphørFra: YearMonth?,
    val utbetalesTil: String? = null,
)
