# Commands

Why Can't I Sleep? adds one command, `/whycantisleep`, with two parts.

| Command | Who can use it | What it does |
| --- | --- | --- |
| `/whycantisleep nightmare` | Operators (permission level 2) | Gives you a night terror. |
| `/whycantisleep nightmare <players>` | Operators (permission level 2) | Gives those players a night terror. |
| `/whycantisleep vote yes` | Every player | Answers a sleep vote: go ahead and skip the night. |
| `/whycantisleep vote no` | Every player | Answers a sleep vote: not yet. |

## nightmare

`/whycantisleep nightmare [players]` marks the players (yourself, without a name) for a
[night terror](../../wiki/nights/night-terrors.md):

- A player who is **asleep** has it at once.
- A player who is **awake** has it the next time they lie down. The mark is kept on the player, so it lasts if they
  leave and come back. It skips the usual chance, but they still have to fall asleep.

They wake up shaken, as from any night terror. Other mods can still cancel it through the API's night terror event.

## vote

When the players in bed have voted to skip the night, the few who are still up are asked in chat, with
**Go ahead** and **Not yet** to click. Those buttons run `/whycantisleep vote yes` and `/whycantisleep vote no`, which
you can also type. A "Not yet" ends the vote for a minute; no answer in time skips the night. See
[Sleeping together](../../wiki/sleeping/together.md#sleep-vote).
