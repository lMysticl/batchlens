package io.github.lmysticl.batchlens.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchLensVersionTest {
  @Test
  void reportsTheVersionWrittenByTheBuild() {
    assertThat(BatchLensVersion.CURRENT).matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
  }
}
