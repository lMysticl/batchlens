package io.github.lmysticl.batchlens;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lmysticl.batchlens.web.BatchController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OptInTest {
  @Test
  void disabledStarterDoesNotInstallEndpointsOrRequireBatchInfrastructure() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BatchLensAutoConfiguration.class))
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertEquals(0, context.getBeansOfType(BatchController.class).size());
              assertFalse(context.containsBean("batchLensAuthentication"));
            });
  }

  @Test
  void enabledConfigurationRequiresANontrivialToken() {
    assertThrows(IllegalArgumentException.class, () -> new BatchLensProperties("", true));
    assertThrows(IllegalArgumentException.class, () -> new BatchLensProperties(null, false));
  }
}
