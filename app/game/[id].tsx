import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { AmbientBackground, CompactPlayer, GlassSurface, PrimaryButton } from "@/components/luma-ui";
import { haptic } from "@/lib/luma/haptics";
import { useLuma } from "@/lib/luma/app-provider";

type GameId = "orbit" | "catch" | "garden";
const GAME_COPY: Record<GameId, { title: string; subtitle: string; action: string }> = {
  orbit: { title: "Orbit Cat", subtitle: "Each tap gives Nebula a tiny star-boost. There is nowhere to rush to.", action: "Give a gentle boost" },
  catch: { title: "Star Catcher", subtitle: "Touch a glowing star before it drifts away. The cat is only here to cheer you on.", action: "Begin a 45-second drift" },
  garden: { title: "Moon Garden", subtitle: "Plant a few light-blooming moonflowers. The little planet keeps every one.", action: "Plant a moonflower" },
};

const ORBIT_POSITIONS = [
  { left: "13%", top: "18%" }, { left: "72%", top: "15%" }, { left: "77%", top: "59%" }, { left: "22%", top: "68%" }, { left: "48%", top: "10%" }, { left: "62%", top: "75%" },
] as const;
const FLOWER_POSITIONS = [
  { left: "14%", bottom: "19%" }, { left: "72%", bottom: "22%" }, { left: "26%", bottom: "40%" }, { left: "63%", bottom: "42%" }, { left: "45%", bottom: "13%" }, { left: "12%", bottom: "50%" }, { left: "76%", bottom: "55%" }, { left: "43%", bottom: "57%" },
] as const;

function Cat({ tint, small = false }: { tint: string; small?: boolean }) {
  const size = small ? 50 : 75;
  return <View style={[styles.cat, { width: size, height: size * 0.72 }]}><View style={[styles.catEar, styles.leftEar, { borderBottomColor: tint }]} /><View style={[styles.catEar, styles.rightEar, { borderBottomColor: tint }]} /><View style={[styles.catFace, { backgroundColor: tint }]}><View style={styles.eye} /><View style={styles.eye} /><View style={styles.nose} /></View><View style={[styles.catTail, { borderColor: tint }]} /></View>;
}

function Star({ color, small = false }: { color: string; small?: boolean }) {
  return <View style={[styles.star, { width: small ? 13 : 26, height: small ? 13 : 26, backgroundColor: color, shadowColor: color }]} />;
}

function OrbitGame() {
  const { theme } = useLuma();
  const [boosts, setBoosts] = useState(0);
  const ringScale = 1 + (boosts % 5) * 0.035;
  return <><View style={styles.gameStats}><Text style={[styles.statValue, { color: theme.accentSoft }]}>{boosts}</Text><Text style={[styles.statLabel, { color: theme.muted }]}>soft boosts shared</Text></View><Pressable onPress={() => { setBoosts((value) => value + 1); haptic.soft(); }} accessibilityRole="button" accessibilityLabel="Give Nebula cat a gentle boost" style={({ pressed }) => [styles.world, { borderColor: theme.border }, pressed && styles.pressed]}><View style={[styles.orbitRing, { borderColor: theme.accent, transform: [{ scale: ringScale }, { rotate: "-17deg" }] }]} /><View style={[styles.orbitRingSmall, { borderColor: theme.glow, transform: [{ scale: 1 + (boosts % 3) * 0.04 }, { rotate: "26deg" }] }]} />{ORBIT_POSITIONS.slice(0, Math.min(ORBIT_POSITIONS.length, boosts + 2)).map((position, index) => <View key={`${index}-${boosts}`} style={[styles.floatingStar, position]}><Star color={index % 2 ? theme.glow : theme.accentSoft} small /></View>)}<View style={styles.catOrbit}><Cat tint={theme.accentSoft} /></View><View style={styles.worldHint}><MaterialIcons name="touch-app" size={17} color={theme.muted} /><Text style={[styles.worldHintText, { color: theme.muted }]}>tap anywhere to help Nebula float</Text></View></Pressable><PrimaryButton label="Give a gentle boost" icon="auto-awesome" onPress={() => { setBoosts((value) => value + 1); haptic.soft(); }} /></>;
}

