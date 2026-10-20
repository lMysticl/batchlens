package io.github.lmysticl.batchlens;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BatchLensPropertiesTest {
  @Test
  void enabledConfigurationRequiresANontrivialToken() {
    assertThatThrownBy(() -> new BatchLensProperties("", true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new BatchLensProperties(null, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new BatchLensProperties("x".repeat(31), true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
