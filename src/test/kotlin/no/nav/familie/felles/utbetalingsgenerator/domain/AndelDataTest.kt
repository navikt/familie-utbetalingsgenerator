package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.felles.utbetalingsgenerator.TestYtelsestype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class AndelDataTest {
    @Test
    fun `groupByIdentOgType skal gruppere ulike stønadstyper`() {
        val andel1 = andelData("1", TestYtelsestype.OVERGANGSSTØNAD)
        val andel2 = andelData("1", TestYtelsestype.OVERGANGSSTØNAD)
        val andel3 = andelData("2", TestYtelsestype.OVERGANGSSTØNAD)
        val andel4 = andelData("1", TestYtelsestype.SKOLEPENGER)

        val gruppertAndeler = listOf(andel1, andel2, andel3, andel4).groupByIdentOgType()

        assertThat(gruppertAndeler.keys).hasSize(3)
        assertThat(gruppertAndeler[IdentOgType("1", TestYtelsestype.OVERGANGSSTØNAD)])
            .containsExactly(andel1, andel2)
        assertThat(gruppertAndeler[IdentOgType("2", TestYtelsestype.OVERGANGSSTØNAD)])
            .containsExactly(andel3)
        assertThat(gruppertAndeler[IdentOgType("1", TestYtelsestype.SKOLEPENGER)])
            .containsExactly(andel4)
    }

    private fun andelData(
        ident: String,
        type: TestYtelsestype,
    ) = AndelData(
        id = UUID.randomUUID().toString(),
        fom = YearMonth.now(),
        tom = YearMonth.now(),
        beløp = 1,
        personIdent = ident,
        type = type,
        periodeId = null,
        forrigePeriodeId = null,
        kildeBehandlingId = null,
        utbetalingsgrad = null,
    )
}
