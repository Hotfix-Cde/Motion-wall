import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { router } from "expo-router";
import { Image, Pressable, StyleSheet, Text, View } from "react-native";

import { AmbientBackground, GlassSurface } from "@/components/luma-ui";
import { useLuma } from "@/lib/luma/app-provider";

function seconds(value: number) { const min = Math.floor(value / 60); const sec = Math.floor(value % 60).toString().padStart(2, "0"); return `${min}:${sec}`; }

export default function PlayerScreen() {
  const { theme, currentTrack, playback, volume, setVolume, togglePlayback, skip, seekTo, toggleFavorite, isFavorite, sleepMinutes, setSleepMinutes } = useLuma();
  if (!currentTrack) { router.replace("/"); return null; }
  const duration = playback.duration || currentTrack.duration || 30;
  const progress = Math.min(1, Math.max(0, playback.currentTime / duration));
  const cycleSleep = () => setSleepMinutes(sleepMinutes === 0 ? 15 : sleepMinutes === 15 ? 30 : sleepMinutes === 30 ? 45 : 0);
  return <AmbientBackground><View style={styles.screen}><View style={styles.topbar}><Pressable onPress={() => router.back()} accessibilityRole="button" accessibilityLabel="Close player" style={({ pressed }) => [styles.topButton, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="keyboard-arrow-down" size={27} color={theme.foreground} /></Pressable><View><Text style={[styles.playingLabel, { color: theme.glow }]}>NOW PLAYING</Text><Text numberOfLines={1} style={[styles.queueLabel, { color: theme.muted }]}>{sleepMinutes ? `sleeping in ${sleepMinutes} min` : "open music, in the moment"}</Text></View><Pressable onPress={() => toggleFavorite(currentTrack)} accessibilityRole="button" accessibilityLabel={isFavorite(currentTrack.id) ? "Remove from library" : "Save to library"} style={({ pressed }) => [styles.topButton, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name={isFavorite(currentTrack.id) ? "favorite" : "favorite-border"} size={21} color={isFavorite(currentTrack.id) ? theme.accent : theme.foreground} /></Pressable></View><GlassSurface style={styles.artworkFrame} strong>{currentTrack.artwork ? <Image source={{ uri: currentTrack.artwork }} style={styles.artwork} /> : <View style={[styles.artworkFallback, { backgroundColor: theme.accent }]}><MaterialIcons name="music-note" size={84} color={theme.background} /></View>}<View style={[styles.artworkLight, { backgroundColor: theme.glow, opacity: 0.28 }]} /></GlassSurface><View style={styles.trackInfo}><Text numberOfLines={2} style={[styles.trackTitle, { color: theme.foreground }]}>{currentTrack.title}</Text><Text numberOfLines={1} style={[styles.artist, { color: theme.muted }]}>{currentTrack.artist} · {currentTrack.genre}</Text></View><View><Pressable onPress={(event) => { void seekTo((event.nativeEvent.locationX / 320) * duration); }} accessibilityRole="adjustable" accessibilityLabel="Playback position" style={[styles.timelineHit, { backgroundColor: theme.border }]}><View style={[styles.timelineFill, { backgroundColor: theme.glow, width: `${progress * 100}%` }]} /><View style={[styles.timelineKnob, { backgroundColor: theme.foreground, left: `${progress * 100}%` }]} /></Pressable><View style={styles.timeLabels}><Text style={[styles.time, { color: theme.muted }]}>{seconds(playback.currentTime)}</Text><Text style={[styles.time, { color: theme.muted }]}>{seconds(duration)}</Text></View></View><View style={styles.transport}><Pressable onPress={() => skip("previous")} accessibilityRole="button" accessibilityLabel="Previous track" style={({ pressed }) => [styles.transportSmall, pressed && styles.pressed]}><MaterialIcons name="skip-previous" size={31} color={theme.foreground} /></Pressable><Pressable onPress={togglePlayback} accessibilityRole="button" accessibilityLabel={playback.playing ? "Pause" : "Play"} style={({ pressed }) => [styles.playButton, { backgroundColor: theme.accent }, pressed && styles.pressed]}><MaterialIcons name={playback.playing ? "pause" : "play-arrow"} size={39} color={theme.background} /></Pressable><Pressable onPress={() => skip("next")} accessibilityRole="button" accessibilityLabel="Next track" style={({ pressed }) => [styles.transportSmall, pressed && styles.pressed]}><MaterialIcons name="skip-next" size={31} color={theme.foreground} /></Pressable></View><View style={styles.bottomControls}><Pressable onPress={cycleSleep} accessibilityRole="button" accessibilityLabel="Set sleep timer" style={({ pressed }) => [styles.controlPill, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="bedtime" size={17} color={theme.accentSoft} /><Text style={[styles.controlText, { color: theme.foreground }]}>{sleepMinutes ? `${sleepMinutes} min` : "Sleep"}</Text></Pressable><View style={[styles.volumePill, { backgroundColor: theme.surface, borderColor: theme.border }]}><Pressable onPress={() => setVolume(volume - 0.1)} accessibilityRole="button" accessibilityLabel="Decrease volume" style={styles.volumeTap}><MaterialIcons name="volume-down" size={18} color={theme.muted} /></Pressable><Text style={[styles.controlText, { color: theme.foreground }]}>{Math.round(volume * 100)}%</Text><Pressable onPress={() => setVolume(volume + 0.1)} accessibilityRole="button" accessibilityLabel="Increase volume" style={styles.volumeTap}><MaterialIcons name="volume-up" size={18} color={theme.accentSoft} /></Pressable></View></View></View></AmbientBackground>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 22, paddingTop: 16, paddingBottom: 26, justifyContent: "space-between" },
  topbar: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  topButton: { width: 44, height: 44, borderRadius: 16, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  playingLabel: { fontSize: 10, textAlign: "center", letterSpacing: 1.3, fontWeight: "900" },
  queueLabel: { fontSize: 11, maxWidth: 160, marginTop: 3 },
  artworkFrame: { width: "100%", aspectRatio: 1, maxHeight: 345, alignSelf: "center", borderRadius: 34, overflow: "hidden" },
  artwork: { width: "100%", height: "100%" },
  artworkFallback: { flex: 1, alignItems: "center", justifyContent: "center" },
  artworkLight: { position: "absolute", width: 190, height: 190, borderRadius: 99, right: -70, top: -55 },
  trackInfo: { gap: 5 },
  trackTitle: { fontSize: 27, lineHeight: 32, letterSpacing: -0.8, fontWeight: "800" },
  artist: { fontSize: 14 },
  timelineHit: { height: 5, borderRadius: 4, marginTop: 4 },
  timelineFill: { height: 5, borderRadius: 4 },
  timelineKnob: { position: "absolute", top: -4, width: 13, height: 13, marginLeft: -6.5, borderRadius: 7 },
  timeLabels: { flexDirection: "row", justifyContent: "space-between", marginTop: 7 },
  time: { fontSize: 11, fontVariant: ["tabular-nums"] },
  transport: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 28 },
  transportSmall: { width: 46, height: 46, alignItems: "center", justifyContent: "center" },
  playButton: { width: 72, height: 72, borderRadius: 36, alignItems: "center", justifyContent: "center" },
  bottomControls: { flexDirection: "row", justifyContent: "space-between", gap: 10 },
  controlPill: { minHeight: 42, borderRadius: 16, borderWidth: 1, paddingHorizontal: 13, flexDirection: "row", alignItems: "center", gap: 7 },
  volumePill: { minHeight: 42, borderRadius: 16, borderWidth: 1, paddingHorizontal: 7, flexDirection: "row", alignItems: "center", gap: 3 },
  volumeTap: { width: 27, height: 36, alignItems: "center", justifyContent: "center" },
  controlText: { fontSize: 12, fontWeight: "800" },
  pressed: { opacity: 0.76, transform: [{ scale: 0.97 }] },
});
