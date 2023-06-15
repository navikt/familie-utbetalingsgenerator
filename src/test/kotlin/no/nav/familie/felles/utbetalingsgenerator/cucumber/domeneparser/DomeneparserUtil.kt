package no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser

import io.cucumber.datatable.DataTable

interface Domenenøkkel {
    val nøkkel: String
}

enum class Domenebegrep(override val nøkkel: String) : Domenenøkkel {
    ID("Id"),
    BEHANDLING_ID("BehandlingId"),
    FORRIGE_BEHANDLING_ID("ForrigeBehandlingId"),
    FRA_DATO("Fra dato"),
    TIL_DATO("Til dato"),
}

object DomeneparserUtil {
    fun DataTable.groupByBehandlingId(): Map<Long, List<Map<String, String>>> =
        this.asMaps().groupBy { rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }
}
