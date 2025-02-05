package no.nav.familie.felles.utbetalingsgenerator.domain

data class IdentOgType(
    val ident: String,
    val type: Ytelsestype,
) : Comparable<IdentOgType> {
    override fun compareTo(other: IdentOgType): Int {
        return (type.klassifisering + ident).compareTo(other.type.klassifisering  + other.ident)
    }
}
