package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.OppdragBeregnerUtil.validerAndeler
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class OppdragBeregnerUtilTest {

    private val tomSisteAndelPerKjede = emptyMap<IdentOgType, AndelData>()
    private val sisteAndelPerKjede = mapOf(
        IdentOgType("", OVERGANGSSTØNAD) to lagAndel(id = "1", periodeId = 1, kildeBehandlingId = "1")
    )

    @Nested
    inner class HappyCase {

        @Test
        fun `skal kunne sende inn tomme lister`() {
            validerAndeler(
                lagBehandlingsinformasjon(),
                forrige = listOf(),
                nye = listOf(),
                tomSisteAndelPerKjede,
            )
        }

        @Test
        fun `forrige inneholer en andel og nye er tom liste`() {
            validerAndeler(
                lagBehandlingsinformasjon(),
                forrige = listOf(lagAndel(periodeId = 1, forrigePeriodeId = 0, kildeBehandlingId = "1")),
                nye = listOf(),
                sisteAndelPerKjede,
            )
        }

        @Test
        fun `forrige er tom, nye inneholder en andel`() {
            validerAndeler(
                lagBehandlingsinformasjon(),
                forrige = listOf(),
                nye = listOf(lagAndel()),
                tomSisteAndelPerKjede,
            )
        }
    }

    @Nested
    inner class IdDuplikat {

        @Test
        fun `kan ikke inneholde duplikat av idn i forrige`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(
                        lagAndel(id = "1", periodeId = 1, forrigePeriodeId = null, kildeBehandlingId = "1"),
                        lagAndel(id = "1", periodeId = 2, forrigePeriodeId = null, kildeBehandlingId = "1"),
                    ),
                    nye = listOf(),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Inneholder duplikat av id'er")
        }

        @Test
        fun `kan ikke inneholde duplikat av idn i nye`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(),
                    nye = listOf(lagAndel(id = "1"), lagAndel(id = "1")),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Inneholder duplikat av id'er")
        }

        @Test
        fun `kan ikke inneholde duplikat av idn tvers gamle og nye`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(
                        lagAndel(
                            id = "1",
                            periodeId = 1,
                            forrigePeriodeId = null,
                            kildeBehandlingId = "1",
                        ),
                    ),
                    nye = listOf(lagAndel(id = "1")),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Inneholder duplikat av id'er")
        }
    }

    @Nested
    inner class ForrigeAndeler {

        @Test
        fun `forrige må inneholde periodeId`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(lagAndel(periodeId = null, forrigePeriodeId = null, kildeBehandlingId = "1")),
                    nye = listOf(),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("mangler periodeId")
        }

        @Test
        fun `kan ikke ha tom siste andel per kjede når forrige inneholder andeler`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(
                        lagAndel(id = "1", periodeId = 1, forrigePeriodeId = null, kildeBehandlingId = "1")
                    ),
                    nye = listOf(),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Mangler sisteAndelPerKjede når det finnes andeler fra før")
        }

    }

    @Nested
    inner class NyeAndeler {

        @Test
        fun `kan ikke inneholde periodeId`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(),
                    nye = listOf(lagAndel(id = "1", periodeId = 1, forrigePeriodeId = null, kildeBehandlingId = "1")),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("inneholder periodeId/forrigePeriodeId")
        }

        @Test
        fun `kan ikke inneholde forrigePeriodeId`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(),
                    forrige = listOf(),
                    nye = listOf(lagAndel(id = "1", periodeId = null, forrigePeriodeId = 1, kildeBehandlingId = "1")),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("inneholder periodeId/forrigePeriodeId")
        }
    }

    @Nested
    inner class OpphørFra {

        @Test
        fun `kan ikke sende inn opphørFra hvis det ikke finnes en kjede fra før`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(opphørFra = YearMonth.now().minusMonths(1)),
                    forrige = listOf(),
                    nye = listOf(),
                    tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Kan ikke sende med opphørFra når det ikke finnes noen kjede fra tidligere")
        }

        @Test
        fun `kan sende inn opphørFra som er før forrige første periode`() {
            validerAndeler(
                lagBehandlingsinformasjon(opphørFra = YearMonth.now().minusMonths(1)),
                forrige = listOf(lagAndel(id = "1", periodeId = 1, forrigePeriodeId = null, kildeBehandlingId = "1")),
                nye = listOf(),
                sisteAndelPerKjede = sisteAndelPerKjede,
            )
        }

        @Test
        fun `kan sende inn opphørFra når det ikke finnes tidligere perioder`() {
            validerAndeler(
                lagBehandlingsinformasjon(opphørFra = YearMonth.now().minusMonths(1)),
                forrige = listOf(),
                nye = listOf(),
                sisteAndelPerKjede = sisteAndelPerKjede,
            )
        }

        @Test
        fun `kan ikke sende inn opphørFra etter forrigePeriode sitt første dato`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(opphørFra = YearMonth.now().plusMonths(1)),
                    forrige = listOf(
                        lagAndel(
                            id = "1",
                            periodeId = 1,
                            forrigePeriodeId = null,
                            kildeBehandlingId = "1",
                        ),
                    ),
                    nye = listOf(),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Ugyldig opphørFra")
        }
    }

    @Nested
    inner class Ytelsessjekk {

        @Test
        fun `skal ikke kunne sende inn en andel av type skolepenger på ytelse overgangsstnad`() {
            assertThatThrownBy {
                validerAndeler(
                    lagBehandlingsinformasjon(Ytelsestype.OVERGANGSSTØNAD),
                    forrige = listOf(),
                    nye = listOf(lagAndel(ytelseType = YtelseType.SKOLEPENGER)),
                    sisteAndelPerKjede = tomSisteAndelPerKjede,
                )
            }.hasMessageContaining("Forrige og nye typer inneholder typene")
        }
    }

    private fun lagAndel(
        id: String = "",
        ytelseType: YtelseType? = null,
        periodeId: Long? = null,
        forrigePeriodeId: Long? = null,
        kildeBehandlingId: String? = null,
        beløp: Int = 1,
    ) = AndelData(
        id = id,
        fom = YearMonth.now(),
        tom = YearMonth.now(),
        beløp = beløp,
        personIdent = "",
        type = ytelseType ?: OVERGANGSSTØNAD,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = kildeBehandlingId,
        utbetalingsgrad = null,
    )

    private fun lagBehandlingsinformasjon(
        ytelse: Ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
        opphørFra: YearMonth? = null,
    ) = Behandlingsinformasjon(
        saksbehandlerId = "saksbehandlerId",
        behandlingId = "1",
        eksternBehandlingId = 1L,
        eksternFagsakId = 1L,
        ytelse = ytelse,
        personIdent = "1",
        vedtaksdato = LocalDate.now(),
        opphørFra = opphørFra,
        utbetalesTil = null,
    )
}
