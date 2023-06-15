package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.BeståendeAndelerBeregner.finnBeståendeAndeler
import no.nav.familie.felles.utbetalingsgenerator.OppdragBeregnerUtil.validerAndeler
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.uten0beløp
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.time.YearMonth

class Utbetalingsgenerator {

    /**
     * Generer utbetalingsoppdrag som sendes til oppdrag
     *
     * @param sisteAndelPerKjede må sende inn siste andelen per kjede for å peke/opphøre riktig forrige andel
     * [sisteAndelPerKjede] brukes også for å utlede om utbetalingsoppdraget settes til NY eller ENDR
     *
     * @return [BeregnetUtbetalingsoppdrag] som inneholder både utbetalingsoppdraget og [BeregnetUtbetalingsoppdrag.andeler]
     * som inneholder periodeId/forrigePeriodeId for å kunne oppdatere andeler i basen
     */
    fun lagUtbetalingsoppdrag(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeAndeler: List<AndelData>,
        forrigeAndeler: List<AndelData>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
    ): BeregnetUtbetalingsoppdrag {
        validerAndeler(behandlingsinformasjon, forrigeAndeler, nyeAndeler)
        val nyeAndeler = nyeAndeler.groupByIdentOgType()
        val forrigeKjeder = forrigeAndeler.groupByIdentOgType()

        return lagUtbetalingsoppdrag(
            nyeAndeler = nyeAndeler,
            forrigeKjeder = forrigeKjeder,
            sisteAndelPerKjede = sisteAndelPerKjede,
            behandlingsinformasjon = behandlingsinformasjon,
        )
    }

    private fun lagUtbetalingsoppdrag(
        nyeAndeler: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): BeregnetUtbetalingsoppdrag {
        val nyeKjeder = lagNyeKjeder(nyeAndeler, forrigeKjeder, sisteAndelPerKjede, behandlingsinformasjon)

        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
            kodeEndring = kodeEndring(sisteAndelPerKjede),
            fagSystem = behandlingsinformasjon.fagsystem.kode,
            saksnummer = behandlingsinformasjon.fagsakId.toString(),
            aktoer = behandlingsinformasjon.personIdent,
            utbetalingsperiode = utbetalingsperioder(behandlingsinformasjon, nyeKjeder),
        )

