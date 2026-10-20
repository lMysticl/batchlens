package io.github.lmysticl.batchlens;

import java.io.Serial;

public final class BatchLensException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN
  }

  private final Kind kind;

  public BatchLensException(Kind kind, String detail) {
    super(detail);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
