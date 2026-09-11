import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { loadPalomarBuildMetadata, parseReleaseProperties } from "./build-metadata";

const repositoryRoot = resolve(process.cwd(), "..");

describe("Palomar build metadata", () => {
  it("derives the web version from the shared release manifest", () => {
    const release = parseReleaseProperties(readFileSync(`${repositoryRoot}/palomar-release.properties`, "utf8"));
    expect(loadPalomarBuildMetadata(repositoryRoot, { PALOMAR_BUILD_COMMIT: "abc123def456" })).toEqual({
      version: release.palomarVersion,
      commit: "abc123def456",
      releaseBuild: release.releaseBuild === "true",
    });
  });

  it("rejects malformed release metadata", () => {
    expect(() => parseReleaseProperties("palomarVersion\n")).toThrow("expected key=value");
  });
});
