/// <reference types="node" />
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it, vi } from "vitest";
import { CURATED_THEMES, DEFAULT_APPEARANCE, type Appearance } from "./storage";
import { applyAppearance, resolvedTheme } from "./theme";

describe("curated Palomar themes", () => {
  it("loads startup theming as a same-origin script compatible with the production CSP", () => {
    const html = readFileSync(join(process.cwd(), "index.html"), "utf8");
    expect(html).toContain('<script src="/assets/theme-startup.js"></script>');
    expect(html).not.toMatch(/<script>(.|\n)*palomar\.appearance/s);
    expect(readFileSync(join(process.cwd(), "public/assets/theme-startup.js"), "utf8"))
      .toContain("palomar.appearance.v2");
    const startup = readFileSync(join(process.cwd(), "public/assets/theme-startup.js"), "utf8");
    for (const { id } of CURATED_THEMES) expect(startup).toContain(`"${id}"`);
    expect(startup).toContain('palomar: { light: "#f7f8fc", dark: "#090b16" }');
    expect(startup).not.toContain('palomar: { light: "#f7f5fc", dark: "#171527" }');
  });

  it("uses the same stable IDs and names as Android", () => {
    expect(CURATED_THEMES.map(({ id, name }) => ({ id, name }))).toEqual([
      { id: "palomar", name: "Palomar" },
      { id: "harbor", name: "Harbor" },
      { id: "grove", name: "Grove" },
      { id: "ember", name: "Ember" },
      { id: "dune", name: "Dune" },
      { id: "slate", name: "Slate" },
      { id: "neon-wave", name: "Neon Wave" },
      { id: "obsidian", name: "Obsidian" },
      { id: "high-contrast", name: "High Contrast" },
    ]);
  });

  it("uses the production asset family for web identity, metadata, and browser chrome", () => {
    const webLogo = readFileSync(join(process.cwd(), "public/palomar-mark.svg"), "utf8");
    const repositoryLogo = readFileSync(join(process.cwd(), "../docs/brand/palomar-mark.svg"), "utf8");
    const favicon = readFileSync(join(process.cwd(), "public/palomar-mark-16px.svg"), "utf8");
    const appIcon = readFileSync(join(process.cwd(), "public/palomar-app-icon.svg"), "utf8");
    const socialPreview = readFileSync(join(process.cwd(), "public/palomar-social-preview.png"));
    const html = readFileSync(join(process.cwd(), "index.html"), "utf8");
    const manifest = JSON.parse(readFileSync(join(process.cwd(), "public/manifest.webmanifest"), "utf8"));
    expect(webLogo).toBe(repositoryLogo);
    expect(favicon).toBe(readFileSync(join(process.cwd(), "../docs/brand/palomar-mark-16px.svg"), "utf8"));
    expect(appIcon).toBe(readFileSync(join(process.cwd(), "../docs/brand/palomar-app-icon.svg"), "utf8"));
    expect(socialPreview).toEqual(readFileSync(join(process.cwd(), "../docs/brand/palomar-social-preview.png")));
    expect(webLogo).toContain("#CFC1FD");
    expect(webLogo).toContain("#62F9F8");
    expect(favicon).toContain('viewBox="0 0 16 16"');
    expect(html).toContain('sizes="16x16" href="/palomar-mark-16px.svg"');
    expect(html).toContain('sizes="any" href="/palomar-mark.svg"');
    expect(html).toContain('property="og:image"');
    expect(html).toContain("docs/brand/palomar-social-preview.png");
    expect(html).toContain('<link rel="manifest" href="/manifest.webmanifest" />');
    expect(manifest.name).toBe("Palomar");
    expect(manifest.icons.map(({ src }: { src: string }) => src)).toEqual([
      "/palomar-app-icon.svg",
      "/palomar-app-icon.svg",
    ]);
  });

  it("applies every named theme in light and dark modes", () => {
    for (const { id } of CURATED_THEMES) {
      for (const colorMode of ["light", "dark"] as const) {
        const appearance: Appearance = { ...DEFAULT_APPEARANCE, colorMode, themeId: id };
        const cleanup = applyAppearance(appearance);
        expect(document.documentElement.dataset.palomarTheme).toBe(id);
        expect(document.documentElement.dataset.colorMode).toBe(colorMode);
        expect(document.documentElement.style.colorScheme).toBe(colorMode);
        cleanup();
      }
    }
  });

  it("follows a live OS color-scheme change in System mode", () => {
    let listener: (() => void) | undefined;
    const media = {
      matches: false,
      addEventListener: vi.fn((_event: string, next: () => void) => { listener = next; }),
      removeEventListener: vi.fn(),
    };
    vi.spyOn(window, "matchMedia").mockReturnValue(media as unknown as MediaQueryList);
    const cleanup = applyAppearance(DEFAULT_APPEARANCE);
    expect(document.documentElement.dataset.colorMode).toBe("light");
    media.matches = true;
    listener?.();
    expect(document.documentElement.dataset.colorMode).toBe("dark");
    cleanup();
    expect(media.removeEventListener).toHaveBeenCalled();
    expect(resolvedTheme("system", true)).toBe("dark");
  });

  it("keeps text, controls, failures, and full access above deterministic contrast floors", () => {
    const css = readFileSync(join(process.cwd(), "src/styles.css"), "utf8");
    [
      "--app-background", "--surface-primary", "--surface-alternate", "--surface-raised",
      "--border-default", "--divider", "--text-primary", "--text-muted", "--accent-primary",
      "--accent-emphasis", "--accent-container", "--brand-structure", "--link", "--focus-indicator", "--selection",
      "--disabled-surface", "--card-surface", "--grouped-header-surface", "--usage-track",
      "--usage-fill", "--context-track", "--context-fill", "--navigation-surface",
      "--dialog-surface", "--popover-surface", "--success", "--working", "--attention",
      "--warning", "--failure", "--full-access",
    ].forEach((token) => expect(css, `${token} is defined`).toContain(`${token}:`));
    const rule = (selector: string) => {
      const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
      const matches = [...css.matchAll(new RegExp(`${escaped}\\{([^}]*)}`, "g"))];
      expect(matches.length, `${selector} exists`).toBeGreaterThan(0);
      return Object.assign({}, ...matches.map((match) => tokens(match[1])));
    };
    const baseLight = rule(":root");
    const baseDark = rule(":root[data-color-mode=dark]");
    const nonPalomarLight = rule(":root:not([data-palomar-theme=palomar])");
    const nonPalomarDark = rule(":root[data-color-mode=dark]:not([data-palomar-theme=palomar])");
    for (const { id } of CURATED_THEMES) {
      for (const dark of [false, true]) {
        const override = id === "palomar" ? {} : rule(
          dark
            ? `:root[data-color-mode=dark][data-palomar-theme=${id}]`
            : `:root[data-palomar-theme=${id}]`,
        );
        const palette = {
          ...baseLight,
          ...(dark ? baseDark : {}),
          ...(id === "palomar" ? {} : nonPalomarLight),
          ...(id !== "palomar" && dark ? nonPalomarDark : {}),
          ...override,
        };
        const resolve = (value: string) => {
          const reference = value.match(/^var\((--[^)]+)\)$/)?.[1];
          return reference ? palette[reference] : value;
        };
        expect(contrast(palette["--text-primary"], palette["--app-background"]), `${id} ${dark ? "dark" : "light"} text`).toBeGreaterThanOrEqual(7);
        expect(contrast(palette["--on-accent"], palette["--accent-primary"]), `${id} ${dark ? "dark" : "light"} accent`).toBeGreaterThanOrEqual(4.5);
        if (["palomar", "neon-wave", "obsidian"].includes(id)) {
          expect(contrast(palette["--on-accent"], palette["--accent-emphasis"]), `${id} ${dark ? "dark" : "light"} secondary accent`).toBeGreaterThanOrEqual(4.5);
          expect(contrast(palette["--text-muted"], palette["--app-background"]), `${id} ${dark ? "dark" : "light"} secondary text`).toBeGreaterThanOrEqual(4.5);
          expect(contrast(palette["--link"], palette["--app-background"]), `${id} ${dark ? "dark" : "light"} link`).toBeGreaterThanOrEqual(4.5);
          expect(contrast(palette["--focus-indicator"], palette["--app-background"]), `${id} ${dark ? "dark" : "light"} focus`).toBeGreaterThanOrEqual(3);
          expect(contrast(palette["--on-accent-container"], palette["--accent-container"]), `${id} ${dark ? "dark" : "light"} selection`).toBeGreaterThanOrEqual(4.5);
          expect(contrast(palette["--disabled-text"], palette["--disabled-surface"]), `${id} ${dark ? "dark" : "light"} disabled`).toBeGreaterThanOrEqual(3);
          expect(contrast(resolve(palette["--usage-fill"]), resolve(palette["--usage-track"])), `${id} ${dark ? "dark" : "light"} usage meter`).toBeGreaterThanOrEqual(3);
          expect(contrast(resolve(palette["--context-fill"]), resolve(palette["--context-track"])), `${id} ${dark ? "dark" : "light"} context meter`).toBeGreaterThanOrEqual(3);
          expect(palette["--brand-structure"]).not.toBe(palette["--accent-primary"]);
          expect(palette["--usage-fill"]).not.toBe(palette["--context-fill"]);
        } else if (id !== "high-contrast") {
          expect(palette["--brand-structure"]).toBe("var(--accent-primary)");
          expect(palette["--usage-fill"]).toBe("var(--accent-primary)");
          expect(palette["--disabled-surface"]).toBe(dark ? "#302b45" : "#e9e5f1");
          expect(palette["--disabled-text"]).toBe(dark ? "#9d94b2" : "#685f7e");
          expect(palette["--disabled-border"]).toBe(dark ? "#403a55" : "#ccc5d8");
        }
        for (const role of ["success", "working", "attention", "warning", "failure", "full-access"]) {
          expect(
            contrast(palette[`--${role}`], palette[`--${role}-container`]),
            `${id} ${dark ? "dark" : "light"} ${role}`,
          ).toBeGreaterThanOrEqual(id === "high-contrast" ? 7 : 4.5);
        }
        expect(palette["--full-access"]).not.toBe(palette["--working"]);
        if (id === "high-contrast") {
          expect(contrast(palette["--text-muted"], palette["--app-background"]), `${dark ? "dark" : "light"} high contrast muted text`).toBeGreaterThanOrEqual(7);
          expect(contrast(palette["--border-default"], palette["--app-background"]), `${dark ? "dark" : "light"} high contrast borders`).toBeGreaterThanOrEqual(7);
          expect(contrast(palette["--accent-primary"], palette["--app-background"]), `${dark ? "dark" : "light"} high contrast accent`).toBeGreaterThanOrEqual(7);
          expect(contrast(palette["--disabled-text"], palette["--disabled-surface"]), `${dark ? "dark" : "light"} high contrast disabled`).toBeGreaterThanOrEqual(4.5);
        }
      }
    }
    expect(baseLight).toMatchObject({
      "--app-background": "#f7f8fc",
      "--surface-primary": "#fff",
      "--surface-alternate": "#eef0f7",
      "--border-default": "#d7dae5",
      "--text-primary": "#111326",
      "--text-muted": "#5d6175",
      "--accent-primary": "#006e73",
      "--accent-emphasis": "#493b82",
      "--accent-container": "#eae5ff",
      "--on-accent": "#fff",
      "--on-accent-container": "#2f2853",
      "--brand-structure": "#493b82",
      "--link": "#006e73",
      "--focus-indicator": "#006e73",
      "--disabled-surface": "#e8eaf1",
      "--disabled-text": "#686b7c",
      "--disabled-border": "#cdd1dc",
      "--context-fill": "#006e73",
    });
    expect(baseDark).toMatchObject({
      "--app-background": "#090b16",
      "--surface-primary": "#111326",
      "--surface-alternate": "#171527",
      "--border-default": "#30354d",
      "--text-primary": "#fff",
      "--text-muted": "#b6b7ca",
      "--accent-primary": "#62f9f8",
      "--accent-emphasis": "#cfc1fd",
      "--accent-container": "#352d63",
      "--on-accent": "#090b16",
      "--on-accent-container": "#f5f0ff",
      "--brand-structure": "#cfc1fd",
      "--link": "#62f9f8",
      "--focus-indicator": "#62f9f8",
      "--disabled-surface": "#202338",
      "--disabled-text": "#989bad",
      "--disabled-border": "#30344a",
      "--context-fill": "#62f9f8",
    });
    expect(baseLight["--usage-fill"]).toBe("var(--brand-structure)");
    expect({ ...baseLight, ...baseDark }["--usage-fill"]).toBe("var(--brand-structure)");
    expect(rule(":root[data-palomar-theme=neon-wave]")).toMatchObject({
      "--app-background": "#fcf6fb", "--surface-primary": "#fff", "--surface-alternate": "#f5e9fa",
      "--grouped-header-surface": "#ebd8f7", "--navigation-surface": "#f4e9fa",
      "--accent-primary": "#b30078", "--accent-emphasis": "#007480", "--brand-structure": "#713db9",
      "--usage-fill": "#a83d5d", "--context-fill": "#007480",
    });
    expect(rule(":root[data-color-mode=dark][data-palomar-theme=neon-wave]")).toMatchObject({
      "--app-background": "#070315", "--surface-primary": "#100826", "--surface-alternate": "#1c0c3a",
      "--card-surface": "#160a33", "--grouped-header-surface": "#2c1055", "--navigation-surface": "#0d0622",
      "--accent-primary": "#ff3cac", "--accent-emphasis": "#4debff", "--brand-structure": "#a864ff",
      "--link": "#ff8a9f", "--usage-fill": "#ff7794", "--context-fill": "#4debff",
    });
    expect(rule(":root[data-palomar-theme=obsidian]")).toMatchObject({
      "--app-background": "#f6f7f8", "--surface-primary": "#fff", "--surface-alternate": "#eceef1",
      "--accent-primary": "#7d3b52", "--accent-emphasis": "#665e6b", "--brand-structure": "#6f6875",
    });
    expect(rule(":root[data-color-mode=dark][data-palomar-theme=obsidian]")).toMatchObject({
      "--app-background": "#0b0c0f", "--surface-primary": "#13151a", "--surface-alternate": "#1c1e24",
      "--border-default": "#3f4248", "--accent-container": "#292a2e",
      "--accent-primary": "#b65f78", "--accent-emphasis": "#958a9b", "--brand-structure": "#958a9b",
    });
    expect(css).toContain(":root[data-palomar-theme=neon-wave] .session-card.selected");
    expect(css).not.toContain(":root[data-palomar-theme=obsidian] .session-card.selected");
    expect(baseLight).toMatchObject({
      "--success": "#087443", "--working": "#315fc4", "--attention": "#9b5800",
      "--warning": "#8a5000", "--failure": "#b42318", "--full-access": "#a4293d",
    });
    expect(baseDark).toMatchObject({
      "--success": "#6ce9a6", "--working": "#a9c7ff", "--attention": "#ffc56f",
      "--warning": "#ffd58a", "--failure": "#ffb4ab", "--full-access": "#ffb0bc",
    });
    for (const dark of [false, true]) {
      const palettes = CURATED_THEMES.filter(({ id }) => id !== "high-contrast").map(({ id }) => {
        const override = id === "palomar" ? {} : rule(
          dark
            ? `:root[data-color-mode=dark][data-palomar-theme=${id}]`
            : `:root[data-palomar-theme=${id}]`,
        );
        return {
          ...baseLight,
          ...(dark ? baseDark : {}),
          ...(id === "palomar" ? {} : nonPalomarLight),
          ...(id !== "palomar" && dark ? nonPalomarDark : {}),
          ...override,
        };
      });
      expect(new Set(palettes.map((palette) => palette["--working"])).size).toBe(palettes.length);
      expect(new Set(palettes.map((palette) => palette["--success"])).size).toBe(palettes.length);
    }
  });

  it("uses the ink foundation for Palomar browser chrome without changing its stable ID", () => {
    const meta = document.createElement("meta");
    meta.name = "theme-color";
    document.head.append(meta);
    const lightCleanup = applyAppearance({ ...DEFAULT_APPEARANCE, colorMode: "light", themeId: "palomar" });
    expect(meta.content).toBe("#f7f8fc");
    lightCleanup();
    const darkCleanup = applyAppearance({ ...DEFAULT_APPEARANCE, colorMode: "dark", themeId: "palomar" });
    expect(meta.content).toBe("#090b16");
    expect(document.documentElement.dataset.palomarTheme).toBe("palomar");
    darkCleanup();
    meta.remove();
  });
});

function tokens(body: string): Record<string, string> {
  return Object.fromEntries(
    [...body.matchAll(/(--[a-z-]+):\s*(#[0-9a-f]{3,8}|var\(--[a-z-]+\))/gi)]
      .map(([, key, value]) => [key, value]),
  );
}

function contrast(a: string, b: string): number {
  const [lighter, darker] = [luminance(a), luminance(b)].sort((left, right) => right - left);
  return (lighter + 0.05) / (darker + 0.05);
}

function luminance(hex: string): number {
  const normalized = hex.length === 4 ? hex.slice(1).split("").map((value) => value + value).join("") : hex.slice(1, 7);
  const channels = normalized.match(/.{2}/g)!.map((value) => Number.parseInt(value, 16) / 255).map((value) =>
    value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4,
  );
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
}
