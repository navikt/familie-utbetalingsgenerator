package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.uten0beløp

internal object OppdragBeregnerUtil {

    fun validerAndeler(
        behandlingsinformasjon: Behandlingsinformasjon,
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
    ) {
        val forrigeUtenNullbeløp = forrige.filter { it.beløp != 0 }
        val idn = forrigeUtenNullbeløp.map { it.id } + nye.map { it.id }
        if (idn.size != idn.toSet().size) {
            error("Inneholder duplikat av id'er")
        }

        forrigeUtenNullbeløp.find { it.periodeId == null }
            ?.let { error("Tidligere andel=${it.id} mangler periodeId") }

        nye.find { it.periodeId != null || it.forrigePeriodeId != null }
            ?.let { error("Ny andel=${it.id} inneholder periodeId/forrigePeriodeId") }

        behandlingsinformasjon.opphørFra?.let { opphørFra ->
            forrige.find { it.fom < opphørFra }
                ?.let { error("Ugyldig opphørFra=$opphørFra som er etter andel=${it.id} sitt fom=${it.fom}") }
        }
        if (behandlingsinformasjon.opphørKjederFraFørsteUtbetaling && behandlingsinformasjon.opphørFra != null) {
            error("Kan ikke sette opphørKjederFraFørsteUtbetaling til true samtidig som opphørFra er satt")
        }
        if (sisteAndelPerKjede.isEmpty() && behandlingsinformasjon.opphørFra != null) {
            error("Kan ikke sende med opphørFra når det ikke finnes noen kjede fra tidligere")
        }
        if (sisteAndelPerKjede.isEmpty() && forrige.uten0beløp().isNotEmpty()) {
            error("Mangler sisteAndelPerKjede når det finnes andeler fra før")
        }

        validereTyperForYtelse(forrige, nye, behandlingsinformasjon)
    }

    private fun validereTyperForYtelse(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ) {
        val typer = (forrige.map { it.type } + nye.map { it.type })
        val gyldigeTyper = behandlingsinformasjon.fagsystem.gyldigeSatstyper
        if (!gyldigeTyper.containsAll(typer)) {
            error("Forrige og nye typer inneholder typene=$typer men tillater kun $gyldigeTyper")
        }
    }
}
