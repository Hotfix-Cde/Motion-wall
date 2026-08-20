import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import * as Linking from "expo-linking";
import { Pressable, ScrollView, StyleSheet, Switch, Text, View } from "react-native";

import { AmbientBackground, GlassSurface, SectionHeading } from "@/components/luma-ui";
import { useLuma } from "@/lib/luma/app-provider";

function SettingRow({ icon, label, detail, action, children }: { icon: keyof typeof MaterialIcons.glyphMap; label: string; detail: string; action?: () => void; children?: React.ReactNode }) {
  const { theme } = useLuma();
  const content = <><View style={[styles.settingIcon, { backgroundColor: `${theme.accent}20` }]}><MaterialIcons name={icon} size={19} color={theme.accentSoft} /></View><View style={styles.settingCopy}><Text style={[styles.settingLabel, { color: theme.foreground }]}>{label}</Text><Text style={[styles.settingDetail, { color: theme.muted }]}>{detail}</Text></View>{children ?? (action ? <MaterialIcons name="chevron-right" size={21} color={theme.muted} /> : null)}</>;
  if (!action) return <GlassSurface style={styles.settingRow}>{content}</GlassSurface>;
  return <Pressable onPress={action} accessibilityRole="button" accessibilityLabel={label} style={({ pressed }) => [styles.settingPressable, pressed && styles.pressed]}><GlassSurface style={styles.settingRow}>{content}</GlassSurface></Pressable>;
}

export default function SettingsScreen() {
  const { theme, reduceMotion, setReduceMotion, volume, setVolume } = useLuma();
  return (
    <AmbientBackground>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View><Text style={[styles.kicker, { color: theme.glow }]}>SMALL COMFORTS</Text><Text style={[styles.title, { color: theme.foreground }]}>Settings</Text><Text style={[styles.subtitle, { color: theme.muted }]}>Keep Luma soft, simple, and comfortable.</Text></View>
        <SectionHeading title="Listening" />
        <View style={styles.stack}><SettingRow icon="volume-up" label="Playback volume" detail={`${Math.round(volume * 100)}% · adjusts the player right away`}><View style={styles.volumeControls}><Pressable onPress={() => setVolume(volume - 0.1)} accessibilityRole="button" accessibilityLabel="Lower volume" style={({ pressed }) => [styles.smallButton, { borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="remove" size={17} color={theme.foreground} /></Pressable><Pressable onPress={() => setVolume(volume + 0.1)} accessibilityRole="button" accessibilityLabel="Raise volume" style={({ pressed }) => [styles.smallButton, { borderColor: theme.border }, pressed && styles.pressed]}><MaterialIcons name="add" size={17} color={theme.foreground} /></Pressable></View></SettingRow><SettingRow icon="motion-photos-on" label="Reduce motion" detail="Less drifting light and fewer ambient effects."><Switch value={reduceMotion} onValueChange={setReduceMotion} trackColor={{ false: theme.border, true: theme.accent }} thumbColor={theme.foreground} /></SettingRow></View>
        <SectionHeading title="About your music" />
        <View style={styles.stack}><SettingRow icon="public" label="Open music catalog" detail="Discovery and streaming are provided by Audius." action={() => void Linking.openURL("https://audius.co")} /><SettingRow icon="description" label="Music use and credit" detail="Only tracks offered through the open catalog are played; audio is not downloaded or modified." action={() => void Linking.openURL("https://docs.audius.org/api/")} /></View>
        <GlassSurface style={styles.note}><MaterialIcons name="favorite" color={theme.accent} size={18} /><Text style={[styles.noteText, { color: theme.muted }]}>Luma keeps your favorites, visual choices, and recent plays on this device. There is no account wall.</Text></GlassSurface>
      </ScrollView>
    </AmbientBackground>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 130, gap: 18 },
  kicker: { fontSize: 10, fontWeight: "800", letterSpacing: 1.4 },
  title: { marginTop: 5, fontSize: 36, lineHeight: 42, letterSpacing: -1.2, fontWeight: "800" },
  subtitle: { marginTop: 5, fontSize: 14, lineHeight: 20 },
  stack: { gap: 8 },
  settingPressable: { borderRadius: 23 },
  settingRow: { minHeight: 81, padding: 14, flexDirection: "row", alignItems: "center", gap: 11 },
  settingIcon: { width: 39, height: 39, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  settingCopy: { flex: 1, minWidth: 0 },
  settingLabel: { fontSize: 15, fontWeight: "700" },
  settingDetail: { fontSize: 11, lineHeight: 16, marginTop: 3 },
  volumeControls: { flexDirection: "row", gap: 5 },
  smallButton: { width: 33, height: 33, borderRadius: 12, borderWidth: 1, alignItems: "center", justifyContent: "center" },
  note: { padding: 16, flexDirection: "row", gap: 10, alignItems: "flex-start" },
  noteText: { flex: 1, fontSize: 12, lineHeight: 18 },
  pressed: { opacity: 0.76, transform: [{ scale: 0.98 }] },
});
