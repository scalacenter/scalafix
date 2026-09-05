/*
rules = [OrganizeImports]
OrganizeImports {
  preset = INTELLIJ_2020_3
  removeUnused = false
}
 */
package test.organizeImports

import test.organizeImports.PresetIntelliJ_2020_3_Selectors.metrics.MetricRegistry
import test.organizeImports.PresetIntelliJ_2020_3_Selectors.metrics.{Gauge => DropwizardGauge}
import test.organizeImports.PresetIntelliJ_2020_3_Selectors.mockito.verify
import test.organizeImports.PresetIntelliJ_2020_3_Selectors.mockito.when
import test.organizeImports.PresetIntelliJ_2020_3_Selectors.mockito.{timeout => mockitoTimeout}

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
