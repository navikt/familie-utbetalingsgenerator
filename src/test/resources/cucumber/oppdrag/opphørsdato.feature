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
      | Melding                                                                   |
      | Kan ikke sende med opphørFra når det ikke finnes noen kjede fra tidligere |

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


  Scenario: Revurderer en tidligere behandling, samtidig som man opphører lengre bak i tiden (eks med simulering)

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør fra |
      | 2            | 05.2022    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 05.2022  | 02.2023  | 1676  | 1     |
      | 1            | 03.2023  | 06.2023  | 1723  | 1     |
      | 1            | 07.2023  | 03.2026  | 1766  | 1     |
      | 1            | 04.2026  | 03.2038  | 1310  | 1     |

      | 2            | 05.2022  | 02.2023  | 1676  | 1     |
      | 2            | 03.2023  | 06.2023  | 1723  | 1     |
      | 2            | 07.2023  | 08.2023  | 1766  | 1     |
      | 2            | 09.2023  | 03.2026  | 883   | 1     |
      | 2            | 04.2026  | 03.2038  | 655   | 1     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 05.2022  | 02.2023  |             | 1676  | NY           | Nei        | 0          |                    |
      | 1            | 03.2023  | 06.2023  |             | 1723  | NY           | Nei        | 1          | 0                  |
      | 1            | 07.2023  | 03.2026  |             | 1766  | NY           | Nei        | 2          | 1                  |
      | 1            | 04.2026  | 03.2038  |             | 1310  | NY           | Nei        | 3          | 2                  |

      | 2            | 04.2026  | 03.2038  | 05.2022     | 1310  | ENDR         | Ja         | 3          | 2                  |
      | 2            | 05.2022  | 02.2023  |             | 1676  | ENDR         | Nei        | 4          | 3                  |
      | 2            | 03.2023  | 06.2023  |             | 1723  | ENDR         | Nei        | 5          | 4                  |
      | 2            | 07.2023  | 08.2023  |             | 1766  | ENDR         | Nei        | 6          | 5                  |
      | 2            | 09.2023  | 03.2026  |             | 883   | ENDR         | Nei        | 7          | 6                  |
      | 2            | 04.2026  | 03.2038  |             | 655   | ENDR         | Nei        | 8          | 7                  |
