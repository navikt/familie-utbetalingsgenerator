package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.BeståendeAndelerBeregner.finnBeståendeAndeler
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BeståendeAndelerBeregnerTest {
    companion object {
        private val JAN = YearMonth.of(2021, 1)
        private val FEB = YearMonth.of(2021, 2)
        private val MARS = YearMonth.of(2021, 3)
        private val APRIL = YearMonth.of(2021, 4)
        private val MAI = YearMonth.of(2021, 5)
    }

    @Test
    fun `ingen endring mellom 2 andeler`() {
        val jan = lagAndel(JAN, JAN, 1, periodeId = 0)
        val forrige = listOf(jan)
        val ny = listOf(lagAndel(JAN, JAN, 1))

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).containsExactly(jan)
    }

    @Test
    fun `en ny andel`() {
        val forrige = listOf(lagAndel(JAN, JAN, 1, periodeId = 0))
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(FEB, FEB, 1),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).containsExactlyElementsOf(forrige)
    }

    @Test
    fun `fra 0 til 1 andel`() {
        val forrige = listOf<AndelData>()
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `en ny andel mellom tidligere perioder`() {
        val januar = lagAndel(JAN, JAN, 1, periodeId = 0)
        val forrige = listOf(
            januar,
            lagAndel(MARS, MARS, 1, periodeId = 1, forrigePeriodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(FEB, FEB, 1),
            lagAndel(MARS, MARS, 1),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).containsExactly(januar)
    }

    @Test
    fun `fjernet en andel mellom tidligere perioder`() {
        val januar = lagAndel(JAN, JAN, 1, periodeId = 0)
        val forrige = listOf(
            januar,
            lagAndel(FEB, FEB, 1, periodeId = 1, forrigePeriodeId = 0),
            lagAndel(MARS, MARS, 1, periodeId = 2, forrigePeriodeId = 1),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(MARS, MARS, 1),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(FEB)
        assertThat(beståendeAndeler.andeler).containsExactly(januar)
    }

    @Test
    fun `fjernet en andel`() {
        val januar = lagAndel(JAN, JAN, 1, periodeId = 0)
        val forrige = listOf(januar, lagAndel(FEB, FEB, 1, periodeId = 1, forrigePeriodeId = 0))
        val ny = listOf(lagAndel(JAN, JAN, 1))

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(FEB)
        assertThat(beståendeAndeler.andeler).containsExactly(januar)
    }

    @Test
    fun `avkortet en andel`() {
        val forrige = listOf(lagAndel(JAN, FEB, 1, periodeId = 0))
        val ny = listOf(lagAndel(JAN, JAN, 1))

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(FEB)
        assertThat(beståendeAndeler.andeler)
            .containsExactly(lagAndel(JAN, JAN, 1, periodeId = 0))
    }

    @Test
    fun `avkortet en lengre andel`() {
        val forrige = listOf(lagAndel(JAN, MAI, 1, periodeId = 0))
        val ny = listOf(lagAndel(JAN, FEB, 1))

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(MARS)
        assertThat(beståendeAndeler.andeler)
            .containsExactly(lagAndel(JAN, FEB, 1, periodeId = 0))
    }

    // TODO er dette et reell case?
    @Test
    fun `forlenget en andel`() {
        val forrige = listOf(lagAndel(JAN, JAN, 1, periodeId = 0))
        val ny = listOf(lagAndel(JAN, FEB, 1))

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `nytt beløp for periode`() {
        val forrige = listOf(
            lagAndel(JAN, FEB, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, FEB, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `avkortet periode og nytt beløp`() {
        val forrige = listOf(
            lagAndel(JAN, FEB, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `forlenget periode og nytt beløp`() {
        val forrige = listOf(
            lagAndel(JAN, JAN, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, FEB, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `nytt beløp fra feb`() {
        val forrige = listOf(
            lagAndel(JAN, FEB, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(FEB, FEB, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler)
            .containsExactly(lagAndel(JAN, JAN, 1, periodeId = 0))
    }

    @Test
    fun `første andelen får tidligere fom, og en ny andel avkorter`() {
        val forrige = listOf(
            lagAndel(FEB, MAI, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, MARS, 1),
            lagAndel(APRIL, MAI, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `får nytt beløp fra del av første perioden`() {
        val forrige = listOf(
            lagAndel(JAN, FEB, 1, periodeId = 0),
            lagAndel(MARS, APRIL, 1, periodeId = 1, forrigePeriodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(FEB, FEB, 2),
            lagAndel(MARS, APRIL, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler)
            .containsExactly(lagAndel(JAN, JAN, 1, periodeId = 0))
    }

    @Test
    fun `avkorter den første perioden og beholden den andre`() {
        val forrige = listOf(
            lagAndel(JAN, FEB, 1, periodeId = 0),
            lagAndel(MARS, APRIL, 1, periodeId = 1, forrigePeriodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(MARS, APRIL, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(FEB)
        assertThat(beståendeAndeler.andeler)
            .containsExactly(lagAndel(JAN, JAN, 1, periodeId = 0))
    }

    @Test
    fun `forlenger den første perioden og beholden den andre`() {
        val forrige = listOf(
            lagAndel(JAN, JAN, 1, periodeId = 0),
            lagAndel(MARS, APRIL, 1, periodeId = 1, forrigePeriodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, FEB, 1),
            lagAndel(MARS, APRIL, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isNull()
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `første andelen får senere fom, og en ny andel avkorter`() {
        val forrige = listOf(
            lagAndel(JAN, MAI, 1, periodeId = 0),
        )
        val ny = listOf(
            lagAndel(FEB, MARS, 1),
            lagAndel(APRIL, MAI, 2),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(JAN)
        assertThat(beståendeAndeler.andeler).isEmpty()
    }

    @Test
    fun `avkorter periode 2`() {
        val forrige = listOf(
            lagAndel(JAN, JAN, 1, periodeId = 0),
            lagAndel(FEB, MARS, 1, periodeId = 1, forrigePeriodeId = 0),
        )
        val ny = listOf(
            lagAndel(JAN, JAN, 1),
            lagAndel(FEB, FEB, 1),
        )

        val beståendeAndeler = finnBeståendeAndeler(forrige, ny, null)

        assertThat(beståendeAndeler.opphørFra).isEqualTo(MARS)
        assertThat(beståendeAndeler.andeler).containsExactly(
            lagAndel(JAN, JAN, 1, periodeId = 0),
            lagAndel(FEB, FEB, 1, periodeId = 1, forrigePeriodeId = 0),
        )
    }

    private fun lagAndel(
        fom: YearMonth,
        tom: YearMonth,
        beløp: Int,
        periodeId: Long? = null,
        forrigePeriodeId: Long? = null,
        ident: String = "12",
        type: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    ): AndelData {
        return AndelData(
            id = "0",
            fom = fom,
            tom = tom,
            beløp = beløp,
            personIdent = ident,
            type = type,
            periodeId = periodeId,
            forrigePeriodeId = forrigePeriodeId,
            kildeBehandlingId = null,
            utbetalingsgrad = null
        )
    }
}
