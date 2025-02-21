package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.BeståendeAndelerBeregner.finnBeståendeAndeler
import no.nav.familie.felles.utbetalingsgenerator.OppdragBeregnerUtil.validerAndeler
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.felles.utbetalingsgenerator.domain.groupByIdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.representererSammePeriodeSom
import no.nav.familie.felles.utbetalingsgenerator.domain.uten0beløp
import org.slf4j.LoggerFactory
import java.time.YearMonth

class Utbetalingsgenerator {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generer utbetalingsoppdrag som sendes til oppdrag
     *
     * @param sisteAndelPerKjede må sende inn siste andelen per kjede for å peke/opphøre riktig forrige andel
     * Siste andelen er første andelen med høyeste periodeId, per ident/type, dvs hvis man har avkortet en periode,
     * og fått et nytt tom, så skal man bruke den opprinnelige perioden for det periodeId'et
     * ex
     * SELECT * FROM (SELECT aty.id,
     *        row_number() OVER (PARTITION BY aty.type, aty.fk_aktoer_id ORDER BY aty.periode_offset DESC, x.opprettet_tid ASC) rn
     * FROM andel_tilkjent_ytelse aty) WHERE rn = 1
     *
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
        validerAndeler(behandlingsinformasjon, forrigeAndeler, nyeAndeler, sisteAndelPerKjede)
        val nyeAndelerGruppert = nyeAndeler.groupByIdentOgType()
        val forrigeKjeder = forrigeAndeler.groupByIdentOgType()

        return lagUtbetalingsoppdrag(
            nyeAndeler = nyeAndelerGruppert,
            forrigeKjeder = forrigeKjeder,
            sisteAndelPerKjede = sisteAndelPerKjede,
            behandlingsinformasjon = behandlingsinformasjon,
        )
    }

    fun lagUtbetalingsoppdrag(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeAndeler: List<AndelDataLongId>,
        forrigeAndeler: List<AndelDataLongId>,
        sisteAndelPerKjede: Map<IdentOgType, AndelDataLongId>,
    ): BeregnetUtbetalingsoppdragLongId {
        val beregnetUtbetalingsoppdrag =
            lagUtbetalingsoppdrag(
                behandlingsinformasjon = behandlingsinformasjon,
                nyeAndeler = nyeAndeler.map(AndelDataLongId::tilAndelData),
                forrigeAndeler = forrigeAndeler.map(AndelDataLongId::tilAndelData),
                sisteAndelPerKjede = sisteAndelPerKjede.mapValues { it.value.tilAndelData() },
            )
        return BeregnetUtbetalingsoppdragLongId(
            utbetalingsoppdrag = beregnetUtbetalingsoppdrag.utbetalingsoppdrag,
            andeler = beregnetUtbetalingsoppdrag.andeler.map(AndelMedPeriodeId::tilAndelMedPeriodeIdMedLongId),
        )
    }

    private fun lagUtbetalingsoppdrag(
        nyeAndeler: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): BeregnetUtbetalingsoppdrag {
        val nyeKjeder = lagNyeKjeder(nyeAndeler, forrigeKjeder, sisteAndelPerKjede, behandlingsinformasjon)

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
                kodeEndring = kodeEndring(sisteAndelPerKjede),
                fagSystem = behandlingsinformasjon.fagsystem.kode,
                saksnummer = behandlingsinformasjon.eksternFagsakId.toString(),
                aktoer = behandlingsinformasjon.personIdent,
                utbetalingsperiode = utbetalingsperioder(behandlingsinformasjon, nyeKjeder),
                gOmregning = behandlingsinformasjon.erGOmregning,
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
        return alleIdentOgTyper.sorted().map { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            val opphørsdato = finnOpphørsdato(forrigeAndeler, nyeAndeler, sisteAndel, behandlingsinformasjon)

            val nyKjede =
                beregnNyKjede(
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
     *
     * Hvis det ikke finnes en siste andel fra før, eks for BA som simulerer med en ny kjede,
     * så skal man returnere null då man ikke skal opphøre noe fra før
     */
    private fun finnOpphørsdato(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        sisteAndel: AndelData?,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): YearMonth? {
        if (sisteAndel == null) {
            return null
        }
        if (behandlingsinformasjon.opphørKjederFraFørsteUtbetaling) {
            return forrigeAndeler.uten0beløp().minOfOrNull { it.fom }
        }
        return behandlingsinformasjon.opphørAlleKjederFra ?: finnOpphørsdatoPga0Beløp(forrigeAndeler, nyeAndeler)
    }

    /**
     * For å unngå unøvendig 0-sjekk senere, så sjekkes det for om man
     * må opphøre alle andeler pga nye 0-andeler som har startdato før forrige første periode
     */
    private fun finnOpphørsdatoPga0Beløp(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ): YearMonth? {
        val forrigeFørsteAndel = forrigeAndeler.firstOrNull()
        val nyFørsteAndel = nyeAndeler.firstOrNull()
        if (
            forrigeFørsteAndel != null &&
            nyFørsteAndel != null &&
            nyFørsteAndel.beløp == 0 &&
            nyFørsteAndel.fom < forrigeFørsteAndel.fom
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
    ): List<AndelMedPeriodeId> =
        nyeKjeder.flatMap { nyKjede ->
            nyKjede.beståendeAndeler.map { beståendeAndel ->
                AndelMedPeriodeId(
                    andel = beståendeAndel,
                    nyKildeBehandlingId =
                        bestemKildeBehandlingIdForBeståendeAndel(
                            beståendeAndel = beståendeAndel,
                            opphørsandel = nyKjede.opphørsandel?.first,
                            inneværendeBehandlingId = behandlingsinformasjon.behandlingId,
                        ),
                )
            } +
                nyKjede.nyeAndeler.map { nyAndel ->
                    AndelMedPeriodeId(
                        andel = nyAndel,
                        nyKildeBehandlingId = behandlingsinformasjon.behandlingId,
                    )
                }
        }

    private fun bestemKildeBehandlingIdForBeståendeAndel(
        beståendeAndel: AndelData,
        opphørsandel: AndelData?,
        inneværendeBehandlingId: String,
    ): String? {
        val beståendeAndelErOpphørsandel = opphørsandel?.representererSammePeriodeSom(beståendeAndel) ?: false

        return if (beståendeAndelErOpphørsandel) {
            inneværendeBehandlingId
        } else {
            null
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
            opphørsandel =
                beståendeAndeler.opphørFra?.let {
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
        val nyeAndelerMedPeriodeId =
            nyeAndeler.map { andelData ->
                gjeldendePeriodeId += 1
                val nyAndel = andelData.copy(periodeId = gjeldendePeriodeId, forrigePeriodeId = forrigePeriodeId)
                forrigePeriodeId = gjeldendePeriodeId
                nyAndel
            }
        return Pair(nyeAndelerMedPeriodeId, gjeldendePeriodeId)
    }

    private fun lagOpphørsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<Pair<AndelData, YearMonth>>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal =
            UtbetalingsperiodeMal(
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
