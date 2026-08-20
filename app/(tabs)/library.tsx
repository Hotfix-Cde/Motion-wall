import { router } from "expo-router";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { AmbientBackground, EmptyLibrary, SectionHeading, TrackRow } from "@/components/luma-ui";
import { useLuma } from "@/lib/luma/app-provider";

export default function LibraryScreen() {
  const { theme, favorites, recent } = useLuma();
  return (
    <AmbientBackground>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View><Text style={[styles.kicker, { color: theme.glow }]}>YOUR LITTLE CORNER</Text><Text style={[styles.title, { color: theme.foreground }]}>Library</Text><Text style={[styles.subtitle, { color: theme.muted }]}>The sounds you chose to keep close.</Text></View>
        <SectionHeading title="Saved for later" />
        {favorites.length ? <View style={styles.list}>{favorites.map((track) => <TrackRow key={track.id} track={track} queue={favorites} />)}</View> : <EmptyLibrary title="Nothing saved yet" detail="When a track feels right, tap the heart. It will wait here for your next quiet moment." actionLabel="Explore music" onAction={() => router.push("/" as never)} />}
        {recent.length ? <><SectionHeading eyebrow="A gentle rewind" title="Recently played" /><View style={styles.list}>{recent.map((track) => <TrackRow key={track.id} track={track} queue={recent} subtle />)}</View></> : null}
      </ScrollView>
    </AmbientBackground>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 130, gap: 18 },
  kicker: { fontSize: 10, fontWeight: "800", letterSpacing: 1.4 },
  title: { marginTop: 5, fontSize: 36, lineHeight: 42, letterSpacing: -1.2, fontWeight: "800" },
  subtitle: { marginTop: 5, fontSize: 14, lineHeight: 20 },
  list: { gap: 5 },
});
