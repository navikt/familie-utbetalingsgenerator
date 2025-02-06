package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.felles.utbetalingsgenerator.TestYtelsestype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IdentOgTypeTest {
    @Test
    fun `skal sorteres etter klassifisering og ident`() {
        // Arrange
        val identOgTypeListe =
            listOf(
                IdentOgType("12345678912", TestYtelsestype.ORDINÆR_BARNETRYGD),
                IdentOgType("12345678910", TestYtelsestype.UTVIDET_BARNETRYGD),
                IdentOgType("12345678910", TestYtelsestype.ORDINÆR_BARNETRYGD),
                IdentOgType("12345678911", TestYtelsestype.ORDINÆR_BARNETRYGD),
            )
        val forventetSortertListe =
            listOf(
                IdentOgType("12345678910", TestYtelsestype.ORDINÆR_BARNETRYGD),
                IdentOgType("12345678911", TestYtelsestype.ORDINÆR_BARNETRYGD),
                IdentOgType("12345678912", TestYtelsestype.ORDINÆR_BARNETRYGD),
                IdentOgType("12345678910", TestYtelsestype.UTVIDET_BARNETRYGD),
            )
        // Act
        val sortertListe = identOgTypeListe.sorted()

        // Assert
        assertThat(sortertListe).containsExactlyElementsOf(forventetSortertListe)
    }
}
