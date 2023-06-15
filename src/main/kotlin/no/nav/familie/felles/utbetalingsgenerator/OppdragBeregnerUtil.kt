package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon

internal object OppdragBeregnerUtil {

    fun validerAndeler(
        behandlingsinformasjon: Behandlingsinformasjon,
        forrige: List<AndelData>,
        nye: List<AndelData>,
    ) {
        val forrigeUtenNullbeløp = forrige.filter { it.beløp != 0 }
        val id = forrigeUtenNullbeløp.map { it.id } + nye.map { it.id }
        if (id.size != id.toSet().size) {
            error("Inneholder duplikat av id'er")
        }
        forrigeUtenNullbeløp.find { it.periodeId == null }
            ?.let { error("Tidligere andel=${it.id} mangler offset") }

        nye.find { it.periodeId != null || it.forrigePeriodeId != null }
            ?.let { error("Ny andel=${it.id} inneholder periodeId/forrigePeriodeId") }

        behandlingsinformasjon.opphørFra?.let { opphørFra ->
            forrige.find { it.fom < opphørFra }
                ?.let { error("Kan ikke sende inn opphørFra=$opphørFra som er etter andel=${it.id} sitt fom=${it.fom}") }
        }
    }
}
