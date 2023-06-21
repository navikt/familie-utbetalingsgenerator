package no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.felles.utbetalingsgenerator.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

object OppdragParser {

    fun mapAndeler(dataTable: DataTable): Map<Long, List<AndelData>> {
        var index = 0L
        return dataTable.groupByBehandlingId().map { (behandlingId, rader) ->
            val andeler = parseAndelder(rader, index)
            index += andeler.size
            behandlingId to andeler
        }.toMap()
    }

    private fun parseAndelder(
        rader: List<Map<String, String>>,
        forrigeAndelId: Long,
    ): List<AndelData> {
        val erUtenAndeler = (parseValgfriBoolean(DomenebegrepAndeler.UTEN_ANDELER, rader.first()) ?: false)
        var andelId = forrigeAndelId
        return if (erUtenAndeler) {
            emptyList()
        } else {
            rader.map { mapAndelTilkjentYtelse(it, andelId++) }
        }
    }

    private fun mapAndelTilkjentYtelse(
        rad: Map<String, String>,
        andelId: Long,
    ): AndelData {
        val ytelseType = parseValgfriEnum(DomenebegrepAndeler.YTELSE_TYPE, rad) ?: YtelseType.ORDINÆR_BARNETRYGD
        return AndelData(
            id = andelId.toString(),
            fom = parseÅrMåned(Domenebegrep.FRA_DATO, rad),
            tom = parseÅrMåned(Domenebegrep.TIL_DATO, rad),
            beløp = parseInt(DomenebegrepAndeler.BELØP, rad),
            personIdent = parseFødselsnummer(rad),
            type = ytelseType,
            kildeBehandlingId = parseValgfriString(DomenebegrepAndeler.KILDEBEHANDLING_ID, rad),
            periodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.PERIODE_ID, rad),
            forrigePeriodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.FORRIGE_PERIODE_ID, rad),
            utbetalingsgrad = null,
        )
    }

    fun mapForventetUtbetalingsoppdrag(
        dataTable: DataTable,
    ): List<ForventetUtbetalingsoppdrag> {
        return dataTable.groupByBehandlingId().map { (behandlingId, rader) ->
            val rad = rader.first()
            validerAlleKodeEndringerLike(rader)
            ForventetUtbetalingsoppdrag(
                behandlingId = behandlingId,
                kodeEndring = parseEnum(DomenebegrepUtbetalingsoppdrag.KODE_ENDRING, rad),
                utbetalingsperiode = rader.map { mapForventetUtbetalingsperiode(it) },
            )
        }
    }

    private fun mapForventetUtbetalingsperiode(it: Map<String, String>) =
        ForventetUtbetalingsperiode(
            erEndringPåEksisterendePeriode = parseBoolean(DomenebegrepUtbetalingsoppdrag.ER_ENDRING, it),
            periodeId = parseLong(DomenebegrepUtbetalingsoppdrag.PERIODE_ID, it),
            forrigePeriodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.FORRIGE_PERIODE_ID, it),
            sats = parseInt(DomenebegrepUtbetalingsoppdrag.BELØP, it),
            ytelse = parseValgfriEnum<YtelseType>(DomenebegrepUtbetalingsoppdrag.YTELSE_TYPE, it)
                ?: YtelseType.ORDINÆR_BARNETRYGD,
            fom = parseÅrMåned(Domenebegrep.FRA_DATO, it).atDay(1),
            tom = parseÅrMåned(Domenebegrep.TIL_DATO, it).atEndOfMonth(),
            opphør = parseValgfriÅrMåned(DomenebegrepUtbetalingsoppdrag.OPPHØRSDATO, it)?.atDay(1),
            kildebehandlingId = parseValgfriLong(DomenebegrepAndeler.KILDEBEHANDLING_ID, it),
            satstype = parseValgfriEnum<Utbetalingsperiode.SatsType>(DomenebegrepAndeler.SATSTYPE, it)
                ?: Utbetalingsperiode.SatsType.MND,
        )

    private fun validerAlleKodeEndringerLike(rader: List<Map<String, String>>) {
        rader.map { parseEnum<Utbetalingsoppdrag.KodeEndring>(DomenebegrepUtbetalingsoppdrag.KODE_ENDRING, it) }
            .zipWithNext().forEach {
                assertThat(it.first).isEqualTo(it.second)
                    .withFailMessage("Alle kodeendringer for en og samme oppdrag må være lik ${it.first} -> ${it.second}")
            }
    }

    private fun parseFødselsnummer(rad: Map<String, String>): String {
        val id = (parseValgfriInt(DomenebegrepAndeler.IDENT, rad) ?: 1).toString()
        return id.padStart(11, '0')
    }
}

enum class DomenebegrepBehandlingsinformasjon(override val nøkkel: String) : Domenenøkkel {
    OPPHØR_FRA("Opphør fra"),
    YTELSE("Ytelse"),
}

enum class DomenebegrepAndeler(override val nøkkel: String) : Domenenøkkel {
    YTELSE_TYPE("Ytelse"),
    UTEN_ANDELER("Uten andeler"),
    BELØP("Beløp"),
    KILDEBEHANDLING_ID("Kildebehandling"),
    IDENT("Ident"),
    SATSTYPE("Satstype"),
}

enum class DomenebegrepUtbetalingsoppdrag(override val nøkkel: String) : Domenenøkkel {
    KODE_ENDRING("Kode endring"),
    ER_ENDRING("Er endring"),
    PERIODE_ID("Periode id"),
    FORRIGE_PERIODE_ID("Forrige periode id"),
    BELØP("Beløp"),
    YTELSE_TYPE("Ytelse"),
    OPPHØRSDATO("Opphørsdato"),
}

data class ForventetUtbetalingsoppdrag(
    val behandlingId: Long,
    val kodeEndring: Utbetalingsoppdrag.KodeEndring,
    val utbetalingsperiode: List<ForventetUtbetalingsperiode>,
)

data class ForventetUtbetalingsperiode(
    val erEndringPåEksisterendePeriode: Boolean,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val sats: Int,
    val ytelse: YtelseType,
    val fom: LocalDate,
    val tom: LocalDate,
    val opphør: LocalDate?,
    val kildebehandlingId: Long?,
    val satstype: Utbetalingsperiode.SatsType,
)
