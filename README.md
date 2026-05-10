# Replicated Integration

<!-- Replicated Integration のタイトル画像またはスクリーンショット -->

**Replicated Integration** は、Replication の物質計算をほかの工業・魔術・ストレージ系 mod のレシピへ広げるための互換
addon です。

Replication はアイテムを「matter」として扱う強力な mod ですが、大型 modpack では追加 mod
の素材・中間素材・流体・化学物質が計算対象から外れてしまうことがあります。Replicated Integration はそれらのレシピを解析し、Replication
の matter graph に追加して、より自然に複製・変換できる世界を作ります。

## Features

<!-- 対応modが並んだゲーム内スクリーンショット -->

- **追加 mod のレシピを matter 計算へ統合**
  対応 mod の機械レシピや加工レシピを読み取り、入力素材から出力アイテム・流体・化学物質の matter value を推定します。

- **アイテム・タグ・流体・化学物質を扱える command**
  `/repint matter` コマンドで、計算済み matter の確認、手動上書き、deny、reset、config への保存ができます。

- **datapack / config / runtime override に対応**
  datapack化 された補助 matter value、config に保存した明示値、実行中だけの runtime override を組み合わせて使えます。

- **外部 addon 登録に対応**
- このmodの機能に不足を感じた場合、簡単に別modからaddonを登録して機能拡張できます。

- **デバッグ用 trace command**
  `/repint matter node ... trace` で、ある node の matter value がどの変換から導かれているかを追跡できます。

## Supported Loaders

| Minecraft | Loader   | Status    |
|-----------|----------|-----------|
| 1.20.1    | Forge    | Supported |
| 1.21.1    | NeoForge | Supported |

## Supported Mods

<!-- 対応mod一覧のバナー画像 -->

Replicated Integration は、Replication 本体に以下の mod との連携を追加します。

| Mod                   | 1.20.1 Forge | 1.21.1 NeoForge | What is integrated                                                     |
|-----------------------|--------------|-----------------|------------------------------------------------------------------------|
| Minecraft / Forge     | Yes          | Yes             | Stone cutting, Blasting, Campfires and more,   Forge Fluids            |
| Mekanism              | Yes          | Yes             | Machine recipes, chemical nodes, chemical tags,optional Nuclear things |
| Applied Energistics 2 | Yes          | Yes             | Inscriber and charger recipes, AE2 matter value supplements            |
| Advanced AE           | Yes          | Yes             | Reaction chamber recipes                                               |
| Draconic Evolution    | Yes          | Yes             | Fusion crafting recipes, Draconic matter value supplements             |

### Notes
- 全てのmodを必要とするわけではありません。必要なmodのaddonだけを選んで導入できます。
- 対応modは今後も増える予定です。追加してほしいmodがあれば issue でリクエストしてください。
- このmodはレシピのエネルギーコストなどを無視します。おかしいと思ったものはdenyすることができます。
- このmodで複製できるものはアイテムのみです。今のところ液体や化学物質をそのまま複製することはできませんが、経由することは可能です。

## Command Examples

<!-- コマンド実行例のスクリーンショット -->

全てのコマンドは`/repint`で始まります。サブコマンドは以下の通りです。

### Read matter values

Use `get` to inspect calculated or explicitly configured matter.

```mcfunction
/repint matter get item minecraft:stone
/repint matter get item_tag minecraft:logs
/repint matter get fluid minecraft:water
/repint matter get fluid_tag minecraft:water
/repint matter get chemical mekanism:oxygen
/repint matter get chemical_tag c:gases/oxygen
```

You can optionally filter the output to one matter type:

```mcfunction
/repint matter get item minecraft:diamond precious
/repint matter get item minecraft:ender_pearl ender
```

Available matter filters:

```text
earth, nether, organic, ender, metallic, precious, living, quantum
```

### Set matter values

`set` requires permission level 2. It writes a runtime override first.

Set a single matter type:

```mcfunction
/repint matter set item minecraft:diamond precious 64
/repint matter set item_tag minecraft:logs organic 8
/repint matter set fluid minecraft:lava nether 0.001
/repint matter set chemical mekanism:oxygen earth 1
```

マターの値を全て同時にセットすることも一つずつ設定することもできます。不要なマターは0にすることで含まないようにできます。

