package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

internal object OppdragBeregnerUtil {

    fun validerAndeler(
        behandlingsinformasjon: Behandlingsinformasjon,
        forrige: List<AndelData>,
        nye: List<AndelData>,
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
        validereTyperForYtelse(forrige, nye, behandlingsinformasjon)
    }

    private fun validereTyperForYtelse(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ) {
        val typer = (forrige.map { it.type } + nye.map { it.type })
        val gyldigeTyper = when (behandlingsinformasjon.ytelse) {
            Ytelsestype.BARNETRYGD -> BARNETRYGD_GYLDIGE_TYPER
            Ytelsestype.OVERGANGSSTØNAD -> OVERGANGSSTØNAD_GYLDIGE_TYPER
            Ytelsestype.BARNETILSYN -> BARNETILSYN_GYLDIGE_TYPER
            Ytelsestype.SKOLEPENGER -> SKOLEPENGER_GYLDIGE_TYPER
            Ytelsestype.KONTANTSTØTTE -> KONTANTSTØTTE_GYLDIGE_TYPER
        }
        if (!gyldigeTyper.containsAll(typer)) {
            error("Forrige og nye typer inneholder typene=$typer men tillater kun $gyldigeTyper")
        }
    }

    private val BARNETRYGD_GYLDIGE_TYPER = setOf(
        YtelseType.ORDINÆR_BARNETRYGD,
        YtelseType.UTVIDET_BARNETRYGD,
        YtelseType.SMÅBARNSTILLEGG,
    )

    private val OVERGANGSSTØNAD_GYLDIGE_TYPER = setOf(
        YtelseType.OVERGANGSSTØNAD,
    )

    private val BARNETILSYN_GYLDIGE_TYPER = setOf(
        YtelseType.BARNETILSYN,
    )

    private val SKOLEPENGER_GYLDIGE_TYPER = setOf(
        YtelseType.SKOLEPENGER,
    )

    private val KONTANTSTØTTE_GYLDIGE_TYPER = setOf<YtelseType>() // TODO
}
