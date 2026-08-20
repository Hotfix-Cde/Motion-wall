# Luma Research Notes

## Design direction

The supplied Apple-design topic surfaces a liquid-glass reference that treats glass as **navigation and controls**, rather than rendering every card as translucent. That distinction is appropriate for Luma: the bottom bar, compact player, transport controls, sheets, and theme controls receive glass treatment, while dense search and library rows remain more opaque for legibility. The reference also emphasizes adaptive quality, reduced motion, reduced transparency, clear focus states, and localized use of glass. Luma will apply those principles as original React Native components rather than copying its Flutter implementation or assets.[1]

## Music integration decision

Apple’s iTunes Search API was ruled out as Luma’s playback source. Although it supports search and returns 30-second previews, its stated terms restrict previews to promotion of the associated store content, require an approved store badge close to the preview, require attribution, and prohibit independent entertainment use. This does not suit a standalone mood player with games.[2]

Luma will use **Audius read-only API access** for catalog search and streamed tracks. Audius documents its REST API as suitable for querying and streaming tracks in music players and notes that most read-only endpoints work without credentials. Its 2025 terms update states that the Open Music License and API terms govern third-party use in apps and games, with creator-selected access restrictions carrying through the API. Luma will only render tracks returned as API-accessible and will not cache, download, or modify audio.[3] [4]

## Implementation safeguards

The player will use Expo Audio’s remote URL playback capability and will explicitly manage player lifecycle. The app’s glass effects will use a limited number of clipped BlurView surfaces; Android blur will be optional because the native mode is experimental. Gradients will form the durable fallback behind all glass treatment. Feedback will use Android-safe haptics only for significant actions, always paired with visible feedback.[5] [6] [7]

## References

[1]: https://github.com/sdegenaar/liquid_glass_widgets "liquid_glass_widgets README"
[2]: https://performance-partners.apple.com/search-api "iTunes Search API legal and usage terms"
[3]: https://docs.audius.org/api/ "Audius API Reference"
[4]: https://blog.audius.co/posts/audius-terms-of-service-update "Audius terms and Open Music License update"
[5]: https://docs.expo.dev/versions/latest/sdk/audio/ "Expo Audio documentation"
[6]: https://docs.expo.dev/versions/latest/sdk/blur-view/ "Expo BlurView documentation"
[7]: https://docs.expo.dev/versions/latest/sdk/haptics/ "Expo Haptics documentation"
