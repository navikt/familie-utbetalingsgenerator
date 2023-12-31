package no.nav.familie.felles.utbetalingsgenerator.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * @param opphørAlleKjederFra Kan brukes når man ønsker å oppøre bak i tiden, før man selv var master,
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
    val fagsystem: Fagsystem,
    val personIdent: String,
    val vedtaksdato: LocalDate,
    val opphørAlleKjederFra: YearMonth?,
    val utbetalesTil: String? = null,
    val erGOmregning: Boolean = false, // Kan fjernes? Vi sender brev nå
    val opphørKjederFraFørsteUtbetaling: Boolean = false,
)

interface Fagsystem {
    val kode: String
    val gyldigeSatstyper: Set<Ytelsestype>
}
