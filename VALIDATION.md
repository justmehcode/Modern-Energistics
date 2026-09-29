# Modern Energistics 1.0.0 - input-capacity dispatch

- Removed fixed 64-execution dispatch limit. Provider registration includes a weak reference to its block entity so capacity probes use actual enabled target faces.
- MachineBridge simulates the same recipe validation and MI inventory transaction as the real insertion, rolling back before commit and restoring status. Probe does not accept a job, move resources permanently, set return origin or update provider locks.
- Binary search finds the largest fitting count for the selected ingredients, limited by remaining CPU work, reserved supplies, available AE power and existing one-million recipe-execution validation ceiling. Actual extracted alternatives are checked again for slot packing before one real provider push.
- Formed EBF with one current hatch: 18 input slots -> 1152 one-item crafts per transfer, tested as 1152+1152+37. Two linked hatches: 36 inputs -> 2304+2304+37. Nine player-locked slots leave nine usable -> 576+576+37.
- All three use native AE2 CPU jobs and check remaining ingredient reservations, rejection while busy, exact output totals across repeated provider-buffer drains, and final job completion. Earlier one-slot compressor, AdvancedAE CPU, catalyst, EI and AppliedFlux regressions remain passing.
- Full addon suite: all 21 GameTests pass (work/capacity-all-final.log). Base-only suite: all 21 registered tests pass (optional-only bodies skip when absent), work/capacity-base-final.log. Five JUnit tests remain passing. No visual GUI inspection, as requested.

## Previous validation
# Modern Energistics 1.0.0 - smart batching and reusable catalysts

- Full addon suite: 18 GameTests pass (work/catalyst-full-final.log). Base-only suite: 18 registered tests pass, optional-only tests skip their bodies (work/catalyst-base-final.log). Five JUnit tests pass.
- Native AE2 CPU dispatch with all four providers: 1000 single-output crafts dispatch as fifteen 64-craft batches then 40. A two-item recipe with a 64-item input buffer dispatches 32, 32, 1 for 65 crafts. Busy-target failures return all reserved ingredients. Completed CPU job has exactly the requested outputs and no leftover input reservation.
- AdvancedAE CPU implementation tested through its native logic using a test CPU adapter, not an assembled large CPU structure. Same 1000-craft accounting checks pass.
- Fixed multi-target mixin shadow annotations that previously disabled CPU batching; annotations now explicitly disable remapping. No own invalid-mixin errors in successful test logs.
- Formed native MI Pressurizer accepts an air intake through the hatch crafting capability and produces 1000 mB liquid air. Intake is not returned early, survives ledger save/load, returns exactly once, releases the lock and accepts a repeated job.
- Shared matcher/return path tested on 64 mixed-resource crafts with reusable diamond and water, consumable cobblestone and lava, and exact obsidian/tool/fluid returns. This validates shared semantics, not every individual MI/EI machine.
- Catalyst return ledger persists under the existing private job tag. Preloaded catalysts are not entered into that ledger; only supplied reusable inputs are returned. Unlocking waits for output and catalyst drainage.
- Existing efficiency cap/idle retention, inventory persistence, namespace migration, optional provider, AppliedFlux and EI tests remain passing. User-reported ongoing efficiency fluctuations are not yet reproduced; need whether they occur within one fully powered recipe or on recipe changes. No additional efficiency behavior change in this build.
- GUI visual inspection remains waived. Production JAR excludes GameTest classes, test recipes/structure and dependencies.

## Previous validation
# Modern Energistics 1.0.0 - efficiency cap and inventory grid fixes

- Reproduced recipe-switch overflow in both MI and EI before fixing: MI reached 72 efficiency ticks against a maximum of 8; the EI regression also exceeded its new cap. Evidence: work/efficiency-cap-before.log (two intentional failing regressions).
- CrafterMixin and ModularCrafterMixin now cap bridge-managed efficiency immediately after native recipe-limit recalculation, before overclock calculation, on saved-state load and at tick boundaries. Valid idle efficiency remains retained. Native unmanaged behavior is unchanged.
- Regression covers a lower-cap recipe switch, every processing tick, an already-over-limit saved MI machine, and continued idle retention. Existing native cooldown and subsequent-batch checks remain.
- All 13 all-addon GameTests and five JUnit tests passed after the efficiency fix: work/efficiency-cap-fixed.log.
- Hatch custom background covered the native player inventory grid. BridgeClient now redraws the native 18x18 slot background for each actual player Inventory slot after the fill. Machine slots retain MI's own later rendering. No slot contents or menu logic changed.
- Final build with grid fix and five JUnit tests passed: work/efficiency-grid-final-build.log. GUI visual inspection remains skipped per user instruction; the render-order fix was verified against native MI source and compiled, not visually exercised.
- Production test recipes/classes are excluded from the JAR. Version remains 1.0.0 as requested.

## Previous validation
# Modern Energistics 1.0.0 - namespace revision

Supersedes the older statements below about retaining the old namespace.

- Active mod ID, registry/resource namespace, dependency sections, GUI IDs, recipe/model/loot/tag references, translation keys, mixin configuration, Java packages and Gradle mod/test configuration now use modernenergistics.
- Java entrypoint renamed ModernEnergistics; creative tab title now Modern Energistics. Version stays 1.0.0.
- Only compatibility alias code and its tests refer to the old miaebridge namespace. Existing private job/overflow NBT keys are kept to preserve saved work; they are not registry/mod IDs.
- Registry aliases cover blocks, items, block entities and creative tab. Migration tests deserialize old item stacks, block states and a hatch with stored energy, and verify saves use new IDs. Full-world manual migration has not been performed.
- Appearance, recipe ingredients, efficiency retention and optional-addon behavior remain unchanged.

