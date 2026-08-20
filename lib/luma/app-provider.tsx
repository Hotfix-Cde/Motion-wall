import AsyncStorage from "@react-native-async-storage/async-storage";
import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from "expo-audio";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type PropsWithChildren } from "react";
import { Platform } from "react-native";

import { haptic } from "./haptics";
import { DEFAULT_THEME, LUMA_THEMES } from "./theme";
import type { LumaTheme, LumaTrack, PersistedLumaState, PlaybackSnapshot, ThemeId } from "./types";

const STORAGE_KEY = "luma-v1-state";

type LumaContextValue = {
  hydrated: boolean;
  theme: LumaTheme;
  themeId: ThemeId;
  setThemeId: (themeId: ThemeId) => void;
  glassIntensity: number;
  setGlassIntensity: (value: number) => void;
  glowStrength: number;
  setGlowStrength: (value: number) => void;
  reduceMotion: boolean;
  setReduceMotion: (value: boolean) => void;
  volume: number;
  setVolume: (value: number) => void;
  favorites: LumaTrack[];
  recent: LumaTrack[];
  currentTrack: LumaTrack | null;
  queue: LumaTrack[];
  playback: PlaybackSnapshot;
  sleepMinutes: number;
  playTrack: (track: LumaTrack, queue?: LumaTrack[]) => void;
  togglePlayback: () => void;
  skip: (direction: "next" | "previous") => void;
  seekTo: (seconds: number) => Promise<void>;
  toggleFavorite: (track: LumaTrack) => void;
  isFavorite: (trackId: string) => boolean;
  setSleepMinutes: (minutes: number) => void;
};

const LumaContext = createContext<LumaContextValue | null>(null);

const DEFAULT_PLAYBACK: PlaybackSnapshot = { currentTime: 0, duration: 0, playing: false, buffering: false };

function within(value: number, minimum: number, maximum: number) {
  return Math.min(maximum, Math.max(minimum, value));
}

