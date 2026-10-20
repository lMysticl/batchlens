import { describe, expect, it } from "vitest";
import { parseParameters } from "../workspace/StartExecution";

describe("job parameter entry", () => {
  it("rejects JSON that the string-parameter API cannot accept", () => {
    for (const input of ["null", "[]", '"text"', '{"run":3}', '{"run":{"id":"a"}}'])
      expect(() => parseParameters(input)).toThrow("object of strings");
  });
  it("preserves identifying parameter values", () => {
    expect(parseParameters('{"run":"import-1","failAt":"137"}')).toEqual({
      run: "import-1",
      failAt: "137",
    });
  });
});
