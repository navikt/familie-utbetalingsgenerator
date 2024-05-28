package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Lager mal for generering av utbetalingsperioder med tilpasset setting av verdier basert på parametre
 *
 * @param[vedtak] for vedtakdato og opphørsdato hvis satt
 * @param[erEndringPåEksisterendePeriode] ved true vil oppdrag sette asksjonskode ENDR på linje og ikke referere bakover
 * @return mal med tilpasset lagPeriodeFraAndel
 */
internal data class UtbetalingsperiodeMal(
    val behandlingsinformasjon: Behandlingsinformasjon,
    val erEndringPåEksisterendePeriode: Boolean = false,
) {
    /**
     * Lager utbetalingsperioder som legges på utbetalingsoppdrag. En utbetalingsperiode tilsvarer linjer hos økonomi
     *
     * Denne metoden brukes også til simulering og på dette tidspunktet er ikke vedtaksdatoen satt.
     * Derfor defaulter vi til now() når vedtaksdato mangler.
     *
     * @param[andel] andel som skal mappes til periode
     * @param[periodeIdOffset] brukes til å synce våre linjer med det som ligger hos økonomi
     * @param[forrigePeriodeIdOffset] peker til forrige i kjeden. Kun relevant når IKKE erEndringPåEksisterendePeriode
     * @param[opphørKjedeFom] fom-dato fra tidligste periode i kjede med endring
     * @return Periode til utbetalingsoppdrag
     */
    fun lagPeriodeFraAndel(
        andel: AndelData,
        opphørKjedeFom: YearMonth? = null,
    ): Utbetalingsperiode =
        Utbetalingsperiode(
            erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
            opphør =
                if (erEndringPåEksisterendePeriode) {
                    val opphørDatoFom =
                        opphørKjedeFom?.førsteDagIInneværendeMåned()
                            ?: error("Mangler opphørsdato for kjede")
                    Opphør(opphørDatoFom)
                } else {
                    null
                },
            forrigePeriodeId = andel.forrigePeriodeId,
            periodeId = andel.periodeId ?: error("Mangler periodeId på andel=${andel.id}"),
            datoForVedtak = behandlingsinformasjon.vedtaksdato,
            klassifisering = andel.type.klassifisering,
            vedtakdatoFom = andel.fom.førsteDagIInneværendeMåned(),
            vedtakdatoTom = andel.tom.sisteDagIInneværendeMåned(),
            sats = BigDecimal(andel.beløp),
            satsType = andel.type.satsType,
            utbetalesTil = behandlingsinformasjon.utbetalesTil ?: behandlingsinformasjon.personIdent,
            behandlingId = behandlingsinformasjon.eksternBehandlingId,
            utbetalingsgrad = andel.utbetalingsgrad,
        )

    private fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)

    private fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()
}
