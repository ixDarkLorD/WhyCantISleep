# Dreams

Waking in the morning, there's a **50%** chance (the server's setting) you wake with a sleep effect. There are 22:

- **Positive** (9), likelier the better you slept: Early Bird, Refreshed, Sweet Dreams, Clear Mind, Dreamt of a Feast,
  Dreamt of Victory, Lucky Dream, Heroic Dream and Dream of Flight.
- **Negative** (8), likelier the worse you slept: Groggy, Stiff Back, Woke Up Starving, Pounding Headache, Weak Limbs,
  Bleary Eyes, Haunted and Cursed Dream.
- **Neutral** (5), possible after any night: Night Owl, Vivid Dream, Dream of Fire, Dream of the Sea and Restless Legs.

First the kind is drawn: positive weighs your score minus 40, negative 60 minus your score, and neutral always 20. So a
score of 95 is mostly positive with a few neutral, and 20 mostly negative. Then one effect of that kind is drawn by
its **rarity**, among those whose quality range fits your score (Dream of Flight needs 90 or more, Cursed Dream 20 or
less):

| Rarity | Weight |
| --- | --- |
| Common | 60 |
| Uncommon | 25 |
| Rare | 10 |
| Epic | 4 |
| Legendary | 1 |

Each effect gives one or more of the game's potion effects, and is shown on the card and in the sleep status, named in
its rarity's color with a buff, debuff or neutral icon. Data packs can add more ([Sleep effects](../../docs/datapacks/sleep-effects.md)).

## Every sleep effect

The score column is the night's score the effect can come after.

### Positive

| Effect | Rarity | Score | Gives |
| --- | --- | --- | --- |
| **Early Bird** | Common | any | Speed (1 min) |
| **Refreshed** | Common | any | Haste (1 min) |
| **Sweet Dreams** | Common | any | Regeneration (30 s) |
| **Clear Mind** | Uncommon | any | Night Vision (2 min) |
| **Dreamt of a Feast** | Uncommon | any | Saturation II (instant) |
| **Dreamt of Victory** | Rare | 60 or more | Strength (1 min) |
| **Lucky Dream** | Rare | 70 or more | Luck (5 min) |
| **Heroic Dream** | Epic | 80 or more | Absorption II (2 min), Resistance (1 min) |
| **Dream of Flight** | Legendary | 90 or more | Slow Falling (1 min), Jump Boost II (1 min), Speed II (1 min) |

### Negative

| Effect | Rarity | Score | Gives |
| --- | --- | --- | --- |
| **Groggy** | Common | any | Slowness (1 min) |
| **Stiff Back** | Common | any | Mining Fatigue (1 min) |
| **Woke Up Starving** | Common | any | Hunger (30 s) |
| **Pounding Headache** | Uncommon | any | Nausea (10 s) |
| **Weak Limbs** | Uncommon | any | Weakness (1 min) |
| **Bleary Eyes** | Rare | 40 or less | Blindness (5 s) |
| **Haunted** | Epic | 30 or less | Darkness (30 s), Bad Luck (5 min) |
| **Cursed Dream** | Legendary | 20 or less | Poison (5 s), Weakness (2 min) |

### Neutral

| Effect | Rarity | Score | Gives |
| --- | --- | --- | --- |
| **Night Owl** | Common | any | Night Vision (45 s) |
| **Vivid Dream** | Common | any | Glowing (30 s) |
| **Dream of Fire** | Uncommon | any | Fire Resistance (2 min) |
| **Dream of the Sea** | Uncommon | any | Water Breathing (3 min), Dolphin's Grace (1 min) |
| **Restless Legs** | Rare | any | Jump Boost II (1 min), Mining Fatigue (30 s) |