export function LumaProvider({ children }: PropsWithChildren) {
  const playerRef = useRef<AudioPlayer | null>(null);
  const sleepTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [themeId, setThemeId] = useState<ThemeId>(DEFAULT_THEME);
  const [glassIntensity, setGlassIntensity] = useState(62);
  const [glowStrength, setGlowStrength] = useState(72);
  const [reduceMotion, setReduceMotion] = useState(false);
  const [volume, setVolumeState] = useState(0.82);
  const [favorites, setFavorites] = useState<LumaTrack[]>([]);
  const [recent, setRecent] = useState<LumaTrack[]>([]);
  const [currentTrack, setCurrentTrack] = useState<LumaTrack | null>(null);
  const [queue, setQueue] = useState<LumaTrack[]>([]);
  const [playback, setPlayback] = useState<PlaybackSnapshot>(DEFAULT_PLAYBACK);
  const [sleepMinutes, setSleepMinutesState] = useState(0);

  useEffect(() => {
    void setAudioModeAsync({
      playsInSilentMode: true,
      shouldPlayInBackground: true,
      interruptionMode: "mixWithOthers",
      interruptionModeAndroid: "duckOthers",
    });
    const player = createAudioPlayer(null, { updateInterval: 300, keepAudioSessionActive: true });
    player.volume = volume;
    playerRef.current = player;
    const subscription = player.addListener("playbackStatusUpdate", (status) => {
      setPlayback({
        currentTime: status.currentTime,
        duration: status.duration,
        playing: status.playing,
        buffering: status.isBuffering,
      });
    });
    return () => {
      subscription.remove();
      player.remove();
      playerRef.current = null;
    };
  }, []);

  useEffect(() => {
    void AsyncStorage.getItem(STORAGE_KEY)
      .then((raw) => {
        if (!raw) return;
        const saved = JSON.parse(raw) as Partial<PersistedLumaState>;
        if (saved.themeId && LUMA_THEMES[saved.themeId]) setThemeId(saved.themeId);
        if (typeof saved.glassIntensity === "number") setGlassIntensity(within(saved.glassIntensity, 15, 95));
        if (typeof saved.glowStrength === "number") setGlowStrength(within(saved.glowStrength, 0, 100));
        if (typeof saved.reduceMotion === "boolean") setReduceMotion(saved.reduceMotion);
        if (typeof saved.volume === "number") setVolumeState(within(saved.volume, 0, 1));
        if (Array.isArray(saved.favorites)) setFavorites(saved.favorites);
        if (Array.isArray(saved.recent)) setRecent(saved.recent);
        if (saved.currentTrack) setCurrentTrack(saved.currentTrack);
      })
      .catch(() => undefined)
      .finally(() => setHydrated(true));
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    const saved: PersistedLumaState = {
      themeId,
      glassIntensity,
      glowStrength,
      reduceMotion,
      volume,
      favorites,
      recent,
      currentTrack,
    };
    void AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(saved));
  }, [currentTrack, favorites, glassIntensity, glowStrength, hydrated, recent, reduceMotion, themeId, volume]);

  useEffect(() => {
    if (playerRef.current) playerRef.current.volume = volume;
  }, [volume]);

  useEffect(() => {
    if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current);
    if (sleepMinutes <= 0) return;
    sleepTimerRef.current = setTimeout(() => {
      playerRef.current?.pause();
      setSleepMinutesState(0);
    }, sleepMinutes * 60 * 1000);
    return () => {
      if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current);
    };
  }, [sleepMinutes]);

  const setVolume = useCallback((value: number) => {
    const nextVolume = within(value, 0, 1);
    setVolumeState(nextVolume);
    if (playerRef.current) playerRef.current.volume = nextVolume;
  }, []);

  const playTrack = useCallback((track: LumaTrack, requestedQueue?: LumaTrack[]) => {
    const player = playerRef.current;
    if (!player) return;
    try {
      player.replace({ uri: track.streamUrl });
      player.volume = volume;
      player.play();
      setCurrentTrack(track);
      setQueue(requestedQueue && requestedQueue.length > 0 ? requestedQueue : [track]);
      setRecent((items) => [track, ...items.filter((item) => item.id !== track.id)].slice(0, 12));
      setPlayback((state) => ({ ...state, currentTime: 0, duration: track.duration, playing: true, buffering: true }));
      haptic.soft();
    } catch {
      setPlayback((state) => ({ ...state, playing: false, buffering: false }));
    }
  }, [volume]);

  const togglePlayback = useCallback(() => {
    const player = playerRef.current;
    if (!player || !currentTrack) return;
    if (player.playing) {
      player.pause();
      haptic.light();
    } else {
      player.play();
      haptic.soft();
    }
  }, [currentTrack]);

  const skip = useCallback((direction: "next" | "previous") => {
    if (!currentTrack || queue.length < 2) {
      void playerRef.current?.seekTo(0);
      return;
    }
    const currentIndex = Math.max(0, queue.findIndex((track) => track.id === currentTrack.id));
    const offset = direction === "next" ? 1 : -1;
    const nextIndex = (currentIndex + offset + queue.length) % queue.length;
    playTrack(queue[nextIndex], queue);
  }, [currentTrack, playTrack, queue]);

  const seekTo = useCallback(async (seconds: number) => {
    const player = playerRef.current;
    if (!player) return;
    try {
      await player.seekTo(within(seconds, 0, playback.duration || currentTrack?.duration || 0));
    } catch {
      return;
    }
  }, [currentTrack?.duration, playback.duration]);

  const toggleFavorite = useCallback((track: LumaTrack) => {
    setFavorites((items) => {
      const exists = items.some((item) => item.id === track.id);
      haptic.selection();
      return exists ? items.filter((item) => item.id !== track.id) : [track, ...items];
    });
  }, []);

  const setSleepMinutes = useCallback((minutes: number) => {
    setSleepMinutesState(minutes);
    haptic.selection();
  }, []);

  const value = useMemo<LumaContextValue>(() => ({
    hydrated,
    theme: LUMA_THEMES[themeId],
    themeId,
    setThemeId: (nextTheme) => { setThemeId(nextTheme); haptic.selection(); },
    glassIntensity,
    setGlassIntensity: (value) => setGlassIntensity(within(value, 15, 95)),
    glowStrength,
    setGlowStrength: (value) => setGlowStrength(within(value, 0, 100)),
    reduceMotion,
    setReduceMotion,
    volume,
    setVolume,
    favorites,
    recent,
    currentTrack,
    queue,
    playback,
    sleepMinutes,
    playTrack,
    togglePlayback,
    skip,
    seekTo,
    toggleFavorite,
    isFavorite: (trackId) => favorites.some((track) => track.id === trackId),
    setSleepMinutes,
  }), [currentTrack, favorites, glassIntensity, glowStrength, hydrated, playback, playTrack, queue, recent, reduceMotion, seekTo, setSleepMinutes, setVolume, sleepMinutes, skip, themeId, toggleFavorite, togglePlayback, volume]);

  return <LumaContext.Provider value={value}>{children}</LumaContext.Provider>;
}

export function useLuma() {
  const context = useContext(LumaContext);
  if (!context) throw new Error("useLuma must be used within LumaProvider");
  return context;
}
