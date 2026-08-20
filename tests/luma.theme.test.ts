import { describe, expect, it } from "vitest";

import { DEFAULT_THEME, LUMA_THEMES } from "../lib/luma/theme";

describe("Luma themes", () => {
  it("ships four complete, uniquely named visual presets", () => {
    const themes = Object.values(LUMA_THEMES);
    expect(themes).toHaveLength(4);
    expect(new Set(themes.map((theme) => theme.name)).size).toBe(4);
    expect(LUMA_THEMES[DEFAULT_THEME].name).toBe("Velvet Aurora");
  });

  it("gives every preset all surfaces required by the liquid-glass UI", () => {
    Object.values(LUMA_THEMES).forEach((theme) => {
      expect(theme.background).toMatch(/^#/);
      expect(theme.accent).toMatch(/^#/);
      expect(theme.glow).toMatch(/^#/);
      expect(theme.gradient).toHaveLength(3);
    });
  });
});