```mcfunction
/repint matter set item minecraft:diamond precious 0
```

全てを一度にセットする場合:

```mcfunction
/repint matter set item minecraft:nether_star all earth 0 nether 0 organic 0 ender 128 metallic 0 precious 256 living 0 quantum 64
```

### Deny and reset

`deny`することで、そのアイテム・タグ・化学物質とそれを前提とするものからマターを削除することができます。denyは`reset`や値の設定で上書きできます。

```mcfunction
/repint matter deny item minecraft:bedrock
/repint matter deny item_tag minecraft:logs
/repint matter deny chemical mekanism:spent_nuclear_waste
```

`reset`はコマンドでの上書きを無かったことにします。レシピから導かれる場合やデータパックで指定されている場合はその値に戻ります。

```mcfunction
/repint matter reset item minecraft:bedrock
/repint matter reset item_tag minecraft:logs
/repint matter reset chemical mekanism:spent_nuclear_waste
```

### Commit runtime changes

コマンドでの設定はそのワールドにのみ有効な runtime override として保存されます。`commit`することで、現在の runtime override を config に保存してminecraft全体に適用できます。
config に保存された値は、今後のワールドでもデフォルトで上書きされるようになります。

```mcfunction
/repint commit
```

### Debug commands

Use debug commands when you want to inspect arbitrary node types or trace how a value was calculated.


```mcfunction
/repint matter get type c:item minecraft:stone
/repint matter get tag c:item minecraft:logs
/repint matter node c:item minecraft:diamond
/repint matter node c:item minecraft:diamond trace
/repint matter node c:item minecraft:diamond trace 3
```

Debug trace depth is limited to `1..4`.

## Command Targets

The available command targets depend on installed addons.

| Target         | Meaning                          | Example           |
|----------------|----------------------------------|-------------------|
| `item`         | A single item node               | `minecraft:stone` |
| `item_tag`     | An item tag selector             | `minecraft:logs`  |
| `fluid`        | A single fluid node              | `minecraft:water` |
| `fluid_tag`    | A fluid tag selector             | `minecraft:water` |
| `chemical`     | A Mekanism chemical node         | `mekanism:oxygen` |
| `chemical_tag` | A Mekanism chemical tag selector | `c:gases/oxygen`  |

## How It Works

<!-- matter graph の概念図 -->

Replicated Integration builds an addon-driven conversion graph:

1. Built-in and external addons are collected through the loader event bus.
2. Addons register matter node command targets such as item, fluid, or chemical.
3. Addons collect default values, selectors, and recipe conversions.
4. Runtime/config/datapack values are materialized into explicit matter nodes.
5. The solver propagates matter values through the conversion graph.
6. Replication receives the resulting item matter compound map.

This means values can come from several places:

| Source                    | Description                                         |
|---------------------------|-----------------------------------------------------|
| Recipe-derived            | Calculated from known inputs and recipe conversions |
| Datapack / generated data | Supplemental matter values shipped with the mod     |
| Runtime override          | Temporary value set with commands                   |
| Config override           | Persistent value committed from runtime overrides   |
| Deny                      | Explicitly blocked matter value                     |

## Datapack Data

Supplemental matter values are generated into `src/generated/resources` by datagen.

For development:

```bash
./gradlew :1.21.1-neo:runData --no-daemon
```

Do not hand-write generated matter JSON. Add or change values in the datagen provider and regenerate.

## For Modpack Authors

<!-- modpack向け設定例のスクリーンショット -->

- Use `/repint matter get ...` to inspect surprising values before changing them.
- Use `/repint matter set ...` for temporary tuning while testing.
- Use `/repint commit` once values feel right.
- Use `deny` for items, tags, or chemicals that should never become replicable.
- Prefer tag-level overrides for families of equivalent items.

## For Addon Developers

External mods can register their own replication addon through the loader-specific registration event:

- Forge: `RegisterForgeReplicationAddonsEvent`
- NeoForge: `RegisterNeoReplicationAddonsEvent`

Addons participate in the same lifecycle as built-in integrations:

- `isEnabled`
- `registerMatterNodes`
- `collectDefaults`
- `collectSelectors`
- `collectConversions`

Addon collection order is not meaningful. The final addon list is sorted by addon id before loading, and duplicate ids
are rejected.

## License

MIT
