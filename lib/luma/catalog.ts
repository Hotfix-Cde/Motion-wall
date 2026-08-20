import type { LumaTrack } from "./types";

const API_BASE = "https://api.audius.co/v1";

type AudiusArtwork = Record<string, string> | null | undefined;
type AudiusTrack = {
  id?: string;
  title?: string;
  duration?: number;
  genre?: string;
  mood?: string;
  permalink?: string;
  artwork?: AudiusArtwork;
  user?: { name?: string; handle?: string };
};

function imageFromArtwork(artwork: AudiusArtwork): string | null {
  if (!artwork) return null;
  return artwork["480x480"] ?? artwork["150x150"] ?? artwork["1000x1000"] ?? Object.values(artwork)[0] ?? null;
}

function toTrack(track: AudiusTrack): LumaTrack | null {
  if (!track.id || !track.title) return null;
  return {
    id: track.id,
    title: track.title,
    artist: track.user?.name ?? track.user?.handle ?? "Independent artist",
    artwork: imageFromArtwork(track.artwork),
    streamUrl: `${API_BASE}/tracks/${track.id}/stream?app_name=Luma`,
    duration: Math.max(0, Number(track.duration ?? 0)),
    genre: track.genre?.trim() || "Open music",
    mood: track.mood?.trim() || "Unhurried",
    permalink: track.permalink ? `https://audius.co${track.permalink}` : null,
  };
}

async function request(path: string): Promise<LumaTrack[]> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12_000);
  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: { Accept: "application/json" },
      signal: controller.signal,
    });
    if (!response.ok) throw new Error(`Catalog request failed (${response.status})`);
    const payload = (await response.json()) as { data?: AudiusTrack[] };
    return (payload.data ?? []).map(toTrack).filter((track): track is LumaTrack => Boolean(track));
  } finally {
    clearTimeout(timeout);
  }
}

export function getTrendingTracks(limit = 12): Promise<LumaTrack[]> {
  return request(`/tracks/trending?limit=${limit}&app_name=Luma`);
}

export function searchTracks(query: string, limit = 20): Promise<LumaTrack[]> {
  const params = new URLSearchParams({ query, limit: String(limit), app_name: "Luma" });
  return request(`/tracks/search?${params.toString()}`);
}
