# Modern Energistics

Modern Energistics connects Applied Energistics 2 autocrafting with Modern Industrialization machines and multiblocks.

Built for **Minecraft 1.21.1**, **NeoForge 21.1.251**, and **Java 21**.

## Features

- Combined ME Input/Output Hatch for supported MI multiblocks.
- Linked hatches share one crafting job and their input/output capacity.
- Multiple item and fluid inputs and outputs.
- Energy input through the combined hatch.
- Recipe locking during processing, with automatic release or optional retained locks.
- Capacity-based ingredient dispatch that respects the amount AE2 requested.
- Support for reusable ingredients, such as the Pressurizer’s air intake.
- Integration with supported Extended Industrialization crafting machines.

## Pattern Providers

| Provider | Pattern slots | Functionality |
|---|---:|---|
| MI Pattern Provider | 9 | Standard processing |
| Advanced MI Pattern Provider | 9 | AdvancedAE functionality |
| Extended MI Pattern Provider | 36 | Expanded pattern storage |
| Advanced Extended MI Pattern Provider | 36 | AdvancedAE functionality with expanded storage |

These providers work with supported single-block machines and combined I/O hatches. Upgrade-card support follows their corresponding native providers. AppliedFlux Induction Cards have been tested.

## How to use

1. Install the required dependencies on both client and server.
2. For a multiblock, place a combined I/O hatch in a supported hatch position and form the structure.
3. Place a powered, network-connected Pattern Provider against the hatch.
4. Put processing patterns in the provider.
5. Request the output through AE2 autocrafting.

The hatch does not store patterns.

For reusable ingredients, include the returned ingredient in the pattern’s outputs. For example:

**1 air intake → 1000 mB liquid air + 1 air intake**

Place liquid air first as the primary output.

## Input capacity

Each combined hatch provides:

- 18 item-input slots and 18 item-output slots.
- Eight fluid-input tanks and eight fluid-output tanks.
- 1,024,000 mB capacity per tank.
- 614,400 EU energy storage.

Providers calculate transfers using the actual available input space, recipe quantities, slot locks, reserved ingredients, and available AE power.

For a recipe consuming one ordinary stackable item, 18 empty input slots can accept up to 1,152 items. Transfers never exceed the remaining requested work.

## Dependencies

Required, tested versions:

- Applied Energistics 2 — 19.2.17
- Modern Industrialization — 2.5.8
- GuideME — 21.1.14

Optional, tested integrations:

- AdvancedAE — 1.6.12
- ExtendedAE — 2.2.38
- AppliedFlux — 2.1.6
- Extended Industrialization — 1.16.2

Install the dependencies required by those optional mods as well.

## Status and known limitations

Modern Energistics is under active development. Back up existing worlds before updating.

- Integration targets recipe-driven crafting machines; not every special MI or EI machine is supported or individually tested.
- Probabilistically consumed ingredients are not supported.
- Chance outputs should not be encoded as guaranteed pattern outputs.
- Keep the submitting provider in place until the job’s outputs return.



## Reporting bugs

Please include:

- Minecraft, NeoForge, and mod versions.
- The machine and Pattern Provider used.
- The processing pattern’s inputs and outputs.
- Steps to reproduce the problem.
- Relevant logs or a crash report.
- Screenshots when useful.

## Credits

Thanks to the developers of Applied Energistics 2, Modern Industrialization, AdvancedAE, ExtendedAE, AppliedFlux, Extended Industrialization, and their supporting libraries.

Modern Energistics is an unofficial addon and is not affiliated with or endorsed by those projects, Mojang, or Microsoft.

## License

Modern Energistics’ original code and artwork are provided under the MIT License. Third-party components remain subject to their respective licenses.
