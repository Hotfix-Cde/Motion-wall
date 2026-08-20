import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { router } from "expo-router";
import { Image, Platform, Pressable, StyleSheet, Text, View, type StyleProp, type ViewStyle } from "react-native";

import { haptic } from "@/lib/luma/haptics";
import { useLuma } from "@/lib/luma/app-provider";
import type { LumaTrack } from "@/lib/luma/types";

export function AmbientBackground({ children }: { children: React.ReactNode }) {
  const { theme, glowStrength, reduceMotion } = useLuma();
  const glowOpacity = (0.16 + glowStrength / 430) * (reduceMotion ? 0.78 : 1);
  return (
    <View style={[styles.ambient, { backgroundColor: theme.background }]}>
      <LinearGradient colors={theme.gradient} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={StyleSheet.absoluteFill} />
      <View style={[styles.orb, styles.orbOne, { backgroundColor: theme.accent, opacity: glowOpacity }]} />
      <View style={[styles.orb, styles.orbTwo, { backgroundColor: theme.glow, opacity: glowOpacity * 0.9 }]} />
      <View style={styles.content}>{children}</View>
    </View>
  );
}

export function GlassSurface({ children, style, strong = false }: { children: React.ReactNode; style?: StyleProp<ViewStyle>; strong?: boolean }) {
  const { theme, glassIntensity } = useLuma();
  return (
    <BlurView
      intensity={glassIntensity}
      tint="dark"
      experimentalBlurMethod={Platform.OS === "android" ? "dimezisBlurView" : undefined}
      style={[styles.glass, { backgroundColor: strong ? theme.surfaceStrong : theme.surface, borderColor: theme.border }, style]}
    >
      {children}
    </BlurView>
  );
}

