import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Pressable, ScrollView, StyleSheet, Switch, Text, View } from "react-native";

import { AmbientBackground, GlassSurface, SectionHeading } from "@/components/luma-ui";
import { useLuma } from "@/lib/luma/app-provider";
import { LUMA_THEMES } from "@/lib/luma/theme";
import type { ThemeId } from "@/lib/luma/types";

function Stepper({ label, detail, value, suffix, onDecrease, onIncrease }: { label: string; detail: string; value: number; suffix: string; onDecrease: () => void; onIncrease: () => void }) {
  const { theme } = useLuma();
  return <GlassSurface style={styles.stepper}><View style={styles.stepperCopy}><Text style={[styles.stepperLabel, { color: theme.foreground }]}>{label}</Text><Text style={[styles.stepperDetail, { color: theme.muted }]}>{detail}</Text></View><View style={styles.stepperControls}><Pressable accessibilityRole="button" accessibilityLabel={`Decrease ${label}`} onPress={onDecrease} style={({ pressed }) => [styles.stepperButton, { borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="remove" size={19} color={theme.foreground} /></Pressable><Text style={[styles.stepperValue, { color: theme.accentSoft }]}>{value}{suffix}</Text><Pressable accessibilityRole="button" accessibilityLabel={`Increase ${label}`} onPress={onIncrease} style={({ pressed }) => [styles.stepperButton, { borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="add" size={19} color={theme.foreground} /></Pressable></View></GlassSurface>;
}

export default function ThemesScreen() {
  const { theme, themeId, setThemeId, glassIntensity, setGlassIntensity, glowStrength, setGlowStrength, reduceMotion, setReduceMotion } = useLuma();
  return (
    <AmbientBackground>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View><Text style={[styles.kicker, { color: theme.glow }]}>PERSONAL ATMOSPHERE</Text><Text style={[styles.title, { color: theme.foreground }]}>Themes</Text><Text style={[styles.subtitle, { color: theme.muted }]}>Make this small corner feel like yours.</Text></View>
        <GlassSurface style={styles.preview} strong><View style={[styles.previewLight, { backgroundColor: theme.glow, opacity: glowStrength / 135 }]} /><Text style={[styles.previewTiny, { color: theme.accentSoft }]}>LIVE PREVIEW</Text><Text style={[styles.previewTitle, { color: theme.foreground }]}>{theme.name}</Text><Text style={[styles.previewDetail, { color: theme.muted }]}>{theme.description}</Text><View style={styles.previewDots}>{[theme.accent, theme.glow, theme.foreground].map((color) => <View key={color} style={[styles.dot, { backgroundColor: color }]} />)}</View></GlassSurface>
        <SectionHeading title="Choose a feeling" />
        <View style={styles.themeGrid}>{(Object.keys(LUMA_THEMES) as ThemeId[]).map((id) => { const option = LUMA_THEMES[id]; const selected = id === themeId; return <Pressable key={id} accessibilityRole="button" accessibilityLabel={`Apply ${option.name} theme`} onPress={() => setThemeId(id)} style={({ pressed }) => [styles.themeCard, { backgroundColor: option.backgroundAlt, borderColor: selected ? option.glow : option.border }, selected && { borderWidth: 2 }, pressed && styles.pressed]}><View style={[styles.swatch, { backgroundColor: option.accent }]}><View style={[styles.swatchGlow, { backgroundColor: option.glow }]} /></View><Text style={[styles.themeName, { color: option.foreground }]}>{option.name}</Text><Text numberOfLines={2} style={[styles.themeDescription, { color: option.muted }]}>{option.description}</Text>{selected ? <View style={[styles.selectedPill, { backgroundColor: option.glow }]}><MaterialIcons name="check" size={13} color={option.background} /><Text style={[styles.selectedText, { color: option.background }]}>ACTIVE</Text></View> : null}</Pressable>; })}</View>
        <SectionHeading title="Fine tune" />
        <View style={styles.controls}><Stepper label="Glass depth" detail="Controls how misty the floating surfaces feel." value={glassIntensity} suffix="%" onDecrease={() => setGlassIntensity(glassIntensity - 8)} onIncrease={() => setGlassIntensity(glassIntensity + 8)} /><Stepper label="Glow" detail="The brightness of your ambient light." value={glowStrength} suffix="%" onDecrease={() => setGlowStrength(glowStrength - 10)} onIncrease={() => setGlowStrength(glowStrength + 10)} /><GlassSurface style={styles.motionControl}><View style={styles.stepperCopy}><Text style={[styles.stepperLabel, { color: theme.foreground }]}>Reduce motion</Text><Text style={[styles.stepperDetail, { color: theme.muted }]}>Keep the calm, with less drifting light.</Text></View><Switch value={reduceMotion} onValueChange={setReduceMotion} trackColor={{ false: theme.border, true: theme.accent }} thumbColor={theme.foreground} /></GlassSurface></View>
      </ScrollView>
    </AmbientBackground>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 130, gap: 18 },
  kicker: { fontSize: 10, fontWeight: "800", letterSpacing: 1.4 },
  title: { marginTop: 5, fontSize: 36, lineHeight: 42, letterSpacing: -1.2, fontWeight: "800" },
  subtitle: { marginTop: 5, fontSize: 14, lineHeight: 20 },
  preview: { minHeight: 190, padding: 20, overflow: "hidden", justifyContent: "flex-end" },
  previewLight: { position: "absolute", width: 220, height: 220, borderRadius: 999, right: -52, top: -88 },
  previewTiny: { fontSize: 10, letterSpacing: 1.2, fontWeight: "800", marginBottom: 8 },
  previewTitle: { fontSize: 28, lineHeight: 32, fontWeight: "800", letterSpacing: -0.8 },
  previewDetail: { fontSize: 13, marginTop: 4, maxWidth: "82%", lineHeight: 19 },
  previewDots: { flexDirection: "row", gap: 6, marginTop: 13 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  themeGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  themeCard: { width: "48.5%", minHeight: 180, borderRadius: 22, borderWidth: 1, padding: 13, overflow: "hidden" },
  swatch: { height: 46, borderRadius: 15, overflow: "hidden", marginBottom: 12 },
  swatchGlow: { width: 70, height: 70, borderRadius: 40, position: "absolute", right: -12, top: -16, opacity: 0.85 },
  themeName: { fontSize: 14, fontWeight: "800" },
  themeDescription: { fontSize: 11, lineHeight: 16, marginTop: 4 },
  selectedPill: { position: "absolute", bottom: 11, left: 12, flexDirection: "row", alignItems: "center", gap: 3, borderRadius: 10, paddingHorizontal: 7, paddingVertical: 4 },
  selectedText: { fontSize: 8, fontWeight: "900", letterSpacing: 0.8 },
  controls: { gap: 9 },
  stepper: { minHeight: 84, padding: 15, flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 8 },
  stepperCopy: { flex: 1, minWidth: 0 },
  stepperLabel: { fontSize: 15, fontWeight: "700" },
  stepperDetail: { fontSize: 11, lineHeight: 15, marginTop: 3, maxWidth: 210 },
  stepperControls: { flexDirection: "row", alignItems: "center", gap: 5 },
  stepperButton: { width: 34, height: 34, borderRadius: 12, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  stepperValue: { minWidth: 37, textAlign: "center", fontSize: 13, fontWeight: "800" },
  motionControl: { minHeight: 82, padding: 15, flexDirection: "row", alignItems: "center", gap: 12 },
  pressed: { opacity: 0.76, transform: [{ scale: 0.975 }] },
});
