package io.github.lmysticl.batchlens.web;

import io.github.lmysticl.batchlens.BatchLensProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public final class BatchApiAuthentication implements HandlerInterceptor {
  private final byte[] expected;

  public BatchApiAuthentication(BatchLensProperties properties) {
    expected = ("Bearer " + properties.apiToken()).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    // Authenticate the resolved controller, including encoded/context/matrix paths.
    if (!(handler instanceof HandlerMethod method)
        || !BatchController.class.isAssignableFrom(method.getBeanType())) return true;
    String authorization = request.getHeader("Authorization");
    if (authorization == null
        || !MessageDigest.isEqual(expected, authorization.getBytes(StandardCharsets.UTF_8))) {
      response.setStatus(401);
      response.setContentType("application/problem+json");
      response.setHeader("WWW-Authenticate", "Bearer realm=\"BatchLens\"");
      response
          .getWriter()
          .write(
              "{\"title\":\"Authentication required\",\"status\":401,\"detail\":\"Supply the configured BatchLens bearer token.\"}");
      return false;
    }
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");
    return true;
  }
}