        return BeregnetUtbetalingsoppdrag(
            utbetalingsoppdrag,
            lagAndelerMedPeriodeId(behandlingsinformasjon, nyeKjeder),
        )
    }

    private fun lagNyeKjeder(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): List<ResultatForKjede> {
        val alleIdentOgTyper = nyeKjeder.keys + forrigeKjeder.keys
        var sistePeriodeId = sisteAndelPerKjede.values.mapNotNull { it.periodeId }.maxOrNull() ?: -1
        return alleIdentOgTyper.map { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            val opphørsdato = finnOpphørsdato(forrigeAndeler, nyeAndeler, behandlingsinformasjon)

            val nyKjede = beregnNyKjede(
                forrigeAndeler.uten0beløp(),
                nyeAndeler.uten0beløp(),
                sisteAndel,
                sistePeriodeId,
                opphørsdato,
            )
            sistePeriodeId = nyKjede.sistePeriodeId
            nyKjede
        }
    }

    /**
     * Har tidligere valideret at opphørFra er <= andeler sitt fom
     * opphørFom opphører alle perioder, og kanskje lengre bak i tiden
     */
    private fun finnOpphørsdato(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): YearMonth? = behandlingsinformasjon.opphørFra ?: finnOpphørsdatoPga0Beløp(forrigeAndeler, nyeAndeler)

    /**
     * For å unngå unøvendig 0-sjekk senere, så sjekkes det for om man
     * må opphøre alle andeler pga nye 0-andeler som har startdato før forrige første periode
     */
    private fun finnOpphørsdatoPga0Beløp(forrigeAndeler: List<AndelData>, nyeAndeler: List<AndelData>): YearMonth? {
        val forrigeFørsteAndel = forrigeAndeler.firstOrNull()
        val nyFørsteAndel = nyeAndeler.firstOrNull()
        if (
            forrigeFørsteAndel != null && nyFørsteAndel != null &&
            nyFørsteAndel.beløp == 0 && nyFørsteAndel.fom < forrigeFørsteAndel.fom
        ) {
            return nyFørsteAndel.fom
        }
        return null
    }

    private fun utbetalingsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<Utbetalingsperiode> {
        val opphørsperioder = lagOpphørsperioder(behandlingsinformasjon, nyeKjeder.mapNotNull { it.opphørsandel })
        val nyePerioder = lagNyePerioder(behandlingsinformasjon, nyeKjeder.flatMap { it.nyeAndeler })
        return opphørsperioder + nyePerioder
    }

    private fun lagAndelerMedPeriodeId(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<AndelMedPeriodeId> = nyeKjeder.flatMap { nyKjede ->
        nyKjede.beståendeAndeler.map { AndelMedPeriodeId(it) } + nyKjede.nyeAndeler.map {
            AndelMedPeriodeId(it, behandlingsinformasjon.behandlingId)
        }
    }

    // Hos økonomi skiller man på endring på oppdragsnivå 110 og på linjenivå 150 (periodenivå).
    // Da de har opplevd å motta
    // UEND på oppdrag som skulle vært ENDR anbefaler de at kun ENDR brukes når sak
    // ikke er ny, så man slipper å forholde seg til om det er endring over 150-nivå eller ikke.
    private fun kodeEndring(sisteAndelMedPeriodeId: Map<IdentOgType, AndelData>) =
        if (sisteAndelMedPeriodeId.isEmpty()) KodeEndring.NY else KodeEndring.ENDR

    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        periodeId: Long,
        opphørsdato: YearMonth?,
    ): ResultatForKjede {
        val beståendeAndeler = finnBeståendeAndeler(forrige, nye, opphørsdato)
        val nyeAndeler = nye.subList(beståendeAndeler.andeler.size, nye.size)

        val (nyeAndelerMedPeriodeId, gjeldendePeriodeId) = nyeAndelerMedPeriodeId(nyeAndeler, periodeId, sisteAndel)
        return ResultatForKjede(
            beståendeAndeler = beståendeAndeler.andeler,
            nyeAndeler = nyeAndelerMedPeriodeId,
            opphørsandel = beståendeAndeler.opphørFra?.let {
                Pair(sisteAndel ?: error("Må ha siste andel for å kunne opphøre"), it)
            },
            sistePeriodeId = gjeldendePeriodeId,
        )
    }

    private fun nyeAndelerMedPeriodeId(
        nyeAndeler: List<AndelData>,
        periodeId: Long,
        sisteAndel: AndelData?,
    ): Pair<List<AndelData>, Long> {
        var gjeldendePeriodeId = periodeId
        var forrigePeriodeId = sisteAndel?.periodeId
        val nyeAndelerMedPeriodeId = nyeAndeler.mapIndexed { index, andelData ->
            gjeldendePeriodeId += 1
            val nyAndel = andelData.copy(periodeId = gjeldendePeriodeId, forrigePeriodeId = forrigePeriodeId)
            forrigePeriodeId = gjeldendePeriodeId
            nyAndel
        }
        return Pair(nyeAndelerMedPeriodeId, gjeldendePeriodeId)
    }

    private fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> =
        groupBy { IdentOgType(it.personIdent, it.type) }.mapValues { it.value.sortedBy { it.fom } }

    private fun lagOpphørsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<Pair<AndelData, YearMonth>>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(
            behandlingsinformasjon = behandlingsinformasjon,
            erEndringPåEksisterendePeriode = true,
        )

        return andeler.map {
            utbetalingsperiodeMal.lagPeriodeFraAndel(it.first, opphørKjedeFom = it.second)
        }
    }

    private fun lagNyePerioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<AndelData>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(behandlingsinformasjon = behandlingsinformasjon)
        return andeler.map { utbetalingsperiodeMal.lagPeriodeFraAndel(it) }
    }
}
