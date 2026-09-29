# Modern Energistics 1.0.0

Minecraft **1.21.1**, NeoForge **21.1.251**, Java **21**.

## Install
Replace the previous Modern Energistics or MI AE Bridge jar with `modern-energistics-1.21.1-1.0.0.jar` on client and server. Keep only one version installed.

Tested required dependencies: AE2 19.2.17, Modern Industrialization 2.5.8, GuideME 21.1.14. MI bundles GrandPower 3.0.0.

Tested optional dependencies:
- AdvancedAE 1.6.12-1.21.1 and GeckoLib 4.8.2.
- ExtendedAE 1.21-2.2.38-neoforge and Glodium 1.21-2.2-neoforge.
- AppliedFlux 1.21-2.1.6-neoforge.
- Extended Industrialization 1.16.2-1.21.1 and Tesseract API 1.12.16-1.21.1.

The display name is Modern Energistics. The mod ID and resource namespace are modernenergistics. Legacy miaebridge registry aliases resolve older blocks, items and block entities to the new IDs; newly saved entries use modernenergistics. The release version stays 1.0.0 unless explicitly changed.

## Providers
| Name | Pattern slots | Behavior |
| --- | ---: | --- |
| MI Pattern Provider | 9 | AE2 processing + MI integration |
| Advanced MI Pattern Provider | 9 | AdvancedAE processing + MI integration |
| Extended MI Pattern Provider | 36 | Normal processing with expanded capacity |
| Advanced Extended MI Pattern Provider | 36 | AdvancedAE processing with expanded capacity; requires both addons |

The Advanced MI Pattern Provider recipe uses AdvancedAE's nine-slot Advanced Pattern Provider ("advanced_ae:small_adv_pattern_provider") plus an MI Pattern Provider. It does not consume the larger Advanced Extended Pattern Provider.

All four now target **single-block MI crafting machines AND combined ME I/O hatches**. This fixes the previous Combined MI Provider failure beside a formed multiblock. The combined provider retains its old registry ID so existing blocks survive the display-name change.

Advanced providers accept AdvancedAE processing patterns and check explicit ingredient faces against the target's item/fluid input capabilities. MI uses shared input inventories, so insertion remains one atomic recipe batch. Explicit hatch-face patterns must fit the addressed hatch; patterns without explicit faces can use the combined multiblock capacity.

Upgrade-card registrations mirror the corresponding installed native providers, including their install limits. Native inherited logic supplies behavior and menus. AppliedFlux Induction Cards were tested for installation, persistence, sustained energy transfer and removal on all four variants. Third-party cards with custom hooks outside the native provider classes may need their own adapter; arbitrary untested addon combinations are not guaranteed.

## Multiblock setup
1. Place ME Input/Output Hatches in normal item, fluid or energy-input hatch positions of a supported crafting multiblock.
2. Form the machine and connect its EU/steam supply.
3. Place a powered, network-connected pattern provider directly against a combined hatch. Put patterns in the provider, never in the hatch.
4. Encode exact item/fluid inputs and guaranteed outputs as a processing pattern, then request the result from an ME crafting terminal.

Hatches on the same formed controller share one recipe job. Each contributes 18 item inputs, 18 item outputs, eight input tanks and eight output tanks (1,024,000 mB each). Inputs are inserted atomically; the entire submitted batch must fit. Only the selected MI recipe may start. Output goes back to the provider that submitted the batch, including when another linked hatch handles the return.

Each hatch also provides **614,400 EU storage**, matching MI's HV input-hatch buffer. It accepts LV, MV and HV cables; higher cable tiers are not supported by this release. Cable/network transfer limits and the controller's recipe/coil rules still apply. Energy is saved and preserved in ordinary block drops. AppliedFlux's native MI energy adapter handles induction power; the mod does not invent an FE/EU conversion ratio.

## Hatch screen and locks
The hatch screen separates item inputs, item outputs, fluid inputs and fluid outputs. It keeps the energy icon and controller progress arrow, without the right information panel, info button or input-rate text. Opening a hatch no longer posts job status into chat. Use a GUI scale that leaves at least 310 logical pixels of vertical space for the full inventory.

The left selection panel changes **Automatic recipe release / Keep recipe locked**. Shift-right-click with an empty hand also toggles retention. Automatic release waits for the batch to finish and for the machine's output inventory to drain. Changing the retention setting never permits a competing recipe during an active batch. MI provider shortcuts control adjacent single machines; use the hatch control for linked multiblocks.

Inputs may disappear from local slots as MI consumes them, or be distributed to another linked hatch. The progress arrow follows the controller. The hatch is not an ME cable node and does not itself store patterns.

