# familie-utbetalingsgenerator

Bibliotek for å generere utbetalingsoppdrag mot økonomi

### Utbetalingsgeneratorn
[Utbetalingsgenerator.lagUtbetalingsoppdrag](src/main/kotlin/no/nav/familie/felles/utbetalingsgenerator/Utbetalingsgenerator.kt)
#### Input 
* [Behandlingsinformasjon](src/main/kotlin/no/nav/familie/felles/utbetalingsgenerator/domain/Behandlingsinformasjon.kt)
* Nye andeler
* Tidligere andeler
* Siste andel per kjede
  * Dette er siste (opprinnelige) andelen i en kjede. Dvs hvis man har avkortet eller opphørt en tidligere periode midt i så må man sende inn den opprinnelige
  * Eks hvis man har 
    * Behandling 1: jan - april
    * Behandling 2: jan - mars (denne avkorter den forrige, men beholder periodeId)
    * Behandling 3: jan - februar (denne avkorter den forrige, men beholder periodeId, og skal gjenbruke andelen fra behandling 1)

####
Fagsystem og Ytelsestype er interface som burde implementeres av en enum, eks
```kotlin
enum class YtelsestypeEF(
    override val klassifisering: String,
    override val satsType: SatsType = SatsType.MND,
) : Ytelsestype {
    OVERGANGSSTØNAD("EFOG"),
    BARNETILSYN("EFBT"),
    SKOLEPENGER("EFSP", SatsType.ENG),
}

enum class FagsystemEF(
    override val kode: String,
    override val gyldigeSatstyper: Set<Ytelsestype>,
) : Fagsystem {
    OVERGANGSSTØNAD("EFOG", setOf(TestYtelsestype.OVERGANGSSTØNAD)),
}
```

#### Output
* [BeregnetUtbetalingsoppdrag](src/main/kotlin/no/nav/familie/felles/utbetalingsgenerator/domain/BeregnetUtbetalingsoppdrag.kt)
  * Som inneholder utbetalingsoppdrag og andeler med periodeId/forrigePeriodeId/kildeBehandlingId hvis man trenger å oppdatere andeler i basen


## Kjente begrensninger

* Håndterer kun månedsytelser
* Forlengelse av perioder kan ta i bruk ENDR med ny TOM,
[slackkonversasjon](https://nav-it.slack.com/archives/C01ESUV8V52/p1684833442706959?thread_ts=1683194255.907149&cid=C01ESUV8V52)