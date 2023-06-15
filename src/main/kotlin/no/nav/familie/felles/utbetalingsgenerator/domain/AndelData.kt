package no.nav.familie.felles.utbetalingsgenerator.domain

import java.time.YearMonth

data class AndelData(
    val id: Long,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val personIdent: String,
    val type: YtelseType,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long?,
)

enum class YtelseType(val klassifisering: String) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),

    SMÅBARNSTILLEGG("BATRSMA"),
    OVERGANGSSTØNAD("EFOG"),
    BARNETILSYN("EFBT"),
    SKOLEPENGER("EFSP"),
}

internal fun List<AndelData>.uten0beløp(): List<AndelData> = this.filter { it.beløp != 0 }
