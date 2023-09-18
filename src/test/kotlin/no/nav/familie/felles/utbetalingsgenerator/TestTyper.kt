package no.nav.familie.felles.utbetalingsgenerator

import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType
import no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype

enum class TestYtelsestype(
    override val klassifisering: String,
    override val satsType: SatsType = SatsType.MND,
) : Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),

    OVERGANGSSTØNAD("EFOG"),
    BARNETILSYN("EFBT"),
    SKOLEPENGER("EFSP", SatsType.ENG),
}

enum class TestFagsystem(
    override val kode: String,
    override val gyldigeSatstyper: Set<Ytelsestype>,
) : Fagsystem {
    OVERGANGSSTØNAD("EFOG", setOf(TestYtelsestype.OVERGANGSSTØNAD)),
    SKOLEPENGER("EFSP", setOf(TestYtelsestype.SKOLEPENGER)),
    BARNETILSYN(
        "BA",
        setOf(TestYtelsestype.ORDINÆR_BARNETRYGD, TestYtelsestype.UTVIDET_BARNETRYGD, TestYtelsestype.SMÅBARNSTILLEGG),
    ),
}
