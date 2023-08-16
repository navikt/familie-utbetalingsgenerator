package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate
import java.time.YearMonth

/**
 * @param opphørFra Kan brukes når man ønsker å oppøre bak i tiden, før man selv var master,
 * eller ved simulering når (BA) ønsker å simulere alt på nytt. Den skal ikke settes hvis man ikke har noen kjede fra tidligere
 *
 * @param utbetalesTil I tilfeller der eks mottaker er institusjon, så kan man sende med en annen ident som beløpet utbetales til
 *
 * @param erGOmregning er flagg for overgangsstønad som setter et flagg på utbetalingsoppdraget
 */
data class Behandlingsinformasjon(
    val saksbehandlerId: String,
    val behandlingId: String,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val ytelse: Ytelsestype,
    val personIdent: String,
    val vedtaksdato: LocalDate,
    val opphørFra: YearMonth?,
    val utbetalesTil: String? = null,
    val erGOmregning: Boolean = false, // Kan fjernes? Vi sender brev nå
)
