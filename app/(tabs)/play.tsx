import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { router } from "expo-router";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { AmbientBackground, GlassSurface, SectionHeading } from "@/components/luma-ui";
import { haptic } from "@/lib/luma/haptics";
import { useLuma } from "@/lib/luma/app-provider";

const GAMES = [
  { id: "orbit", number: "01", title: "Orbit Cat", detail: "Give a cosmic cat a little boost through soft orbital rings.", icon: "flare" as const },
  { id: "catch", number: "02", title: "Star Catcher", detail: "Collect drifting stars while a sleepy cat keeps watch.", icon: "auto-awesome" as const },
  { id: "garden", number: "03", title: "Moon Garden", detail: "Plant light-blooming flowers around a tiny moonlit planet.", icon: "local-florist" as const },
] as const;

export default function PlayScreen() {
  const { theme, currentTrack, playback } = useLuma();
  return <AmbientBackground><ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}><View><Text style={[styles.kicker, { color: theme.glow }]}>PLAY WITHOUT PRESSURE</Text><Text style={[styles.title, { color: theme.foreground }]}>Tiny worlds</Text><Text style={[styles.subtitle, { color: theme.muted }]}>Slow games made to sit beside your music, never take over it.</Text></View>{currentTrack ? <GlassSurface style={styles.nowPlaying}><View style={[styles.nowIcon, { backgroundColor: `${theme.glow}24` }]}><MaterialIcons name={playback.playing ? "graphic-eq" : "music-note"} size={21} color={theme.glow} /></View><View style={styles.nowCopy}><Text numberOfLines={1} style={[styles.nowTitle, { color: theme.foreground }]}>{playback.playing ? "Music stays with you" : "Music is paused"}</Text><Text numberOfLines={1} style={[styles.nowDetail, { color: theme.muted }]}>{currentTrack.title} · {currentTrack.artist}</Text></View></GlassSurface> : <GlassSurface style={styles.nowPlaying}><View style={[styles.nowIcon, { backgroundColor: `${theme.accent}24` }]}><MaterialIcons name="headphones" size={21} color={theme.accentSoft} /></View><View style={styles.nowCopy}><Text style={[styles.nowTitle, { color: theme.foreground }]}>Bring a song along</Text><Text style={[styles.nowDetail, { color: theme.muted }]}>Pick a track from Listen, then return to play.</Text></View></GlassSurface>}<SectionHeading title="Choose a small adventure" /><View style={styles.games}>{GAMES.map((game, index) => <Pressable key={game.id} onPress={() => { haptic.soft(); router.push({ pathname: "/game/[id]", params: { id: game.id } } as never); }} accessibilityRole="button" accessibilityLabel={`Open ${game.title}`} style={({ pressed }) => [styles.gameCard, { backgroundColor: index === 1 ? `${theme.accent}24` : theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><View style={styles.gameTop}><Text style={[styles.number, { color: theme.glow }]}>{game.number}</Text><View style={[styles.gameIcon, { backgroundColor: `${theme.accent}25` }]}><MaterialIcons name={game.icon} size={22} color={theme.accentSoft} /></View></View><Text style={[styles.gameTitle, { color: theme.foreground }]}>{game.title}</Text><Text style={[styles.gameDetail, { color: theme.muted }]}>{game.detail}</Text><View style={[styles.playPill, { backgroundColor: theme.accent }]}><Text style={[styles.playPillText, { color: theme.background }]}>Enter gently</Text><MaterialIcons name="arrow-forward" size={15} color={theme.background} /></View></Pressable>)}</View><Text style={[styles.footer, { color: theme.muted }]}>There are no levels to lose, no ads, and no scores to compare. Only small interactions, made for a softer mind.</Text></ScrollView></AmbientBackground>;
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 130, gap: 18 },
  kicker: { fontSize: 10, fontWeight: "800", letterSpacing: 1.4 },
  title: { marginTop: 5, fontSize: 36, lineHeight: 42, letterSpacing: -1.2, fontWeight: "800" },
  subtitle: { marginTop: 5, fontSize: 14, lineHeight: 20 },
  nowPlaying: { minHeight: 74, padding: 13, flexDirection: "row", gap: 11, alignItems: "center" },
  nowIcon: { width: 42, height: 42, borderRadius: 15, alignItems: "center", justifyContent: "center" },
  nowCopy: { flex: 1, minWidth: 0 },
  nowTitle: { fontSize: 14, fontWeight: "700" },
  nowDetail: { marginTop: 3, fontSize: 11 },
  games: { gap: 10 },
  gameCard: { minHeight: 177, borderRadius: 26, padding: 18, borderWidth: 1, overflow: "hidden" },
  gameTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  number: { fontSize: 11, fontWeight: "900", letterSpacing: 1.2 },
  gameIcon: { width: 42, height: 42, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  gameTitle: { marginTop: 15, fontSize: 21, fontWeight: "800", letterSpacing: -0.4 },
  gameDetail: { marginTop: 5, fontSize: 13, lineHeight: 19, maxWidth: "88%" },
  playPill: { marginTop: 15, paddingHorizontal: 12, minHeight: 34, borderRadius: 13, alignSelf: "flex-start", flexDirection: "row", gap: 5, alignItems: "center" },
  playPillText: { fontSize: 11, fontWeight: "900" },
  footer: { fontSize: 12, lineHeight: 18, textAlign: "center", paddingHorizontal: 16 },
  pressed: { opacity: 0.76, transform: [{ scale: 0.98 }] },
});
