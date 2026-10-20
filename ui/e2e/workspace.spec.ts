import { test, expect } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { mkdir } from "node:fs/promises";
import path from "node:path";

test("inspect failure, restart a checkpoint, and stop an asynchronous import", async ({
  page,
}, testInfo) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("./");
  await page.getByRole("button", { name: "Use the bundled local demo token" }).click();
  await page.getByRole("button", { name: "Connect workspace" }).click();
  await expect(page.getByRole("heading", { name: "Invoice import" })).toBeVisible();
  for (let index = 0; index < 2; index++) {
    const seeded = await page.request.post(
      new URL("api/jobs/invoiceImport/start", page.url()).href,
      {
        headers: { Authorization: "Bearer local-demo-token-change-this-before-deployment" },
        data: { parameters: { run: `history-${randomUUID()}`, failAt: "0", delayMs: "0" } },
      },
    );
    expect(seeded.status()).toBe(202);
  }
  await page.getByRole("button", { name: "Start new execution" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await page
    .getByLabel("Job parameters")
    .fill(JSON.stringify({ run: `browser-${randomUUID()}`, failAt: "137", delayMs: "1" }));
  await dialog.getByRole("button", { name: "Start execution", exact: true }).click();
  await expect(dialog).toBeHidden();
  const inspector = page.locator('section[aria-labelledby="execution-title"]');
  await expect(page.getByRole("button", { name: "Restart execution" })).toBeVisible();
  await inspector.locator(".tabs button").nth(1).click();
  await expect(inspector.locator("pre")).toContainText("Planned demo failure");
  await inspector.locator(".tabs button").first().click();
  const committed = Number(await page.locator("tbody tr.selected td").nth(3).innerText());
  expect(committed).toBeGreaterThan(0);
  expect(committed).toBeLessThan(300);
  await capture("workspace");
  await page.getByRole("button", { name: "Restart execution" }).click();
  await expect(inspector.locator(".status").first()).toHaveText(/COMPLETED/i);
  await expect(page.locator("tbody tr.selected td").nth(3)).toHaveText(String(300 - committed));
  await page.getByRole("button", { name: "Start new execution" }).click();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toBeHidden();
  await page.getByRole("button", { name: "Start new execution" }).click();
  await page
    .getByLabel("Job parameters")
    .fill(JSON.stringify({ run: `stop-${randomUUID()}`, failAt: "0", delayMs: "20" }));
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Start execution", exact: true })
    .click();
  await page.getByRole("button", { name: "Stop execution", exact: true }).click();
  await expect(inspector.locator(".status").first()).toHaveText(/STOPPED/i);
  await page.getByRole("button", { name: "Restart execution" }).click();
  await expect(inspector.locator(".status").first()).toHaveText(/COMPLETED/i);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errors).toEqual([]);
  await page.getByRole("button", { name: "Disconnect", exact: true }).click();
  await expect(page.getByLabel("API token")).toHaveValue("");

  async function capture(name: string) {
    const directory = process.env.CAPTURE_DIR;
    const file = directory
      ? path.join(directory, `batchlens-${name}-${testInfo.project.name}.png`)
      : testInfo.outputPath(`${name}.png`);
    await mkdir(path.dirname(file), { recursive: true });
    await page.screenshot({ path: file, fullPage: true });
    await testInfo.attach(name, { path: file, contentType: "image/png" });
  }
});
