# language: no
# encoding: UTF-8

Egenskap: Sender med opphørFra


  Scenario: Revurderer en tidligere behandling, samtidig som man opphører lengre bak i tiden

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør fra |
      | 2            | 01.2021    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 2            | 05.2021  | 05.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |

      | 2            | 03.2021  | 03.2021  | 01.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 05.2021  | 05.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

  Scenario: Revurderer en tidligere behandling, samtidig som man opphører lengre bak i tiden (eks med simulering)

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør fra |
      | 2            | 03.2021    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1     |

      | 2            | 03.2021  | 03.2021  | 700   | 1     |
      | 2            | 03.2021  | 03.2021  | 700   | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |

      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 2          |                    |

  Scenario: Kan ikke sende inn opphørFra på en førstegangsbehandling

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør fra |
      | 1            | 04.2021    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag kjøres kastes exception
      | Melding                               |
      | Må ha siste andel for å kunne opphøre |

  Scenario: Kan ikke sende med opphørFra etter første fom på forrige andeler

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør fra |
      | 2            | 04.2021    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 2            | 05.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag kjøres kastes exception
      | Melding            |
      | som er etter andel |

