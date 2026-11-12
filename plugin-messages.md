# Plugin Messages

Loot Filters uses RuneLite `PluginMessage` events in the `loot-filters` namespace.

## resolved-display-request-v1

Post on the client thread with a mutable data map. Loot Filters fills `status` and, on success, `display` before `post` returns.

```java
Map<String, Object> data = new HashMap<>();
data.put("x", worldX);
data.put("y", worldY);
data.put("z", plane);
data.put("itemId", itemId);
eventBus.post(new PluginMessage("loot-filters", "resolved-display-request-v1", data));
```

All inputs are `Integer`. Optional `worldViewId` defaults to `WorldView.TOPLEVEL` (-1); other IDs must be nonnegative. Coordinates are the selected world view's base X/Y plus scene-tile X/Y; `z` is the plane (0–3). `itemId` must be nonnegative.

| Status | Meaning |
| --- | --- |
| `ok` | `display` contains the evaluated settings. |
| `not-found` | The world view, tile or item is unavailable. |
| `not-ready` | The item has not been evaluated yet. |
| `ambiguous` | Same-ID stacks on the tile have different settings. |
| `invalid-request` | `error` describes an invalid input or a call from outside the client thread. |
| Absent | No compatible provider responded. |

Use a fresh map for each request. `display` and its nested collections are read-only snapshots of `DisplayConfig` accessor values.

| Display fields | Values |
| --- | --- |
| textColor, menuTextColor | Color |
| backgroundColor, borderColor, textAccentColor, lootbeamColor, tileStrokeColor, tileFillColor | Nullable Color |
| hidden, hideOverlay, showLootbeam, showValue, compact, showDespawn, notify, highlightTile | Boolean |
| textAccent | Nullable enum-name String |
| fontType | Enum-name String; defaults to NORMAL |
| menuSort | Integer; defaults to 0 |
| evalTrace | List of matched line/rule integers |
| icon, sound | Nullable descriptors |

Icon descriptors contain `type` and these fields:

| Type | Fields |
| --- | --- |
| sprite | id, index |
| item | id, height |
| current-item | id, quantity, height |
| file | filename |

Each icon includes `getImage`, a `Supplier<AsyncBufferedImage>` to call on the client thread. Register `onLoaded` before using its pixels. Item images use their native dimensions; `height` is the filter's display height. File and sprite images retain their original dimensions. File images come from Loot Filters' evaluated icon cache. An unavailable image returns null; callers may retry for sprites whose archive has not loaded yet. Treat images as shared and copy before modifying them.

Callers handle resizing, caching, timeouts and stale results. Refresh snapshots when ground items or filters change, and discard cached images on `filter-reloaded` or plugin shutdown.

Sound descriptors contain `type: sound-effect` with `id`, or `type: file` with `filename`. Unrecognized icon or sound providers have `type: unknown`.

## filter-reloaded

Loot Filters posts this event on the client thread after resetting evaluated display settings, including reloads of the same filter filename:

```java
new PluginMessage("loot-filters", "filter-reloaded", Map.of())
```
