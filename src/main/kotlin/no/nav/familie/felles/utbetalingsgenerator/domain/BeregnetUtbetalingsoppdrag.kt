package no.nav.familie.felles.utbetalingsgenerator.domain

/**
 * @param andeler er alle andeler med nye periodeId/forrigePeriodeId for å kunne oppdatere lagrede andeler
 */
data class BeregnetUtbetalingsoppdrag(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val andeler: List<AndelMedPeriodeId>,
)

data class BeregnetUtbetalingsoppdragLongId(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val andeler: List<AndelMedPeriodeIdLongId>,
)

data class AndelMedPeriodeId(
    val id: String,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: String,
) {
    constructor(andel: AndelData, nyttKildeBehandlingId: String? = null) :
        this(
            id = andel.id,
            periodeId = andel.periodeId ?: error("Mangler offset på andel=${andel.id}"),
            forrigePeriodeId = andel.forrigePeriodeId,
            kildeBehandlingId =
                nyttKildeBehandlingId ?: andel.kildeBehandlingId
                    ?: error("Mangler kildebehandlingId på andel=${andel.id}"),
        )

    internal fun tilAndelMedPeriodeIdMedLongId() =
        AndelMedPeriodeIdLongId(
            id = id.toLong(),
            periodeId = periodeId,
            forrigePeriodeId = forrigePeriodeId,
            kildeBehandlingId = kildeBehandlingId.toLong(),
        )
}

data class AndelMedPeriodeIdLongId(
    val id: Long,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long,
)
