import { describe, expect, it } from "vitest";
import type { RepositoryInfo } from "./protocol";
import { resolveRepositorySet } from "./repository-labels";

const repository = (path: string): RepositoryInfo => ({
  id: path,
  name: path.split("/").at(-1)!,
  path,
  branch: "main",
  dirty: false,
});

const labels = (paths: string[]) => Object.fromEntries(
  resolveRepositorySet(paths.map(repository), "/projects")
    .map(({ info, label }) => [info.path, label]),
);

describe("repository display labels", () => {
  it("keeps unique basenames concise", () => {
    expect(labels(["palomar", "tools/android"])).toEqual({
      palomar: "palomar",
      "tools/android": "android",
    });
  });

  it("adds the shortest parent suffix for duplicate basenames", () => {
    expect(labels(["mobile/android", "accounts/android"])).toEqual({
      "mobile/android": "mobile/android",
      "accounts/android": "accounts/android",
    });
  });

  it("keeps a root-level repository concise when a nested repository collides", () => {
    expect(labels(["android", "accounts-admin/android"])).toEqual({
      android: "android",
      "accounts-admin/android": "accounts-admin/android",
    });
  });

  it("expands deeper suffix collisions until every label is distinct", () => {
    expect(labels(["clients/mobile/android", "archive/mobile/android"])).toEqual({
      "clients/mobile/android": "clients/mobile/android",
      "archive/mobile/android": "archive/mobile/android",
    });
  });

  it("does not depend on repository discovery order", () => {
    const paths = ["android", "clients/mobile/android", "archive/mobile/android"];
    expect(labels(paths)).toEqual(labels([...paths].reverse()));
  });

  it("returns a remaining repository to its concise label after removal", () => {
    expect(labels(["mobile/android", "accounts/android"])["mobile/android"]).toBe("mobile/android");
    expect(labels(["mobile/android"])["mobile/android"]).toBe("android");
  });
});
