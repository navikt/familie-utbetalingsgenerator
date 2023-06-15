package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData
import java.time.YearMonth

private sealed interface BeståendeAndelResultat
private object NyAndelSkriverOver : BeståendeAndelResultat
private class Opphørsdato(val opphør: YearMonth) : BeståendeAndelResultat
private class AvkortAndel(val andel: AndelData, val opphør: YearMonth? = null) : BeståendeAndelResultat

internal data class BeståendeAndeler(
    val andeler: List<AndelData>,
    val opphørFra: YearMonth? = null,
)

internal object BeståendeAndelerBeregner {

    fun finnBeståendeAndeler(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ): BeståendeAndeler {
        val index = finnIndexPåFørsteDiff(forrigeAndeler, nyeAndeler)
        // TODO for å oppdatere id på forrige sånn at bestående fortsatt har riktig id på nytt
        val forrige = forrigeAndeler.mapIndexed { forrigeIndex, andelData ->
            if (nyeAndeler.size > forrigeIndex) {
                andelData.copy(id = nyeAndeler[forrigeIndex].id)
            } else {
                andelData
            }
        }

        val beståendeAndeler = index?.let {
            val opphørsdato = finnBeståendeAndelOgOpphør(it, forrige, nyeAndeler)
            when (opphørsdato) {
                is Opphørsdato -> BeståendeAndeler(forrige.subList(0, index), opphørsdato.opphør)
                is NyAndelSkriverOver -> BeståendeAndeler(forrige.subList(0, maxOf(0, index)))
                is AvkortAndel -> {
                    val avkortetAndeler = forrige.subList(0, maxOf(0, index))
                    BeståendeAndeler(avkortetAndeler + opphørsdato.andel, opphørsdato.opphør)
                }
            }
        } ?: BeståendeAndeler(forrige, null)

        return beståendeAndeler
    }

    private fun finnBeståendeAndelOgOpphør(
        index: Int,
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ): BeståendeAndelResultat {
        val forrige = forrigeAndeler[index]
        val ny = if (nyeAndeler.size > index) nyeAndeler[index] else null
        val nyNeste = if (nyeAndeler.size > index + 1) nyeAndeler[index + 1] else null

        return finnBeståendeAndelOgOpphør(ny, forrige, nyNeste)
    }

    private fun finnBeståendeAndelOgOpphør(
        ny: AndelData?,
        forrige: AndelData,
        nyNeste: AndelData?,
    ): BeståendeAndelResultat {
        if (ny == null || forrige.fom < ny.fom) {
            return Opphørsdato(forrige.fom)
        }
        if (forrige.fom > ny.fom || forrige.beløp != ny.beløp) {
            if (ny.beløp == 0) {
                return Opphørsdato(ny.fom)
            }
            return NyAndelSkriverOver
        }
        if (forrige.tom > ny.tom) {
            val opphørsdato = if (nyNeste == null || nyNeste.fom != ny.tom.plusMonths(1) || nyNeste.beløp == 0) {
                ny.tom.plusMonths(1)
            } else {
                null
            }
            return AvkortAndel(forrige.copy(tom = ny.tom), opphørsdato)
        }
        return NyAndelSkriverOver
    }

    private fun finnIndexPåFørsteDiff(
        forrige: List<AndelData>,
        nye: List<AndelData>,
    ): Int? {
        forrige.forEachIndexed { index, andelData ->
            if (nye.size > index) {
                val nyAndelForIndex = nye[index]
                if (!andelData.erLik(nyAndelForIndex)) {
                    return index
                }
            } else {
                return index
            }
        }
        return null
    }

    private fun AndelData.erLik(other: AndelData): Boolean =
        this.fom == other.tom &&
            this.tom == other.tom &&
            this.beløp == other.beløp
}
