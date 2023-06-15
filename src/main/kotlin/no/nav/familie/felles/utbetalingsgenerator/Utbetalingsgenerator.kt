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

    fun lagUtbetalingsoppdrag(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeAndeler: List<AndelData>,
        forrigeAndeler: List<AndelData>?,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
    ): BeregnetUtbetalingsoppdrag {
        validerAndeler(behandlingsinformasjon, forrigeAndeler, nyeAndeler)
        val nyeKjeder = nyeAndeler.groupByIdentOgType()
        val forrigeKjeder = (forrigeAndeler ?: emptyList()).groupByIdentOgType()

        return lagUtbetalingsoppdrag(
            nyeKjeder = nyeKjeder,
            forrigeKjeder = forrigeKjeder,
            sisteAndelPerKjede = sisteAndelPerKjede,
            behandlingsinformasjon = behandlingsinformasjon,
            forrigeAndeler = forrigeAndeler,
        )
    }

    private fun lagUtbetalingsoppdrag(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
        forrigeAndeler: List<AndelData>?,
    ): BeregnetUtbetalingsoppdrag {
        /*
        TODO validering av endretSimuleringsdato, burde ikke kunne være etter min fom på nye andeler
        TODO erSimulering
         */
        val nyeKjeder = lagNyeKjeder(nyeKjeder, forrigeKjeder, sisteAndelPerKjede, behandlingsinformasjon)

        val utbetalingsoppdrag = Utbetalingsoppdrag(
            saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
            kodeEndring = kodeEndring(forrigeAndeler),
            fagSystem = behandlingsinformasjon.fagsystem.kode,
            saksnummer = behandlingsinformasjon.fagsakId.toString(),
            aktoer = behandlingsinformasjon.personIdent,
            utbetalingsperiode = utbetalingsperioder(behandlingsinformasjon, nyeKjeder),
        )

        return BeregnetUtbetalingsoppdrag(
            utbetalingsoppdrag,
            andelerMedPeriodeId(behandlingsinformasjon, nyeKjeder),
        )
    }

    private fun lagNyeKjeder(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): List<ResultatForKjede> {
        val alleIdentOgTyper = nyeKjeder.keys + forrigeKjeder.keys
        var sistePeriodeId = sisteAndelPerKjede.values.mapNotNull { it.periodeId }.maxOrNull()
        return alleIdentOgTyper.map { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            val opphørsdato = finnOpphørsdato(forrigeAndeler, nyeAndeler, behandlingsinformasjon)

            // TODO må nog sende med endretMigreringsDato/erSimulering? her og (eller opphørsdato?)
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
     * Må håndtere det at man simulererer / endrer migreringsdato / første andel begynner med 0-beløp
     * Hva skjer når det er kombinasjon av disse?
     * Hvordan håndterer man eks migrering av
     */
    private fun finnOpphørsdato(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): YearMonth? {
        // TODO erSImulering / endretMigreringsdato
        val førsteTidspunkVedSimulering = if (behandlingsinformasjon.erSimulering) {
            listOfNotNull(forrigeAndeler.firstOrNull(), nyeAndeler.firstOrNull()).minOfOrNull { it.fom }
        } else {
            null
        }
        val endretMigreringsDato =
            behandlingsinformasjon.opphørFra // kjedeEtterFørsteEndring.last() to (endretMigreringsDato ?: kjedeEtterFørsteEndring.first().stønadFom)
        val opphørsdatoPga0Beløp = finnOpphørsdatoPga0Beløp(forrigeAndeler, nyeAndeler)
        return listOfNotNull(opphørsdatoPga0Beløp, endretMigreringsDato, førsteTidspunkVedSimulering).minOrNull()
    }

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

    private fun andelerMedPeriodeId(
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
    private fun kodeEndring(forrigeAndeler: List<AndelData>?) =
        if (forrigeAndeler == null) KodeEndring.NY else KodeEndring.ENDR

    /**
     * Hvis vi har et opphørsdato så
     */
    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        periodeId: Long?,
        opphørHeleKjedenFra: YearMonth?,
    ): ResultatForKjede {
        return if (opphørHeleKjedenFra != null) {
            beregnNyKjedeMedOpphørsdato(forrige, nye, periodeId, sisteAndel, opphørHeleKjedenFra)
        } else {
            beregnNyKjede(forrige, nye, periodeId, sisteAndel)
        }
    }

    private fun beregnNyKjedeMedOpphørsdato(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        periodeId: Long?,
        sisteAndel: AndelData?,
        opphørsdato: YearMonth,
    ): ResultatForKjede {
        val (nyeAndelerMedPeriodeId, gjeldendePeriodeId) = nyeAndelerMedPeriodeId(nye, periodeId, sisteAndel)
        val opphørsandel = sisteAndel?.let {
            forrige.firstOrNull()?.let {
                // if (opphørsdato > it.fom) error("Opphørsdato=$opphørsdato må være før første andelen sitt fom=${it.fom}")
            }
            Pair(it, opphørsdato)
        }
        return ResultatForKjede(
            beståendeAndeler = emptyList(),
            nyeAndeler = nyeAndelerMedPeriodeId,
            opphørsandel = opphørsandel,
            sistePeriodeId = gjeldendePeriodeId,
        )
    }

    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        periodeId: Long?,
        sisteAndel: AndelData?,
    ): ResultatForKjede {
        val beståendeAndeler = finnBeståendeAndeler(forrige, nye)
        val nyeAndeler = nye.subList(beståendeAndeler.andeler.size, nye.size)

        val (nyeAndelerMedPeriodeId, gjeldendePeriodeId) = nyeAndelerMedPeriodeId(nyeAndeler, periodeId, sisteAndel)
        // TODO på en eller annen måte verifisere at periodeId har blitt satt?
        if (gjeldendePeriodeId < 0) {
            // gjeldendePeriodeId =
        }
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
        periodeId: Long?,
        sisteAndel: AndelData?,
    ): Pair<List<AndelData>, Long> {
        var gjeldendePeriodeId = periodeId ?: -1
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
