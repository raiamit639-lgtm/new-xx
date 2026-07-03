# Nexus

A client-side rendering optimization mod for Minecraft (Fabric, 1.21.x), with a configurable, animated video settings panel.

This is an original mod: all code here is newly written for this project. It is **not** a fork, rename, or repackaging of Sodium, Iris, or any other existing mod — it implements the general *category* of techniques those mods are known for (frustum/occlusion/entity culling, async chunk mesh building, configurable render distance) as new code.

## Features

- **Culling & Distance** — render distance slider, frustum culling, occlusion culling, entity culling, fog culling
- **Chunk Building** — async chunk rebuild toggle, smooth chunk fade-in, configurable builder thread count
- **Visual Quality** — graphics quality cycle (Fast/Fancy/Fabulous), smooth lighting, animated textures, particles + density slider
- **GUI Behavior** — toggle for the settings panel's own animations, adjustable animation speed
- Press **N** in-game to open the settings panel (rebindable in Controls)
- Settings persist to `config/nexus.json`

## Building

Requires JDK 21.

```bash
./gradlew build
```

The compiled mod jar will be in `build/libs/`.

## Project status

This scaffold includes:
- Full Gradle/Loom build setup
- `fabric.mod.json` and mixin config
- Config system (`NexusConfig`) with JSON persistence
- Animated GUI widget kit (`AnimatedToggleWidget`, `AnimatedSliderWidget`) — smooth eased transitions, respects the animation-speed and animations-on/off settings
- Full video settings screen (`NexusVideoSettingsScreen`) wiring every config option to a widget, with a panel fade/slide-in on open

**Not yet implemented** (config options exist and are wired to the GUI, but don't yet hook into actual rendering — see `## Next steps` below): the mixins that make frustum/occlusion/entity culling, async chunk building, etc. actually affect rendering. Right now toggling them updates and persists config values; making them change what's drawn requires mixing into Minecraft's renderer, which needs your loom-decompiled MC source mapped locally to write against real class/method names.

## Icon

`src/main/resources/assets/nexus/icon-source.svg` is the source logo — a faceted "N" mark (angular render-mesh segments, cyan-to-violet gradient, on a dark charcoal background), sized 512x512.

`fabric.mod.json` references `assets/nexus/icon.png`, which does **not** exist yet — this build environment has no `rsvg-convert`/Inkscape and no network access to install one, so the SVG could not be rasterized here. Before building, convert it yourself with any of:

```bash
# Option 1: rsvg-convert (if installed)
rsvg-convert -w 128 -h 128 icon-source.svg -o assets/nexus/icon.png

# Option 2: Inkscape
inkscape icon-source.svg -w 128 -h 128 -o assets/nexus/icon.png

# Option 3: any browser — open the SVG, screenshot/export at 128x128
```

## Next steps

1. Convert the icon (see above)
2. Set up a Loom-mapped workspace (`./gradlew genSources`) so you have real class names to mixin into
3. Wire `NexusConfig.frustumCulling` etc. into actual mixins on the chunk/entity renderer
4. Replace the placeholder author/homepage fields in `fabric.mod.json`
