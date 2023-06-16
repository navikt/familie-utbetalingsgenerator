package no.nav.familie.felles.utbetalingsgenerator.domain

import java.time.YearMonth

data class AndelData(
    val id: String,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val personIdent: String,
    val type: YtelseType,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: String?,
)

data class AndelDataLongId(
    val id: Long,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val personIdent: String,
    val type: YtelseType,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long?,
) {

    internal fun tilAndelData() = AndelData(
        id = id.toString(),
        fom = fom,
        tom = tom,
        beløp = beløp,
        personIdent = personIdent,
        type = type,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = kildeBehandlingId?.toString(),
    )
}

enum class YtelseType(val klassifisering: String) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),

    SMÅBARNSTILLEGG("BATRSMA"),
    OVERGANGSSTØNAD("EFOG"),
    BARNETILSYN("EFBT"),
    SKOLEPENGER("EFSP"),
}

internal fun List<AndelData>.uten0beløp(): List<AndelData> = this.filter { it.beløp != 0 }
