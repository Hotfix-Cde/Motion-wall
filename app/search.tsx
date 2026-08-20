import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { router, useLocalSearchParams } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { AmbientBackground, GlassSurface, SectionHeading, TrackRow } from "@/components/luma-ui";
import { searchTracks } from "@/lib/luma/catalog";
import { useLuma } from "@/lib/luma/app-provider";
import type { LumaTrack } from "@/lib/luma/types";

export default function SearchScreen() {
  const { q } = useLocalSearchParams<{ q?: string }>();
  const { theme } = useLuma();
  const [query, setQuery] = useState(q ?? "");
  const [results, setResults] = useState<LumaTrack[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Search the open catalog for an artist, genre, or tiny mood.");

  const runSearch = useCallback((nextQuery = query) => {
    const term = nextQuery.trim();
    if (term.length < 2) { setResults([]); setMessage("Try at least two letters. ‘ambient’, ‘cat’, or an artist name all work."); return; }
    setLoading(true);
    setMessage("");
    void searchTracks(term).then((tracks) => { setResults(tracks); if (!tracks.length) setMessage("No playable open tracks showed up. Try another small mood."); }).catch(() => setMessage("The open catalog is taking a moment. Please try again.")).finally(() => setLoading(false));
  }, [query]);

  useEffect(() => { if (q) runSearch(q); }, [q, runSearch]);
  return <AmbientBackground><ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}><View style={styles.topRow}><Pressable onPress={() => router.back()} accessibilityRole="button" accessibilityLabel="Go back" style={({ pressed }) => [styles.back, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="arrow-back" size={21} color={theme.foreground} /></Pressable><Text style={[styles.title, { color: theme.foreground }]}>Find a feeling</Text><View style={styles.topSpacer} /></View><Text style={[styles.subtitle, { color: theme.muted }]}>Music from artists who make their tracks available in the open Audius catalog.</Text><GlassSurface style={styles.searchField} strong><MaterialIcons name="search" size={20} color={theme.accentSoft} /><TextInput value={query} onChangeText={setQuery} onSubmitEditing={() => runSearch()} placeholder="Artist, track, or mood" placeholderTextColor={theme.muted} returnKeyType="search" autoFocus={!q} style={[styles.input, { color: theme.foreground }]} /><Pressable onPress={() => runSearch()} accessibilityRole="button" accessibilityLabel="Run music search" style={({ pressed }) => [styles.searchGo, { backgroundColor: theme.accent }, pressed && styles.pressed]}><MaterialIcons name="arrow-forward" size={18} color={theme.background} /></Pressable></GlassSurface>{loading ? <View style={styles.loader}><ActivityIndicator color={theme.accent} /><Text style={[styles.loaderText, { color: theme.muted }]}>Listening for results…</Text></View> : null}{results.length ? <><SectionHeading eyebrow="Open catalog" title={`${results.length} little discoveries`} /><View style={styles.results}>{results.map((track) => <TrackRow key={track.id} track={track} queue={results} />)}</View></> : !loading ? <GlassSurface style={styles.empty}><MaterialIcons name="music-note" size={30} color={theme.accent} /><Text style={[styles.emptyText, { color: theme.muted }]}>{message}</Text></GlassSurface> : null}</ScrollView></AmbientBackground>;
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 50, gap: 18 },
  topRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  back: { width: 44, height: 44, borderRadius: 16, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  title: { fontSize: 22, fontWeight: "800", letterSpacing: -0.5 },
  topSpacer: { width: 44 },
  subtitle: { fontSize: 13, lineHeight: 19, marginTop: -8 },
  searchField: { minHeight: 60, padding: 9, paddingLeft: 15, borderRadius: 21, flexDirection: "row", alignItems: "center", gap: 10 },
  input: { flex: 1, fontSize: 15, minHeight: 42 },
  searchGo: { width: 40, height: 40, borderRadius: 15, alignItems: "center", justifyContent: "center" },
  loader: { alignItems: "center", gap: 10, paddingVertical: 32 },
  loaderText: { fontSize: 13 },
  results: { gap: 5 },
  empty: { minHeight: 150, padding: 22, alignItems: "center", justifyContent: "center", gap: 12 },
  emptyText: { fontSize: 14, lineHeight: 20, textAlign: "center", maxWidth: 270 },
  pressed: { opacity: 0.76, transform: [{ scale: 0.975 }] },
});
