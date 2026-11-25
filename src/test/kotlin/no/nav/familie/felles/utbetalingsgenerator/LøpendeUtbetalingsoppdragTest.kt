package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class LøpendeUtbetalingsoppdragTest {
    private val januar = YearMonth.of(2026, 1)
    private val februar = januar.plusMonths(1)
    private val behandlingsinformasjon =
        Behandlingsinformasjon(
            saksbehandlerId = "saksbehandler",
            behandlingId = "2",
            eksternBehandlingId = 2,
            eksternFagsakId = 1,
            fagsystem = TestFagsystem.OVERGANGSSTØNAD,
            personIdent = "1",
            vedtaksdato = LocalDate.of(2026, 1, 1),
            opphørAlleKjederFra = null,
        )

    @Test
    fun `ny løpende andel gir utbetalingsperiode uten sluttdato`() {
        val beregnetOppdrag = beregn(listOf(lagAndel("ny")))

        val utbetalingsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(januar.atDay(1))
        assertThat(utbetalingsperiode.vedtakdatoTom).isNull()
        assertThat(utbetalingsperiode.opphør).isNull()
        assertThat(beregnetOppdrag.andeler.single().periodeId).isZero()
    }

    @Test
    fun `uendret løpende andel beholder kjedefeltene uten nytt oppdrag`() {
        val forrigeAndel = lagForrigeAndel()
        val beregnetOppdrag = beregn(listOf(lagAndel("ny")), forrigeAndel)

        assertThat(beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode).isEmpty()
        assertThat(beregnetOppdrag.andeler.single().periodeId).isEqualTo(4)
        assertThat(beregnetOppdrag.andeler.single().kildeBehandlingId).isEqualTo("1")
    }

    @Test
    fun `løpende andel kan avkortes og opphøres fra måneden etter ny sluttdato`() {
        val beregnetOppdrag = beregn(listOf(lagAndel("ny", tom = januar)), lagForrigeAndel())

        val opphørsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(opphørsperiode.erEndringPåEksisterendePeriode).isTrue()
        assertThat(opphørsperiode.vedtakdatoTom).isNull()
        assertThat(opphørsperiode.opphør?.opphørDatoFom).isEqualTo(februar.atDay(1))
        assertThat(opphørsperiode.periodeId).isEqualTo(4)
        assertThat(beregnetOppdrag.andeler.single().periodeId).isEqualTo(4)
        assertThat(beregnetOppdrag.andeler.single().kildeBehandlingId).isEqualTo("2")
    }

    @Test
    fun `avsluttet andel kan forlenges til løpende`() {
        val beregnetOppdrag = beregn(listOf(lagAndel("ny")), lagForrigeAndel().copy(tom = januar))

        val utbetalingsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(utbetalingsperiode.vedtakdatoTom).isNull()
        assertThat(utbetalingsperiode.opphør).isNull()
        assertThat(utbetalingsperiode.periodeId).isEqualTo(5)
        assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(4)
    }

    @Test
    fun `ny sats fra februar avkorter løpende andel og kjeder ny løpende andel`() {
        val beregnetOppdrag =
            beregn(
                listOf(lagAndel("januar", tom = januar), lagAndel("februar", fom = februar, beløp = 200)),
                lagForrigeAndel(),
            )

        val utbetalingsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(februar.atDay(1))
        assertThat(utbetalingsperiode.vedtakdatoTom).isNull()
        assertThat(utbetalingsperiode.sats).isEqualByComparingTo("200")
        assertThat(utbetalingsperiode.opphør).isNull()
        assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(4)
        assertThat(beregnetOppdrag.andeler.map { it.periodeId }).containsExactly(4L, 5L)
    }

    @Test
    fun `fjerning av løpende andel opphører hele perioden`() {
        val beregnetOppdrag = beregn(emptyList(), lagForrigeAndel())

        val opphørsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(opphørsperiode.vedtakdatoTom).isNull()
        assertThat(opphørsperiode.opphør?.opphørDatoFom).isEqualTo(januar.atDay(1))
        assertThat(beregnetOppdrag.andeler).isEmpty()
    }

    @Test
    fun `gjentatt avkorting bruker opprinnelig løpende periode i opphøret`() {
        val opprinneligAndel = lagForrigeAndel()
        val beregnetOppdrag =
            beregn(
                listOf(lagAndel("ny", tom = januar)),
                opprinneligAndel.copy(tom = februar),
                opprinneligAndel,
            )

        val opphørsperiode = beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode.single()
        assertThat(opphørsperiode.vedtakdatoTom).isNull()
        assertThat(opphørsperiode.opphør?.opphørDatoFom).isEqualTo(februar.atDay(1))
        assertThat(opphørsperiode.periodeId).isEqualTo(4)
    }

    @Test
    fun `long-id grensesnittet støtter løpende andeler`() {
        val beregnetOppdrag =
            Utbetalingsgenerator().lagUtbetalingsoppdrag(
                behandlingsinformasjon,
                nyeAndeler = listOf(AndelDataLongId(1, januar, null, 100, "1", TestYtelsestype.OVERGANGSSTØNAD, null, null, null)),
                forrigeAndeler = emptyList<AndelDataLongId>(),
                sisteAndelPerKjede = emptyMap<IdentOgType, AndelDataLongId>(),
            )

        assertThat(
            beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoTom,
        ).isNull()
        assertThat(beregnetOppdrag.andeler.single().id).isEqualTo(1)
    }

    private fun beregn(
        nyeAndeler: List<AndelData>,
        forrigeAndel: AndelData? = null,
        sisteAndel: AndelData? = forrigeAndel,
    ) = Utbetalingsgenerator().lagUtbetalingsoppdrag(
        behandlingsinformasjon,
        nyeAndeler,
        listOfNotNull(forrigeAndel),
        listOfNotNull(sisteAndel).associateBy { IdentOgType(it.personIdent, it.type) },
    )

    private fun lagForrigeAndel() = lagAndel("forrige").copy(periodeId = 4, forrigePeriodeId = 3, kildeBehandlingId = "1")

    private fun lagAndel(
        id: String,
        fom: YearMonth = januar,
        tom: YearMonth? = null,
        beløp: Int = 100,
    ) = AndelData(id, fom, tom, beløp, "1", TestYtelsestype.OVERGANGSSTØNAD, null, null, null)
}