## Capacity-based dispatch
The four MI providers send the largest complete recipe count that fits the target's actual input inventory, limited by the requested work, reserved ingredients and available AE power. The former 64-craft limit is removed. Transactional insertion probes account for shared item slots, tank capacity, stack limits, contents, slot locks and AdvancedAE input-face restrictions before committing anything. Linked hatches contribute their actual aggregated inventories.

Each current hatch has **18 item-input slots**, separate from its 18 output slots. For a recipe consuming one ordinary 64-stackable item per craft, one empty hatch holds 1152 crafts; two linked hatches hold 2304. Nine usable input slots hold 576. A request for only 1000 crafts sends at most 1000, even when more space is available. Recipes requiring several ingredients divide the same space. Smaller-stack items and fluids follow their native slot/tank limits. An existing recipe validation ceiling of 1,000,000 executions still bounds a single dispatch.

CPU ingredient reservations, expected outputs and result locks scale together. Standard AE2 and AdvancedAE CPU dispatch are supported. A machine with one 64-item input slot naturally still sends at most 64 one-item crafts at once.
Reusable ingredients are held until the entire batch completes, then returned to the submitting provider. Their return ledger survives saves. Automatic unlocking also waits for these returns, so a full return buffer cannot discard the catalyst or allow a competing recipe. AE2 may schedule smaller batches when only one reusable tool is available; this does not manufacture additional tools for a larger batch.

## Efficiency between batches
A managed machine retains its earned MI efficiency while idle between AE jobs, including while outputs await collection. Efficiency continues building when the next batch completes, up to the current recipe maximum. Switching to a lower-limit recipe caps the retained value immediately; existing over-limit saved values are corrected on load. This does not create ingredients or run a recipe without an authorized batch. Active-job power shortages, native cooldown when a multiblock is unformed, and single-machine provider removal retain their normal behavior. The same retention applies to supported EI multiplied crafters.

## Extended Industrialization
Standard MI-derived crafting machines are discovered without restricting the registry namespace. Tesseract multiplied crafting multiblocks have a dedicated optional adapter that restricts recipe selection, counts actual parallel crafts and persists the shared job. Verified end-to-end with EI's large electric furnace processing a four-item batch. Processing arrays require their normal machine configuration before a recipe type is available; all native structure and machine rules remain in effect.

This integration covers recipe-driven crafting, not generators, reactors, storage tanks, farmers or beacons with non-recipe behavior. Every EI special machine has not been tested individually.

## Existing worlds
The old Advanced MI Provider had 36 active slots. It now exposes nine. Patterns in old slots 10–36 are retained in a saved overflow inventory and returned when the provider is broken normally. Remove excess patterns before upgrading if convenient. The combined provider remains at 36 slots. Back up worlds before installing any alpha mod update.

## Constraints
- Non-consumed catalysts may be supplied by the pattern or preloaded. Supplied catalysts are returned after the batch finishes; preloaded catalysts stay in the machine. For the Pressurizer, encode **1 air intake -> 1000 mB liquid air + 1 air intake**, with liquid air first. Include reusable ingredients among secondary outputs so AE2 accounts for their return. Supply steam as fuel normally.
- Guaranteed outputs identify the recipe. Chance byproducts are collected, but should not be promised as guaranteed pattern outputs. Probabilistically consumed ingredients are rejected.
- Keep the originating provider in place until outputs return. There is no force-cancel/refund of ingredients already consumed by MI.
- Visual GUI inspection was skipped at the user's request. Client startup/resource loading passed; see VALIDATION.md for measured tests.

## Build
Run `./bootstrap.ps1`, then `./gradlew.bat build` from this directory.

`./gradlew.bat runGameTestServer` tests the base setup. Add `-Padvanced`, `-Pextended`, or `-Paddons` for provider addons, `-Pflux` for AppliedFlux, and `-Pei` for Extended Industrialization. Example: `./gradlew.bat -Paddons -Pflux -Pei runGameTestServer`.

Downloads are pinned by URL and SHA-256 in dependencies.json. GrandPower's compile API comes from MI's embedded jar. Runtime addon jars and all test classes/fixtures are excluded from the production jar.

Providers use the original nine-square face design: gold/orange accents for Normal and slightly lighter gold for Extended, purple for Advanced and a slightly lighter lavender for Advanced Extended. Both retain the same nine-square design. The extra corner posts have been removed. Run tools/generate-resources.ps1 to regenerate the original pixel artwork and models. This mod's original code and artwork are MIT licensed.






