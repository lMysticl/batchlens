package io.github.lmysticl.batchlens.batch6;

import static org.assertj.core.api.Assertions.assertThat;

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
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(BatchController.class);
              assertThat(context).doesNotHaveBean("batchLensAuthentication");
            });
  }
}