function StarCatcherGame() {
  const { theme } = useLuma();
  const [activeStar, setActiveStar] = useState(0);
  const [found, setFound] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(45);
  const [running, setRunning] = useState(false);
  useEffect(() => {
    if (!running || secondsLeft <= 0) return;
    const timer = setInterval(() => setSecondsLeft((value) => value - 1), 1000);
    return () => clearInterval(timer);
  }, [running, secondsLeft]);
  useEffect(() => { if (secondsLeft <= 0) setRunning(false); }, [secondsLeft]);
  const reset = () => { setFound(0); setActiveStar(0); setSecondsLeft(45); setRunning(true); haptic.soft(); };
  const position = ORBIT_POSITIONS[activeStar % ORBIT_POSITIONS.length];
  return <><View style={styles.gameStats}><Text style={[styles.statValue, { color: theme.accentSoft }]}>{found}</Text><Text style={[styles.statLabel, { color: theme.muted }]}>stars found · {secondsLeft}s left</Text></View><View style={[styles.world, { borderColor: theme.border }]}><View style={styles.moon}><View style={[styles.moonCrater, styles.craterOne, { backgroundColor: `${theme.background}44` }]} /><View style={[styles.moonCrater, styles.craterTwo, { backgroundColor: `${theme.background}44` }]} /><View style={styles.moonCat}><Cat tint={theme.accentSoft} small /></View></View>{running ? <Pressable onPress={() => { setFound((value) => value + 1); setActiveStar((value) => value + 1); haptic.selection(); }} accessibilityRole="button" accessibilityLabel="Catch star" style={({ pressed }) => [styles.catchStar, position, pressed && styles.pressed]}><Star color={theme.glow} /></Pressable> : <View style={styles.catchPause}><MaterialIcons name={secondsLeft === 0 ? "celebration" : "auto-awesome"} size={28} color={theme.accentSoft} /><Text style={[styles.catchText, { color: theme.foreground }]}>{secondsLeft === 0 ? "That was a lovely little constellation." : "Start when the moment feels right."}</Text></View>}<Text style={[styles.worldHintText, styles.catcherHint, { color: theme.muted }]}>{running ? "touch the star, again and again" : "the cat waits on the moon"}</Text></View><PrimaryButton label={running ? "Start over softly" : secondsLeft === 0 ? "Drift once more" : "Begin a 45-second drift"} icon="play-arrow" onPress={reset} /></>;
}

function MoonGardenGame() {
  const { theme } = useLuma();
  const [flowers, setFlowers] = useState(0);
  const flowerNodes = useMemo(() => FLOWER_POSITIONS.slice(0, Math.min(flowers, FLOWER_POSITIONS.length)), [flowers]);
  const grow = () => { setFlowers((value) => Math.min(value + 1, FLOWER_POSITIONS.length)); haptic.selection(); };
  return <><View style={styles.gameStats}><Text style={[styles.statValue, { color: theme.accentSoft }]}>{flowers}</Text><Text style={[styles.statLabel, { color: theme.muted }]}>{flowers === 1 ? "moonflower growing" : "moonflowers growing"}</Text></View><Pressable onPress={grow} accessibilityRole="button" accessibilityLabel="Plant a moonflower" style={({ pressed }) => [styles.world, { borderColor: theme.border }, pressed && styles.pressed]}><View style={[styles.planet, { backgroundColor: theme.surfaceStrong, borderColor: theme.accent }]}><View style={[styles.planetGlow, { backgroundColor: theme.glow, opacity: 0.24 }]} /><View style={styles.planetCat}><Cat tint={theme.accentSoft} small /></View></View>{flowerNodes.map((position, index) => <View key={index} style={[styles.flower, position]}><View style={[styles.flowerPetal, styles.petalsOne, { backgroundColor: index % 2 ? theme.glow : theme.accent }]} /><View style={[styles.flowerPetal, styles.petalsTwo, { backgroundColor: index % 2 ? theme.accent : theme.glow }]} /><View style={[styles.flowerCenter, { backgroundColor: theme.accentSoft }]} /></View>)}{flowers < FLOWER_POSITIONS.length ? <View style={styles.worldHint}><MaterialIcons name="local-florist" size={17} color={theme.muted} /><Text style={[styles.worldHintText, { color: theme.muted }]}>tap the sky and a moonflower will appear</Text></View> : <View style={styles.worldHint}><MaterialIcons name="favorite" size={17} color={theme.accent} /><Text style={[styles.worldHintText, { color: theme.muted }]}>a small garden for a cat to dream in</Text></View>}</Pressable><PrimaryButton label={flowers >= FLOWER_POSITIONS.length ? "Let the garden rest" : "Plant a moonflower"} icon={flowers >= FLOWER_POSITIONS.length ? "favorite" : "local-florist"} onPress={grow} /></>;
}

