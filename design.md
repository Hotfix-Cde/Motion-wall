# Luma Mobile Interface Design

## Product intent

Luma is a calm, tactile Android music companion for short mood resets. It combines lawful song previews, ambient playback, responsive neon-light environments, and small, non-competitive games. The interface avoids dashboard clutter: music, personal library, visual themes, and games each have a dedicated destination. The visual language uses restrained Apple-inspired hierarchy, large readable labels, generous spacing, rounded continuous surfaces, and soft haptic feedback rather than copying any third-party interface.

## Portrait layout and navigation

The experience is designed for a 9:16 portrait screen and one-handed reach. A five-item bottom navigation bar provides direct access to **Listen**, **Library**, **Play**, **Themes**, and **Settings**. The persistent compact player sits above the tab bar whenever a track is active; one tap opens the full Now Playing sheet. Primary actions occupy the lower half of the screen where practical, with all tap targets at least 44 points tall.

| Screen | Primary content and functionality | Layout specification |
|---|---|---|
| Listen | Greeting, mood shortcuts, currently featured playable previews, recent searches, and a route to search. | A vertically scrolling feed with a quiet wordmark, a 2-column mood grid, and a single featured album card. |
| Search | Song or artist query, results from the selected free source, play buttons, and save controls. | A focus-first search field at the top; results render as compact rows with artwork and a trailing play action. |
| Now Playing | Artwork, title, artist, timeline, transport controls, favorite button, queue controls, sleep timer, and visualizer. | A modal-like full screen with large hero artwork and controls arranged in a thumb-friendly lower control zone. |
| Library | Saved tracks, recent plays, and locally remembered playlists. | Segmented controls and an empty state that guides users toward Listen without blocking navigation. |
| Play | Three mini-game cards: Orbit Cat, Star Catcher, and Moon Garden. | A quiet game selector followed by an immersive full-screen game view; music continues uninterrupted. |
| Orbit Cat | A cat companion guides a drifting spacecraft through glowing orbital rings. | One large tap zone to give the cat a gentle boost; score and pause are deliberately subtle. |
| Star Catcher | The user taps drifting stars while a sleeping cat observes from a crescent moon. | Single-finger taps; a short, soothing 45-second session with no failure screen. |
| Moon Garden | The user taps the sky to plant soft nebula flowers for a cat resting on a planet. | Open-ended interaction that rewards creating patterns rather than speed. |
| Themes | Theme presets and controls for accent colour, glow, glass depth, and reduced motion. | A live preview card sits above preset choices and sliders, with an Apply button in the reachable lower area. |
| Settings | Playback preferences, accessibility options, data/source disclosure, and reset controls. | Grouped settings with short explanatory labels and native-feeling rows. |

## Key user flows

The core listening flow begins at **Listen**, where a user chooses a mood or opens Search, taps a result, and immediately hears a lawful preview in the persistent player. Selecting the player opens **Now Playing**, where the user can pause, skip among the local queue, favorite a preview, select a sleep timer, or return to browsing while playback continues.

The personalization flow starts at **Themes**. A user chooses a preset, adjusts the liquid-glass and glow controls, and applies the setting. The color system updates all screen surfaces, the mini-player, game scenes, and visualizer in real time. A reduced-motion switch keeps all essential feedback while suppressing drifting elements and pulsing effects.

The relaxation flow starts at **Play**. A user opens one of the three games while the compact player remains available. The games contain no ads, countdown pressure, or losing state. Returning to any other section preserves both theme selection and active playback.

## Visual system

The default theme, **Velvet Aurora**, pairs a deep ink-blue background with luminous lilac and mint glow. All themes employ dark canvas surfaces to preserve the neon quality, a translucent “liquid glass” layer for floating content, and clear high-contrast text. Glass is simulated with transparent layers and borders where native blur is unavailable, while Android-capable devices receive the closest available blur treatment.

| Theme | Canvas | Accent | Glow | Intended mood |
|---|---|---|---|---|
| Velvet Aurora | `#090B21` | `#B7A7FF` | `#65F0C8` | Dreamy midnight and stars |
| Candy Cloud | `#211021` | `#FF98CB` | `#B7A7FF` | Sweet, soft, and playful |
| Tidal Glass | `#061E29` | `#65D8FF` | `#71F0D0` | Cool focus and flowing water |
| Solar Peach | `#2A101A` | `#FFAA7A` | `#FFD36E` | Warm, gentle evening light |

## Accessibility and interaction standards

Luma uses semantic labels for icons, visible text for core actions, 44-point minimum controls, and a readable contrast-first text system. Motion can be reduced without reducing app functionality. Haptics are limited to meaningful actions such as starting playback, applying a theme, and completing a game moment. No interaction relies solely on color, gesture, or audio.
