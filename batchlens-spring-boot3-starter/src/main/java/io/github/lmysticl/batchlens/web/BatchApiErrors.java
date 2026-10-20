package io.github.lmysticl.batchlens.web;

import io.github.lmysticl.batchlens.BatchLensException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BatchController.class)
public final class BatchApiErrors {
  @ExceptionHandler(BatchLensException.class)
  ProblemDetail failure(BatchLensException failure) {
    var status =
        switch (failure.kind()) {
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT -> HttpStatus.CONFLICT;
          case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    return ProblemDetail.forStatusAndDetail(status, failure.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalidInput(IllegalArgumentException failure) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, failure.getMessage());
  }
}
