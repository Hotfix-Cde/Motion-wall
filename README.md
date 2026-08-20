# Luma

**Luma** is an Android-first, cozy music companion. It pairs open-catalog music streaming with a personal local library, an Apple-inspired liquid-glass visual system, and three gentle cat-and-space mini-games that continue alongside playback.

| Area | What it includes |
|---|---|
| **Listen** | Mood shortcuts, live trending discovery, and catalog search. |
| **Player** | Persistent playback, transport controls, queue navigation, volume control, favorites, sleep timer, and a compact player across primary sections. |
| **Library** | On-device favorites and recently played tracks, without an account requirement. |
| **Themes** | Velvet Aurora, Candy Cloud, Tidal Glass, and Solar Peach; all support independent glow, glass-depth, and reduced-motion controls. |
| **Play** | Orbit Cat, Star Catcher, and Moon Garden: short, calming games designed not to interrupt audio. |

## Music source and content rules

Luma uses Audius read-only endpoints for discovery and streaming. Audius documents the API as a REST interface for querying and streaming tracks and notes that most read-only endpoints work without credentials.[1] The app only requests catalog-visible tracks, streams them from their returned track endpoint, and does not download, alter, or cache audio. When that primary catalog is temporarily unavailable, Luma falls back to public ambient radio stations supplied by Radio Browser, which preserves a live, calming listening path.[3] The product’s source disclosure links users to Audius in Settings.

The app deliberately does **not** use iTunes Search previews as a music source. Apple restricts those promotional assets to promotion of associated store content, with attribution and badge requirements that do not fit an independent ambient player.[2]

## Local development

Install dependencies and run the Expo project:

```bash
pnpm install
pnpm dev
```

For Android testing, run `pnpm android` and open the project through Expo Go or an Android emulator. The app is intentionally portrait-only. Native background playback is configured through `expo-audio`; final standalone behavior must be checked in an Android build rather than only in a browser preview.

## Quality checks

```bash
pnpm check
pnpm test
```

The deterministic test suite validates the API-to-player mapping, failure behavior, search query formation, and theme model. Live Audius requests are deliberately mocked in unit tests.

## Project structure

| Path | Purpose |
|---|---|
| `app/(tabs)` | Dedicated Listen, Library, Play, Themes, and Settings sections. |
| `app/player.tsx` | Full-screen Now Playing sheet. |
| `app/game/[id].tsx` | Orbit Cat, Star Catcher, and Moon Garden experiences. |
| `lib/luma/app-provider.tsx` | Persistent local settings, player state, favorites, history, and sleep timer. |
| `lib/luma/catalog.ts` | Audius catalog normalization and read-only requests. |
| `components/luma-ui.tsx` | Shared neon background, liquid-glass surfaces, player, and track rows. |

## References

[1]: https://docs.audius.org/api/ "Audius API Reference"
[2]: https://performance-partners.apple.com/search-api "Apple iTunes Search API"
[3]: https://www.radio-browser.info/ "Radio Browser public radio directory"
