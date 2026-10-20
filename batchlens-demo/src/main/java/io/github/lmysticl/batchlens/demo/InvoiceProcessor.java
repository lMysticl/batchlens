package io.github.lmysticl.batchlens.demo;

import org.springframework.batch.infrastructure.item.ItemProcessor;

public class InvoiceProcessor implements ItemProcessor<Invoice, Invoice> {
  private final int failAt;
  private final int delayMillis;
  private final boolean firstAttempt;

  public InvoiceProcessor(int failAt, int delayMillis, boolean firstAttempt) {
    if (failAt < 0 || failAt > 300 || delayMillis < 0 || delayMillis > 200) {
      throw new IllegalArgumentException("Demo limits: failAt 0–300; delayMs 0–200");
    }
    this.failAt = failAt;
    this.delayMillis = delayMillis;
    this.firstAttempt = firstAttempt;
  }

  @Override
  public Invoice process(Invoice invoice) throws InterruptedException {
    if (firstAttempt && invoice.id() == failAt) {
      throw new IllegalStateException(
          "Planned demo failure at invoice "
              + failAt
              + ". Restart this execution to continue from the last committed chunk.");
    }
    if (delayMillis > 0) Thread.sleep(delayMillis);
    return invoice;
  }
}
