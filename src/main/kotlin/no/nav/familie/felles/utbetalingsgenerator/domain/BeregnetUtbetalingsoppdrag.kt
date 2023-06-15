package no.nav.familie.felles.utbetalingsgenerator.domain

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

/**
 * @param andeler er alle andeler med nye periodeId/forrigePeriodeId for å kunne oppdatere lagrede andeler
 */
data class BeregnetUtbetalingsoppdrag(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val andeler: List<AndelMedPeriodeId>,
)

data class AndelMedPeriodeId(
    val id: Long,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: Long,
) {

    constructor(andel: AndelData, nyttKildeBehandlingId: Long? = null) :
        this(
            id = andel.id,
            periodeId = andel.periodeId ?: error("Mangler offset på andel=${andel.id}"),
            forrigePeriodeId = andel.forrigePeriodeId,
            kildeBehandlingId = nyttKildeBehandlingId ?: andel.kildeBehandlingId
                ?: error("Mangler kildebehandlingId på andel=${andel.id}"),
        )
}
