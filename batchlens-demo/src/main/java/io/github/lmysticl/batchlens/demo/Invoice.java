package io.github.lmysticl.batchlens.demo;

import java.math.BigDecimal;

public record Invoice(long id, String customer, BigDecimal amount) {}
