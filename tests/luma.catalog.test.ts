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

  it("uses an open-radio station when the primary catalog is temporarily unavailable", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({ ok: false, status: 503 })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ([{
          stationuuid: "radio-123",
          name: "Sleeping Pill Radio",
          url_resolved: "https://stream.example/sleep.mp3",
          tags: "ambient,sleep",
          country: "United States",
          homepage: "https://example.com/radio",
        }]),
      }) as typeof fetch;

    const tracks = await getTrendingTracks();

    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(tracks).toEqual([expect.objectContaining({
      id: "radio-radio-123",
      title: "Sleeping Pill Radio",
      streamUrl: "https://stream.example/sleep.mp3",
      genre: "ambient",
      mood: "Live stream",
    })]);
  });
});
