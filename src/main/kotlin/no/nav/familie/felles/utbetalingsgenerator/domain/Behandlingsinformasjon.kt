package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate
import java.time.YearMonth

/**
 * @param opphørFra Kan brukes når man ønsker å oppøre bak i tiden, før man selv var master,
 * eller ved simulering når (BA) ønsker å simulere alt på nytt
 * @param utbetalesTil I tilfeller der eks mottaker er institusjon, så kan man sende med en annen ident som beløpet utbetales til
 *
 * @param erGOmregning er flagg for overgangsstønad som setter et flagg på utbetalingsoppdraget
 * @param initPeriodeId pga at BA begynner på 0 og EF 1 så må EF sende med id som periodeId begynner på
 */
data class Behandlingsinformasjon(
    val saksbehandlerId: String,
    val behandlingId: String,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val fagsystem: Ytelsestype,
    val personIdent: String,
    val vedtaksdato: LocalDate,
    val opphørFra: YearMonth?,
    val utbetalesTil: String? = null,
    val erGOmregning: Boolean = false,
    val initPeriodeId: Int = 0,
) {

    init {
        if (initPeriodeId !in 0..1) {
            error("initPeriodeId=$initPeriodeId må være 0 eller 1")
        }
    }
}