export default function GameScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { theme } = useLuma();
  const gameId: GameId = id === "catch" || id === "garden" || id === "orbit" ? id : "orbit";
  const copy = GAME_COPY[gameId];
  return <AmbientBackground><View style={styles.screen}><View style={styles.header}><Pressable onPress={() => router.back()} accessibilityRole="button" accessibilityLabel="Return to game list" style={({ pressed }) => [styles.closeButton, { backgroundColor: theme.surface, borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="arrow-back" size={21} color={theme.foreground} /></Pressable><Text style={[styles.headerWord, { color: theme.muted }]}>LUMA PLAY</Text><View style={styles.headerSpacer} /></View><View style={styles.copy}><Text style={[styles.title, { color: theme.foreground }]}>{copy.title}</Text><Text style={[styles.subtitle, { color: theme.muted }]}>{copy.subtitle}</Text></View><GlassSurface style={styles.gameContainer} strong>{gameId === "orbit" ? <OrbitGame /> : gameId === "catch" ? <StarCatcherGame /> : <MoonGardenGame />}</GlassSurface><CompactPlayer /></View></AmbientBackground>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, padding: 20, paddingBottom: 14, gap: 15 },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  closeButton: { width: 43, height: 43, borderRadius: 16, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  headerWord: { fontSize: 10, letterSpacing: 1.2, fontWeight: "900" },
  headerSpacer: { width: 43 },
  copy: { gap: 5 },
  title: { fontSize: 30, lineHeight: 35, fontWeight: "800", letterSpacing: -0.9 },
  subtitle: { fontSize: 13, lineHeight: 19, maxWidth: "94%" },
  gameContainer: { flex: 1, minHeight: 390, padding: 16, justifyContent: "space-between" },
  gameStats: { alignItems: "center", gap: 2 },
  statValue: { fontSize: 32, lineHeight: 37, fontWeight: "800", fontVariant: ["tabular-nums"] },
  statLabel: { fontSize: 11 },
  world: { height: 286, width: "100%", borderRadius: 27, borderWidth: 1, overflow: "hidden", position: "relative", alignItems: "center", justifyContent: "center" },
  orbitRing: { width: 238, height: 132, borderRadius: 999, borderWidth: 1.5, position: "absolute" },
  orbitRingSmall: { width: 169, height: 212, borderRadius: 999, borderWidth: 1, position: "absolute" },
  floatingStar: { position: "absolute" },
  star: { borderRadius: 3, transform: [{ rotate: "45deg" }], shadowOpacity: 0.95, shadowRadius: 9, shadowOffset: { width: 0, height: 0 }, elevation: 5 },
  catOrbit: { transform: [{ rotate: "-14deg" }], marginTop: -5 },
  cat: { alignItems: "center", justifyContent: "flex-end", position: "relative" },
  catFace: { width: "70%", height: "63%", borderRadius: 999, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 12, zIndex: 2 },
  catEar: { position: "absolute", top: "9%", width: 0, height: 0, borderLeftWidth: 13, borderRightWidth: 13, borderBottomWidth: 20, borderLeftColor: "transparent", borderRightColor: "transparent", zIndex: 1 },
  leftEar: { left: "9%", transform: [{ rotate: "-19deg" }] },
  rightEar: { right: "9%", transform: [{ rotate: "19deg" }] },
  eye: { width: 5, height: 7, borderRadius: 4, backgroundColor: "#10152D" },
  nose: { width: 5, height: 4, borderRadius: 3, backgroundColor: "#F7F5FF", position: "absolute", bottom: "25%" },
  catTail: { position: "absolute", width: "31%", height: "39%", borderTopWidth: 6, borderRightWidth: 6, borderRadius: 20, right: "3%", bottom: "15%", transform: [{ rotate: "13deg" }] },
  worldHint: { position: "absolute", bottom: 14, left: 14, right: 14, flexDirection: "row", justifyContent: "center", alignItems: "center", gap: 5 },
  worldHintText: { fontSize: 11, textAlign: "center" },
  moon: { width: 160, height: 160, borderRadius: 80, backgroundColor: "rgba(255,255,255,0.12)", alignItems: "center", justifyContent: "center", overflow: "hidden" },
  moonCrater: { position: "absolute", borderRadius: 999 },
  craterOne: { width: 31, height: 31, left: 28, top: 36 },
  craterTwo: { width: 19, height: 19, right: 32, bottom: 39 },
  moonCat: { marginTop: 23 },
  catchStar: { position: "absolute", padding: 12, alignItems: "center", justifyContent: "center" },
  catchPause: { alignItems: "center", gap: 10, maxWidth: 220 },
  catchText: { fontSize: 14, lineHeight: 20, textAlign: "center", fontWeight: "700" },
  catcherHint: { position: "absolute", bottom: 15, left: 20, right: 20 },
  planet: { width: 177, height: 177, borderRadius: 90, borderWidth: 1, overflow: "hidden", alignItems: "center", justifyContent: "center" },
  planetGlow: { position: "absolute", width: 140, height: 90, borderRadius: 90, bottom: -43, left: 12 },
  planetCat: { marginTop: 22 },
  flower: { position: "absolute", width: 30, height: 30, alignItems: "center", justifyContent: "center" },
  flowerPetal: { position: "absolute", width: 11, height: 24, borderRadius: 10 },
  petalsOne: { transform: [{ rotate: "45deg" }] },
  petalsTwo: { transform: [{ rotate: "-45deg" }] },
  flowerCenter: { width: 9, height: 9, borderRadius: 5 },
  pressed: { opacity: 0.76, transform: [{ scale: 0.98 }] },
});
