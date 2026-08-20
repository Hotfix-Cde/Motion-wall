export type ThemeId = "velvet" | "candy" | "tidal" | "solar";

export type LumaTheme = {
  id: ThemeId;
  name: string;
  description: string;
  background: string;
  backgroundAlt: string;
  surface: string;
  surfaceStrong: string;
  foreground: string;
  muted: string;
  accent: string;
  accentSoft: string;
  glow: string;
  border: string;
  gradient: [string, string, string];
};

export type LumaTrack = {
  id: string;
  title: string;
  artist: string;
  artwork: string | null;
  streamUrl: string;
  duration: number;
  genre: string;
  mood: string;
  permalink: string | null;
};

export type PlaybackSnapshot = {
  currentTime: number;
  duration: number;
  playing: boolean;
  buffering: boolean;
};

export type PersistedLumaState = {
  themeId: ThemeId;
  glassIntensity: number;
  glowStrength: number;
  reduceMotion: boolean;
  volume: number;
  favorites: LumaTrack[];
  recent: LumaTrack[];
  currentTrack: LumaTrack | null;
};