export function SectionHeading({ eyebrow, title, actionLabel, onAction }: { eyebrow?: string; title: string; actionLabel?: string; onAction?: () => void }) {
  const { theme } = useLuma();
  return (
    <View style={styles.sectionHeading}>
      <View style={styles.sectionCopy}>
        {eyebrow ? <Text style={[styles.eyebrow, { color: theme.glow }]}>{eyebrow.toUpperCase()}</Text> : null}
        <Text style={[styles.sectionTitle, { color: theme.foreground }]}>{title}</Text>
      </View>
      {actionLabel && onAction ? (
        <Pressable onPress={() => { haptic.light(); onAction(); }} style={({ pressed }) => [styles.textAction, pressed && styles.pressed]} accessibilityRole="button" accessibilityLabel={actionLabel}>
          <Text style={[styles.textActionLabel, { color: theme.accentSoft }]}>{actionLabel}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

export function PrimaryButton({ label, icon, onPress, compact = false }: { label: string; icon?: keyof typeof MaterialIcons.glyphMap; onPress: () => void; compact?: boolean }) {
  const { theme } = useLuma();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={() => { haptic.soft(); onPress(); }}
      style={({ pressed }) => [styles.primaryButton, compact && styles.primaryCompact, { backgroundColor: theme.accent }, pressed && styles.pressed]}
    >
      {icon ? <MaterialIcons name={icon} color={theme.background} size={18} /> : null}
      <Text style={[styles.primaryText, { color: theme.background }]}>{label}</Text>
    </Pressable>
  );
}

function Artwork({ track, size = 54 }: { track: LumaTrack; size?: number }) {
  const { theme } = useLuma();
  if (track.artwork) return <Image source={{ uri: track.artwork }} style={{ width: size, height: size, borderRadius: Math.round(size * 0.3), backgroundColor: theme.surfaceStrong }} />;
  return (
    <LinearGradient colors={[theme.accent, theme.glow]} style={[styles.artworkFallback, { width: size, height: size, borderRadius: Math.round(size * 0.3) }]}>
      <MaterialIcons name="music-note" size={Math.round(size * 0.48)} color={theme.background} />
    </LinearGradient>
  );
}

export function TrackRow({ track, queue, subtle = false }: { track: LumaTrack; queue?: LumaTrack[]; subtle?: boolean }) {
  const { theme, playTrack, currentTrack, playback, toggleFavorite, isFavorite } = useLuma();
  const active = currentTrack?.id === track.id;
  const playing = active && playback.playing;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Play ${track.title} by ${track.artist}`}
      onPress={() => { playTrack(track, queue); router.push("/player" as never); }}
      style={({ pressed }) => [styles.trackRow, !subtle && { backgroundColor: "rgba(255,255,255,0.035)" }, pressed && styles.pressed]}
    >
      <Artwork track={track} />
      <View style={styles.trackCopy}>
        <Text numberOfLines={1} style={[styles.trackTitle, { color: active ? theme.accentSoft : theme.foreground }]}>{track.title}</Text>
        <Text numberOfLines={1} style={[styles.trackMeta, { color: theme.muted }]}>{track.artist} · {track.genre}</Text>
      </View>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={isFavorite(track.id) ? `Remove ${track.title} from library` : `Save ${track.title} to library`}
        onPress={(event) => { event.stopPropagation(); toggleFavorite(track); }}
        style={({ pressed }) => [styles.rowIcon, pressed && styles.pressed]}
      >
        <MaterialIcons name={isFavorite(track.id) ? "favorite" : "favorite-border"} size={20} color={isFavorite(track.id) ? theme.accent : theme.muted} />
      </Pressable>
      <View style={[styles.playOrb, { borderColor: active ? theme.accent : theme.border, backgroundColor: active ? theme.accent : "rgba(255,255,255,0.06)" }]}>
        <MaterialIcons name={playing ? "pause" : "play-arrow"} size={19} color={active ? theme.background : theme.foreground} />
      </View>
    </Pressable>
  );
}

export function CompactPlayer() {
  const { theme, currentTrack, playback, togglePlayback } = useLuma();
  if (!currentTrack) return null;
  const progress = Math.min(100, Math.max(0, (playback.currentTime / (playback.duration || currentTrack.duration || 1)) * 100));
  return (
    <Pressable onPress={() => router.push("/player" as never)} style={({ pressed }) => [styles.miniPlayer, { backgroundColor: theme.surfaceStrong, borderColor: theme.border }, pressed && styles.pressed]} accessibilityRole="button" accessibilityLabel={`Open player for ${currentTrack.title}`}>
      <View style={[styles.progressTrack, { backgroundColor: theme.border }]}><View style={[styles.progressFill, { backgroundColor: theme.glow, width: `${progress}%` }]} /></View>
      <Artwork track={currentTrack} size={40} />
      <View style={styles.miniCopy}>
        <Text numberOfLines={1} style={[styles.miniTitle, { color: theme.foreground }]}>{currentTrack.title}</Text>
        <Text numberOfLines={1} style={[styles.miniArtist, { color: theme.muted }]}>{currentTrack.artist}</Text>
      </View>
      <Pressable
        onPress={(event) => { event.stopPropagation(); togglePlayback(); }}
        accessibilityRole="button"
        accessibilityLabel={playback.playing ? "Pause" : "Play"}
        style={({ pressed }) => [styles.miniPause, { backgroundColor: theme.accent }, pressed && styles.pressed]}
      >
        <MaterialIcons name={playback.playing ? "pause" : "play-arrow"} size={19} color={theme.background} />
      </Pressable>
    </Pressable>
  );
}

export function EmptyLibrary({ title, detail, actionLabel, onAction }: { title: string; detail: string; actionLabel: string; onAction: () => void }) {
  const { theme } = useLuma();
  return (
    <GlassSurface style={styles.emptyCard}>
      <View style={[styles.emptyIcon, { backgroundColor: `${theme.accent}24` }]}><MaterialIcons name="auto-awesome" color={theme.accentSoft} size={25} /></View>
      <Text style={[styles.emptyTitle, { color: theme.foreground }]}>{title}</Text>
      <Text style={[styles.emptyDetail, { color: theme.muted }]}>{detail}</Text>
      <PrimaryButton label={actionLabel} onPress={onAction} compact />
    </GlassSurface>
  );
}

const styles = StyleSheet.create({
  ambient: { flex: 1, overflow: "hidden" },
  content: { flex: 1 },
  orb: { position: "absolute", borderRadius: 999, transform: [{ rotate: "-18deg" }] },
  orbOne: { width: 290, height: 290, right: -120, top: 44 },
  orbTwo: { width: 250, height: 250, left: -115, bottom: 48 },
  glass: { borderWidth: 1, borderRadius: 26, overflow: "hidden" },
  sectionHeading: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-end", marginBottom: 12 },
  sectionCopy: { flexShrink: 1 },
  eyebrow: { fontSize: 10, fontWeight: "800", letterSpacing: 1.35, marginBottom: 5 },
  sectionTitle: { fontSize: 23, lineHeight: 28, fontWeight: "700", letterSpacing: -0.5 },
  textAction: { minHeight: 40, alignItems: "flex-end", justifyContent: "center", paddingLeft: 12 },
  textActionLabel: { fontSize: 14, fontWeight: "700" },
  primaryButton: { minHeight: 48, borderRadius: 18, paddingHorizontal: 18, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8 },
  primaryCompact: { minHeight: 42, borderRadius: 15, paddingHorizontal: 15, alignSelf: "flex-start" },
  primaryText: { fontSize: 14, fontWeight: "800" },
  pressed: { opacity: 0.78, transform: [{ scale: 0.975 }] },
  artworkFallback: { alignItems: "center", justifyContent: "center", overflow: "hidden" },
  trackRow: { minHeight: 76, borderRadius: 22, padding: 10, flexDirection: "row", alignItems: "center", gap: 11 },
  trackCopy: { flex: 1, minWidth: 0 },
  trackTitle: { fontSize: 15, lineHeight: 20, fontWeight: "700" },
  trackMeta: { marginTop: 3, fontSize: 12, lineHeight: 16 },
  rowIcon: { width: 38, height: 44, alignItems: "center", justifyContent: "center" },
  playOrb: { width: 36, height: 36, borderRadius: 18, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  miniPlayer: { marginHorizontal: 14, marginTop: 7, minHeight: 62, padding: 9, borderRadius: 20, borderWidth: 1, flexDirection: "row", alignItems: "center", gap: 10, overflow: "hidden" },
  progressTrack: { position: "absolute", left: 10, right: 10, top: 0, height: 2, borderRadius: 99, overflow: "hidden" },
  progressFill: { height: 2, borderRadius: 99 },
  miniCopy: { flex: 1, minWidth: 0 },
  miniTitle: { fontSize: 13, fontWeight: "700" },
  miniArtist: { fontSize: 11, marginTop: 2 },
  miniPause: { width: 37, height: 37, borderRadius: 18.5, alignItems: "center", justifyContent: "center" },
  emptyCard: { padding: 22, alignItems: "flex-start", gap: 11 },
  emptyIcon: { height: 45, width: 45, borderRadius: 16, justifyContent: "center", alignItems: "center" },
  emptyTitle: { fontSize: 18, fontWeight: "700" },
  emptyDetail: { fontSize: 14, lineHeight: 20, maxWidth: 280 },
});
