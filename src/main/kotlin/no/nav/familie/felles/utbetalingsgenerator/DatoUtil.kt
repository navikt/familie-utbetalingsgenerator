package no.nav.familie.felles.utbetalingsgenerator

import java.time.YearMonth

internal object DatoUtil {
    fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)
    fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()
}
