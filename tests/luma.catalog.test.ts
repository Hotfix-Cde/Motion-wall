import { afterEach, describe, expect, it, vi } from "vitest";

import { getTrendingTracks, searchTracks } from "../lib/luma/catalog";

const originalFetch = global.fetch;

afterEach(() => {
  global.fetch = originalFetch;
  vi.restoreAllMocks();
});

describe("Luma catalog client", () => {
  it("maps an API-visible track to a streamable Luma track", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        data: [{
          id: "track-123",
          title: "Night Lanterns",
          duration: 184,
          genre: "Ambient",
          mood: "Gentle",
          permalink: "/luma/night-lanterns",
          artwork: { "480x480": "https://images.example/night.png" },
          user: { name: "Luma Artist" },
        }],
      }),
    }) as typeof fetch;

    const tracks = await getTrendingTracks(1);

    expect(tracks).toEqual([{
      id: "track-123",
      title: "Night Lanterns",
      artist: "Luma Artist",
      artwork: "https://images.example/night.png",
      streamUrl: "https://api.audius.co/v1/tracks/track-123/stream?app_name=Luma",
      duration: 184,
      genre: "Ambient",
      mood: "Gentle",
      permalink: "https://audius.co/luma/night-lanterns",
    }]);
  });

  it("encodes a mood query and filters malformed catalog entries", async () => {
    const request = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ data: [{ id: "valid", title: "Moon Room", user: { handle: "maker" } }, { title: "Missing ID" }] }),
    });
    global.fetch = request as typeof fetch;

    const tracks = await searchTracks("cloud nap", 5);

    expect(request.mock.calls[0][0]).toContain("query=cloud+nap");
    expect(request.mock.calls[0][0]).toContain("limit=5");
    expect(tracks).toHaveLength(1);
    expect(tracks[0].artist).toBe("maker");
    expect(tracks[0].genre).toBe("Open music");
  });

  it("reports a failed catalog response instead of treating it as playable content", async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 503 }) as typeof fetch;
    await expect(getTrendingTracks()).rejects.toThrow("Catalog request failed (503)");
  });
});
