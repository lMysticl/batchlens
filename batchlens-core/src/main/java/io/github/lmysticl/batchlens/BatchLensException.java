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
    this(kind, detail, null);
  }

  public BatchLensException(Kind kind, String detail, Throwable cause) {
    super(detail, cause);
    this.kind = kind;
  }

  public static BatchLensException executionNotFound() {
    return new BatchLensException(Kind.NOT_FOUND, "Execution does not exist");
  }

  /** Spring Batch refused a start or restart, for example because the instance is running. */
  public static BatchLensException rejectedBySpringBatch(Exception failure) {
    return new BatchLensException(
        Kind.CONFLICT,
        "Spring Batch rejected this operation ("
            + failure.getClass().getSimpleName()
            + "). Refresh the execution and check its current state.",
        failure);
  }

  /** The worker saved the execution between reading it and saving the stop request. */
  public static BatchLensException changedDuringStop(Exception failure) {
    return new BatchLensException(
        Kind.CONFLICT,
        "Execution changed while requesting stop. Refresh before retrying.",
        failure);
  }

  public Kind kind() {
    return kind;
  }
}
