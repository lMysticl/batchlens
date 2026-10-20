package io.github.lmysticl.batchlens;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("batchlens")
public record BatchLensProperties(String apiToken, boolean operationsEnabled) {
  public BatchLensProperties {
    if (apiToken == null || apiToken.length() < 32) {
      throw new IllegalArgumentException("batchlens.api-token must contain at least 32 characters");
    }
  }
}
