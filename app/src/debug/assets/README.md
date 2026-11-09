# Debug assets

Media for the in-process fake HTSP server. This folder is part of the **debug** source set, so
nothing here is ever packaged into a release build.

## demo_recording.mp4

Served as the content of every demo recording, so the internal player streams real video instead of
a generated silent WAV. It is what makes "Completed recordings → PLAY" show an actual picture and
expose a track to the track selection dialog.

- **Big Buck Bunny** (10 s, 360p, H.264, ~0.9 MB), © Blender Foundation, licensed under
  [Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/).
  Source: <https://test-videos.co.uk/vids/bigbuckbunny/mkv/360/Big_Buck_Bunny_360_10s_1MB.mkv>
  (that URL serves an MP4 with a faststart `moov` atom, so it is stored here as `.mp4`).
- **Video only** — the clip carries no audio track, so the track selection dialog shows a single
  video group. Swap in a clip with audio if you want two tabs.

The file is served byte for byte, so the container decides what plays. `TvheadendExtractorsFactory`
sniffs Matroska, MP4, fragmented MP4, MPEG-TS, FLV, Ogg and WAV; any extension works, because
`DemoRecordingAsset` picks the file up by the `demo_recording` name prefix.

Keep a replacement small (a couple of seconds, a few hundred kilobytes). The player seeks within it,
so it also exercises the ranged reads of the HTSP file API.

Without this file the fake server falls back to a generated one-second silent WAV, which still
reaches a ready state but only ever has one audio track.
