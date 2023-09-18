# language: no
# encoding: UTF-8

Egenskap: Sender med opphørKjederFraFørsteUtbetaling


  Scenario: Skal opphøre alle kjeder fra første utbetaling dersom opphørKjederFraFørsteUtbetaling er satt til true

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør kjeder fra første utbetaling |
      | 2            | Ja                                  |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1     |
      | 1            | 04.2021  | 08.2023  | 800   | 1     |
      | 1            | 06.2021  | 08.2022  | 700   | 2     |
      | 1            | 09.2022  | 08.2023  | 800   | 2     |

      | 2            | 03.2021  | 03.2021  | 750   | 1     |
      | 2            | 04.2021  | 08.2023  | 850   | 1     |
      | 2            | 06.2021  | 08.2022  | 750   | 2     |
      | 2            | 09.2022  | 08.2023  | 850   | 2     |


    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 08.2023  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 1            | 06.2021  | 08.2022  |             | 700   | NY           | Nei        | 2          |                    |
      | 1            | 09.2022  | 08.2023  |             | 800   | NY           | Nei        | 3          | 2                  |


      | 2            | 04.2021  | 08.2023  | 03.2021     | 800   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 09.2022  | 08.2023  | 06.2021     | 800   | ENDR         | Ja         | 3          | 2                  |
      | 2            | 03.2021  | 03.2021  |             | 750   | ENDR         | Nei        | 4          | 1                  |
      | 2            | 04.2021  | 08.2023  |             | 850   | ENDR         | Nei        | 5          | 4                  |
      | 2            | 06.2021  | 08.2022  |             | 750   | ENDR         | Nei        | 6          | 3                  |
      | 2            | 09.2022  | 08.2023  |             | 850   | ENDR         | Nei        | 7          | 6                  |

  Scenario: Skal opphøre alle kjeder fra første utbetaling dersom opphørKjederFraFørsteUtbetaling er satt til true og første andel er 0-utbetaling

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør kjeder fra første utbetaling |
      | 2            | Ja                                  |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 03.2021  | 03.2021  | 0     | 1     |
      | 1            | 04.2021  | 08.2023  | 800   | 1     |
      | 1            | 06.2021  | 08.2022  | 0     | 2     |
      | 1            | 09.2022  | 08.2023  | 800   | 2     |

      | 2            | 03.2021  | 03.2021  | 0     | 1     |
      | 2            | 04.2021  | 08.2023  | 850   | 1     |
      | 2            | 06.2021  | 08.2022  | 0     | 2     |
      | 2            | 09.2022  | 08.2023  | 850   | 2     |


    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 04.2021  | 08.2023  |             | 800   | NY           | Nei        | 0          |                    |
      | 1            | 09.2022  | 08.2023  |             | 800   | NY           | Nei        | 1          |                    |


      | 2            | 04.2021  | 08.2023  | 04.2021     | 800   | ENDR         | Ja         | 0          |                    |
      | 2            | 09.2022  | 08.2023  | 09.2022     | 800   | ENDR         | Ja         | 1          |                    |
      | 2            | 04.2021  | 08.2023  |             | 850   | ENDR         | Nei        | 2          | 0                  |
      | 2            | 09.2022  | 08.2023  |             | 850   | ENDR         | Nei        | 3          | 1                  |


  Scenario: Kan ikke sette opphørKjederFraFørsteUtbetaling til true og samtidig sende med en opphørFra dato

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Opphør kjeder fra første utbetaling | Opphør fra |
      | 1            | Ja                                  | 03.2021    |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag kjøres kastes exception
      | Melding                                                                                |
      | Kan ikke sette opphørKjederFraFørsteUtbetaling til true samtidig som opphørFra er satt |