package io.github.lmysticl.batchlens.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

/** The release reported by the API, written into {@code batchlens.properties} by the build. */
final class BatchLensVersion {
  static final String CURRENT = read();

  private BatchLensVersion() {}

  private static String read() {
    var properties = new Properties();
    try (var in = BatchLensVersion.class.getResourceAsStream("batchlens.properties")) {
      if (in != null) properties.load(in);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
    return properties.getProperty("version", "unknown");
  }
}
