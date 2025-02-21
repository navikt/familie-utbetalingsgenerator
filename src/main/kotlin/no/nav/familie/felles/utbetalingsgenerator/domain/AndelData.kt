package no.nav.familie.felles.utbetalingsgenerator.domain

import java.time.YearMonth

/**
 * ID her burde ikke brukes til noe spesielt. EF har ikke et ID på andeler som sendes til utbetalingsgeneratorn
 * @param utbetalingsgrad skal kun brukes for overgangsstønad
 */
data class AndelData(
    val id: String,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val personIdent: String,
    val type: Ytelsestype,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: String?,
    val utbetalingsgrad: Int? = null,
)

fun AndelData.representererSammePeriodeSom(andelData: AndelData): Boolean =
    this.periodeId == andelData.periodeId && this.forrigePeriodeId == andelData.forrigePeriodeId && this.fom == andelData.fom

data class AndelDataLongId(
    val id: Long,
    val fom: YearMonth,
    val tom: YearMonth,
    val beløp: Int,
    val personIdent: String,
    val type: Ytelsestype,
    val periodeId: Long?,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long?,
) {
    internal fun tilAndelData() =
        AndelData(
            id = id.toString(),
            fom = fom,
            tom = tom,
            beløp = beløp,
            personIdent = personIdent,
            type = type,
            periodeId = periodeId,
            forrigePeriodeId = forrigePeriodeId,
            kildeBehandlingId = kildeBehandlingId?.toString(),
            utbetalingsgrad = null, // utbetalingsgrad er ikke i bruk av BA/KS
        )
}

interface Ytelsestype {
    val satsType: Utbetalingsperiode.SatsType
    val klassifisering: String
}

internal fun List<AndelData>.uten0beløp(): List<AndelData> = this.filter { it.beløp != 0 }

internal fun List<AndelData>.groupByIdentOgType(): Map<IdentOgType, List<AndelData>> =
    groupBy { IdentOgType(it.personIdent, it.type) }.mapValues { it.value.sortedBy { it.fom } }
