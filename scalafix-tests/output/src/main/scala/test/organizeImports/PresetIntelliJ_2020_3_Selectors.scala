package test.organizeImports

import test.organizeImports.PresetIntelliJ_2020_3_Selectors.metrics.{
  MetricRegistry,
  Gauge => DropwizardGauge
}
import test.organizeImports.PresetIntelliJ_2020_3_Selectors.mockito.{
  verify,
  when,
  timeout => mockitoTimeout
}

object PresetIntelliJ_2020_3_Selectors {
  object metrics {
    object MetricRegistry
    object Gauge
  }
  object mockito {
    object verify
    object when
    object timeout
  }
}
