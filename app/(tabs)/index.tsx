import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { router } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { AmbientBackground, GlassSurface, PrimaryButton, SectionHeading, TrackRow } from "@/components/luma-ui";
import { getTrendingTracks } from "@/lib/luma/catalog";
import { useLuma } from "@/lib/luma/app-provider";
import type { LumaTrack } from "@/lib/luma/types";

const MOODS = [
  { label: "Soft focus", icon: "blur-on", query: "ambient" },
  { label: "Cloud nap", icon: "bedtime", query: "chill" },
  { label: "Starwalk", icon: "nights-stay", query: "electronic" },
  { label: "Warm room", icon: "wb-sunny", query: "indie" },
] as const;

export default function ListenScreen() {
  const { theme, playTrack, currentTrack } = useLuma();
  const [tracks, setTracks] = useState<LumaTrack[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const load = () => {
    setLoading(true);
    setError(false);
    void getTrendingTracks()
      .then(setTracks)
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);
  const featured = tracks[0];

  return (
    <AmbientBackground>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <View>
            <Text style={[styles.wordmark, { color: theme.foreground }]}>luma</Text>
            <Text style={[styles.tagline, { color: theme.muted }]}>a softer place to listen</Text>
          </View>
          <Pressable onPress={() => router.push("/search" as never)} accessibilityRole="button" accessibilityLabel="Search music" style={({ pressed }) => [styles.searchButton, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}>
            <MaterialIcons name="search" size={22} color={theme.foreground} />
          </Pressable>
        </View>

        <SectionHeading eyebrow="Mood library" title="How do you want to feel?" />
        <View style={styles.moodGrid}>
          {MOODS.map((mood) => (
            <Pressable key={mood.label} onPress={() => router.push({ pathname: "/search", params: { q: mood.query } } as never)} accessibilityRole="button" accessibilityLabel={`Find ${mood.label} music`} style={({ pressed }) => [styles.moodCard, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}>
              <View style={[styles.moodIcon, { backgroundColor: `${theme.accent}25` }]}><MaterialIcons name={mood.icon} size={19} color={theme.accentSoft} /></View>
              <Text style={[styles.moodLabel, { color: theme.foreground }]}>{mood.label}</Text>
            </Pressable>
          ))}
        </View>

        <SectionHeading eyebrow="Just for this moment" title="A little lift" actionLabel="Search" onAction={() => router.push("/search" as never)} />
        {loading ? (
          <GlassSurface style={styles.loadingCard}><ActivityIndicator color={theme.accent} /><Text style={[styles.loadingText, { color: theme.muted }]}>Finding open music for you…</Text></GlassSurface>
        ) : featured ? (
          <GlassSurface style={styles.featureCard} strong>
            <View style={[styles.featureGlow, { backgroundColor: theme.glow, opacity: 0.19 }]} />
            <View style={styles.featureBadge}><MaterialIcons name="auto-awesome" size={15} color={theme.glow} /><Text style={[styles.featureBadgeText, { color: theme.accentSoft }]}>OPEN MUSIC PICK</Text></View>
            <Text numberOfLines={2} style={[styles.featureTitle, { color: theme.foreground }]}>{featured.title}</Text>
            <Text numberOfLines={1} style={[styles.featureArtist, { color: theme.muted }]}>{featured.artist} · {featured.genre}</Text>
            <PrimaryButton label={currentTrack?.id === featured.id ? "Open player" : "Play this softly"} icon="play-arrow" onPress={() => { playTrack(featured, tracks); router.push("/player" as never); }} />
          </GlassSurface>
        ) : (
          <GlassSurface style={styles.loadingCard}><Text style={[styles.loadingText, { color: theme.muted }]}>{error ? "The catalog is taking a small breather." : "No open tracks are available right now."}</Text><PrimaryButton label="Try again" icon="refresh" onPress={load} compact /></GlassSurface>
        )}

        <SectionHeading eyebrow="Trending in open music" title="Press play, stay awhile" />
        <View style={styles.trackList}>
          {tracks.slice(1, 7).map((track) => <TrackRow key={track.id} track={track} queue={tracks} />)}
          {!loading && tracks.length === 0 && !error ? <Text style={[styles.quietText, { color: theme.muted }]}>There is nothing new to play yet.</Text> : null}
        </View>
      </ScrollView>
    </AmbientBackground>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 130, gap: 18 },
  header: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 3 },
  wordmark: { fontSize: 31, lineHeight: 34, letterSpacing: -1.7, fontWeight: "800" },
  tagline: { fontSize: 13, marginTop: 2 },
  searchButton: { width: 46, height: 46, borderRadius: 17, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  moodGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  moodCard: { width: "48.5%", minHeight: 86, borderRadius: 22, borderWidth: 1, padding: 12, justifyContent: "space-between" },
  moodIcon: { width: 31, height: 31, borderRadius: 12, alignItems: "center", justifyContent: "center" },
  moodLabel: { fontSize: 14, fontWeight: "700" },
  featureCard: { minHeight: 224, padding: 20, justifyContent: "flex-end", gap: 9, overflow: "hidden" },
  featureGlow: { position: "absolute", width: 220, height: 220, borderRadius: 999, right: -60, top: -68 },
  featureBadge: { flexDirection: "row", gap: 5, alignItems: "center" },
  featureBadgeText: { fontSize: 10, letterSpacing: 1, fontWeight: "800" },
  featureTitle: { fontSize: 29, lineHeight: 34, letterSpacing: -0.8, fontWeight: "800", maxWidth: "89%" },
  featureArtist: { fontSize: 14, marginBottom: 7 },
  loadingCard: { minHeight: 132, padding: 20, justifyContent: "center", alignItems: "flex-start", gap: 13 },
  loadingText: { fontSize: 14, lineHeight: 20 },
  trackList: { gap: 5 },
  quietText: { paddingVertical: 20, textAlign: "center", fontSize: 14 },
  pressed: { opacity: 0.78, transform: [{ scale: 0.975 }] },
});
