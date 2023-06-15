package no.nav.familie.felles.utbetalingsgenerator.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.cucumber.ValideringUtil.assertSjekkBehandlingIder
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.DomenebegrepBehandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.ForventetUtbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.ForventetUtbetalingsperiode
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.OppdragParser
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.OppdragParser.mapAndeler
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.parseValgfriÅrMåned
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.uten0beløp
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class OppdragSteg {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val utbetalingsgenerator = Utbetalingsgenerator()

    private var behandlingsinformasjon = mutableMapOf<Long, Behandlingsinformasjon>()
    private var andelerPerBehandlingId = mapOf<Long, List<AndelData>>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, BeregnetUtbetalingsoppdrag>()

    @Gitt("følgende behandlingsinformasjon")
    fun følgendeBehandlinger(dataTable: DataTable) {
        opprettBehandlingsinformasjon(dataTable)
    }

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlingsinformasjonForDeSomMangler(dataTable)
        andelerPerBehandlingId = mapAndeler(dataTable)
        if (andelerPerBehandlingId.flatMap { it.value }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        andelerPerBehandlingId.entries.fold(emptyList<Pair<Long, List<AndelData>>>()) { acc, andelPåBehandlingId ->
            val behandlingId = andelPåBehandlingId.key
            try {
                val beregnUtbetalingsoppdrag = beregnUtbetalingsoppdrag(acc, andelPåBehandlingId)
                beregnetUtbetalingsoppdrag[behandlingId] = beregnUtbetalingsoppdrag
                val oppdaterteAndeler = oppdaterAndelerMedPeriodeId(beregnUtbetalingsoppdrag, andelPåBehandlingId)

                acc + (behandlingId to oppdaterteAndeler)
            } catch (e: Throwable) {
                logger.error("Feilet beregning av oppdrag for behandling=$behandlingId")
                throw e
            }
        }
    }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(dataTable, beregnetUtbetalingsoppdrag)
        assertSjekkBehandlingIder(dataTable, beregnetUtbetalingsoppdrag)
    }

    private fun opprettBehandlingsinformasjon(dataTable: DataTable) {
        dataTable.groupByBehandlingId().map { (behandlingId, rader) ->
            val rad = rader.single()

            behandlingsinformasjon[behandlingId] = lagBehandlingsinformasjon(
                behandlingId = behandlingId,
                opphørFra = parseValgfriÅrMåned(DomenebegrepBehandlingsinformasjon.OPPHØR_FRA, rad),
            )
        }
    }

    private fun genererBehandlingsinformasjonForDeSomMangler(dataTable: DataTable) {
        dataTable.groupByBehandlingId().forEach { (behandlingId, _) ->
            if (!behandlingsinformasjon.containsKey(behandlingId)) {
                behandlingsinformasjon.put(
                    behandlingId,
                    lagBehandlingsinformasjon(behandlingId),
                )
            }
        }
    }

    private fun lagBehandlingsinformasjon(
        behandlingId: Long,
        opphørFra: YearMonth? = null,
    ) = Behandlingsinformasjon(
        saksbehandlerId = "saksbehandlerId",
        behandlingId = behandlingId,
        fagsakId = 1L,
        fagsystem = Ytelsestype.BARNETRYGD,
        personIdent = "1",
        vedtaksdato = LocalDate.now(),
        opphørFra = opphørFra,
        utbetalesTil = null,
    )

    private fun beregnUtbetalingsoppdrag(
        acc: List<Pair<Long, List<AndelData>>>,
        andeler: Map.Entry<Long, List<AndelData>>,
    ): BeregnetUtbetalingsoppdrag {
        val forrigeKjeder = acc.lastOrNull()?.second ?: emptyList()
        val behandlingId = andeler.key
        val sisteOffsetPerIdent = gjeldendeForrigeOffsetForKjede(acc)
        return utbetalingsgenerator.lagUtbetalingsoppdrag(
            behandlingsinformasjon = behandlingsinformasjon.getValue(behandlingId),
            nyeAndeler = andeler.value,
            forrigeAndeler = forrigeKjeder,
            sisteAndelPerKjede = sisteOffsetPerIdent,
        )
    }

    private fun gjeldendeForrigeOffsetForKjede(forrigeKjeder: List<Pair<Long, List<AndelData>>>): Map<IdentOgType, AndelData> {
        return forrigeKjeder.flatMap { it.second }
            .uten0beløp()
            .groupBy { IdentOgType(it.personIdent, it.type) }
            .mapValues { it.value.maxBy { it.periodeId!! } }
    }

    private fun oppdaterAndelerMedPeriodeId(
        beregnUtbetalingsoppdrag: BeregnetUtbetalingsoppdrag,
        andelPåBehandlingId: Map.Entry<Long, List<AndelData>>,
    ): List<AndelData> {
        val andelerPerId = beregnUtbetalingsoppdrag.andeler.associateBy { it.id }
        return andelPåBehandlingId.value.map {
            if (it.beløp == 0) {
                it
            } else {
                val andelMedPeriodeId = andelerPerId[it.id]!!
                it.copy(
                    periodeId = andelMedPeriodeId.periodeId,
                    forrigePeriodeId = andelMedPeriodeId.forrigePeriodeId,
                    kildeBehandlingId = andelMedPeriodeId.kildeBehandlingId,
                )
            }
        }
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, BeregnetUtbetalingsoppdrag>,
    ) {
        val forventedeUtbetalingsoppdrag = OppdragParser.mapForventetUtbetalingsoppdrag(dataTable)
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag = beregnetUtbetalingsoppdrag[behandlingId]
                ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag.utbetalingsoppdrag)
            } catch (e: Throwable) {
                logger.error("Feilet validering av behandling $behandlingId")
                throw e
            }
        }
    }

    private fun assertUtbetalingsoppdrag(
        forventetUtbetalingsoppdrag: ForventetUtbetalingsoppdrag,
        utbetalingsoppdrag: Utbetalingsoppdrag,
    ) {
        assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(forventetUtbetalingsoppdrag.kodeEndring)
        assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(forventetUtbetalingsoppdrag.utbetalingsperiode.size)
        forventetUtbetalingsoppdrag.utbetalingsperiode.forEachIndexed { index, forventetUtbetalingsperiode ->
            val utbetalingsperiode = utbetalingsoppdrag.utbetalingsperiode[index]
            try {
                assertUtbetalingsperiode(utbetalingsperiode, forventetUtbetalingsperiode)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad $index for oppdrag=${forventetUtbetalingsoppdrag.behandlingId}")
                throw e
            }
        }
    }
}

private fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode,
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .`as`("erEndringPåEksisterendePeriode")
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering)
        .`as`("klassifisering")
        .isEqualTo(forventetUtbetalingsperiode.ytelse.klassifisering)
    assertThat(utbetalingsperiode.periodeId)
        .`as`("periodeId")
        .isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId)
        .`as`("forrigePeriodeId")
        .isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt())
        .`as`("sats")
        .isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType)
        .`as`("satsType")
        .isEqualTo(Utbetalingsperiode.SatsType.MND)
    assertThat(utbetalingsperiode.vedtakdatoFom)
        .`as`("fom")
        .isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom)
        .`as`("tom")
        .isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom)
        .`as`("opphør")
        .isEqualTo(forventetUtbetalingsperiode.opphør)
    forventetUtbetalingsperiode.kildebehandlingId?.let {
        assertThat(utbetalingsperiode.behandlingId)
            .`as`("kildebehandlingId")
            .isEqualTo(forventetUtbetalingsperiode.kildebehandlingId)
    }
}
