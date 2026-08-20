import MaterialIcons from "@expo/vector-icons/MaterialIcons";
import { Tabs } from "expo-router";
import { Platform, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { CompactPlayer } from "@/components/luma-ui";
import { HapticTab } from "@/components/haptic-tab";
import { useLuma } from "@/lib/luma/app-provider";

const TAB_ITEMS = [
  { name: "index", title: "Listen", icon: "headphones" },
  { name: "library", title: "Library", icon: "favorite-border" },
  { name: "play", title: "Play", icon: "rocket-launch" },
  { name: "themes", title: "Themes", icon: "palette" },
  { name: "settings", title: "Settings", icon: "tune" },
] as const;

export default function TabLayout() {
  const { theme } = useLuma();
  const insets = useSafeAreaInsets();
  const bottomPadding = Platform.OS === "web" ? 10 : Math.max(insets.bottom, 8);
  const tabBarHeight = 60 + bottomPadding;
  return (
    <View style={styles.root}>
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarButton: HapticTab,
          tabBarActiveTintColor: theme.accentSoft,
          tabBarInactiveTintColor: theme.muted,
          sceneStyle: { backgroundColor: "transparent" },
          tabBarStyle: {
            height: tabBarHeight,
            paddingTop: 7,
            paddingBottom: bottomPadding,
            borderTopWidth: 1,
            borderTopColor: theme.border,
            backgroundColor: theme.surfaceStrong,
          },
          tabBarLabelStyle: { fontSize: 10, fontWeight: "700" },
        }}
      >
        {TAB_ITEMS.map((item) => (
          <Tabs.Screen
            key={item.name}
            name={item.name}
            options={{
              title: item.title,
              tabBarIcon: ({ color, size }) => <MaterialIcons name={item.icon} size={size} color={color} />,
            }}
          />
        ))}
      </Tabs>
      <View pointerEvents="box-none" style={[styles.compactPlayerSlot, { bottom: tabBarHeight + 4 }]}><CompactPlayer /></View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  compactPlayerSlot: { position: "absolute", left: 0, right: 0 },
});