- Clean all-addon build passed all 12 GameTests and five JUnit tests, including the namespace migration regression (work/modernenergistics-namespace-tests.log).
- Production archive checked for new metadata, new namespace resource paths, no old Java/resources paths and no bundled test classes.

## Historical validation
# Modern Energistics 1.0.0 validation (2026-09-28)

- Advanced MI Pattern Provider recipe and resource generator now use advanced_ae:small_adv_pattern_provider (upstream name Advanced Pattern Provider), not advanced_ae:adv_pattern_provider (upstream name Advanced Extended Pattern Provider). MI Pattern Provider remains the other ingredient.
- Display name Modern Energistics; version 1.0.0. Internal miaebridge IDs remain unchanged for saved-world compatibility.
- Advanced accent #ad83fa; Advanced Extended accent #bb93fa. Programmatic pixel comparison confirms only the 144 accent pixels differ; geometry, background and border pixels are identical.
- Visual GUI checks remain skipped at the user's request.

- Normal accent #efba49; Extended accent #f4ca70. The same 144-pixel-only accent check passes for this pair.
- All-addon build: all 11 GameTests and five JUnit tests passed (work/release-100-tests.log). Final resource-only rebuild passed (work/release-100-final-build.log).
- Production JAR metadata, recipe and textures verified against source; no test classes or dependency JARs bundled.

## Previous validation evidence
# Validation: 0.2.1-alpha (2026-09-28)

- Build succeeded; all five JUnit tests passed.
- All-addon GameTests: 11 passed, including MI Kanthal EBF and EI large electric furnace efficiency retention through 100 idle ticks, a second batch, increasing efficiency, no unauthorized idle outputs, and native explicit cooldown. Log: work/release-021-tests.log.
- Base-only build and GameTests: 11 passed (optional tests skip when their dependencies are absent). Added single-machine retention and provider-removal cooldown assertions. Log: work/release-021-base.log.
- Original nine-square provider faces restored with gold/orange normal/Extended and purple Advanced/combined accents. No upstream texture dependency remains in these provider models.
- Removed hatch chat status, right information panel, information button, EU text and input-rate tracking. Static hatch layout sends no changing status strings; native energy icon/progress remain.
- Efficiency protection is limited to managed crafters between jobs. Active-job power starvation and external unformed-machine cooldown retain native behavior. MI tier limits still cap retained efficiency.
- Visual GUI inspection remains skipped at the user's request. This revision was compiled and server-tested; the prior client startup check below belongs to 0.2.0.
- Upstream optional integration warnings (absent Functional Storage, EMC interface, wireless-terminal serializers) occurred in the addon test environment; all required tests passed.

## Prior release evidence and continuing limits
# Validation — 0.2.0-alpha

Target: Minecraft 1.21.1 / NeoForge 21.1.251 / Java 21.

## Verified
- Full setup: AE2 19.2.17, MI 2.5.8, AdvancedAE 1.6.12, ExtendedAE 2.2.38, AppliedFlux 2.1.6, EI 1.16.2, Tesseract 1.12.16, dependencies as pinned in dependencies.json.
- All 11 final GameTests passed (work/release-020-tests.log), including Advanced 36-to-9 overflow preservation across reload and ordinary hatch energy preservation on drops. Five JUnit tests also passed during the release build.
- Kanthal-tier electric blast furnace accepts a pattern from this mod's combined provider through the hatch, consumes hatch EU, reports progress, produces/returns outputs and releases its recipe lock.
- EI large electric furnace accepts four cobblestone and returns four stone with parallel craft accounting and automatic unlock.
- All four variants accept/save/remove AppliedFlux Induction Cards. A separate test mounts energy storage on real AE networks, observes hatch charging, drains it, observes charging again, then removes cards and verifies charging stops.
- Advanced 9 / Extended 36 / combined 36 capacities and native dispatch. Advanced providers tested with directional AdvancedAE processing patterns.
- Mixed item/fluid atomic insertion, wrong recipe output rejection, insertion rollback, recipe switching, linked hatch aggregation/unlinking, steam fuel, job persistence and output return regressions retained.
- Base-only build and GameTests passed after isolating optional test helpers (work/revision-base-tests.log). Optional tests return early when their dependencies are absent.
- Client startup and resource/model loading succeeded with all addons (work/revision-client.log); no mod-specific missing-resource errors were found.

## Limits
- In-world visual GUI inspection was explicitly skipped by the user. Desktop automation helper could not start due to an environment sandbox ACL error. Do not describe the UI as visually verified.
- Craft tests invoke native provider logic; they do not simulate a player's complete crafting-terminal/CPU interaction.
- EI tests cover its multiplied crafting path with a large electric furnace, not every special EI machine. Steam support is implemented but the EI steam variant is not separately exercised yet.
- Card registration is inherited dynamically, but arbitrary third-party cards are not exhaustively tested. AppliedFlux Induction behavior is tested specifically.
- Automated save/drop and migration assertions are included; no manual multiplayer interaction test has been performed.







