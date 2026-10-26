# Sleeping together

On a server, skipping the night takes more than one bed. The game wants a share of the players asleep (the
`playersSleepingPercentage` game rule, half of them by default), and from your bed you can't tell who is still up or
why. The **Bedtime** panel shows you, lets you nudge the ones keeping you waiting, and, when only a player or two is
still busy, lets everyone in bed vote to skip the night anyway.

All of this needs the mod on the server, and on your client to see the panel.

## The Bedtime panel

While you're in bed with at least one other player online, a panel appears in the top left of the screen:

- **Bedtime** and the count, **2 of 3 asleep**.
- A bar in segments, one for each sleeper the night needs, filled green for those asleep.
- **Still awake**: a row for each player who isn't in bed, with their face, their name, and a colored dot and word for
  why they're up. Up to ten rows are shown, then "...and 2 more".
- A footer: **Click to nudge** on the left, and **Ready** or **Ready in 12s** on the right.

When nobody is awake it says **Everyone is asleep**. If that still isn't enough (the rule asks for more players than
there are), it says **This night can't be skipped**.

The panel counts the way the game does: spectators aside, the players in your dimension. Players in another dimension
are listed too, dimmed, but don't count either way.

### Why they're awake

| Reason | Color | Means |
| --- | --- | --- |
| **Other dimension** | Pink | They're in another dimension. |
| **Fighting** | Red | They hit a mob, or a mob hit them, in the last 5 seconds. |
| **Mining** | Orange | They broke a block in the last 5 seconds. |
| **AFK** | Gray | They haven't done anything for a minute. |
| **Awake** | White | None of the above: they're just up. |

When more than one fits, the first in this list wins.

The panel is on by default. You can hide it in the client settings, and the server can turn it off for everyone
([Configuration](../server/configuration.md)).

## Nudges

**Click an awake player's row** to nudge them. They hear a sound and get a chat message: "*Steve* is in bed and
waiting for you to sleep". A card tells you "You nudged *Alex*". The player you nudge doesn't need the
mod.

You can only nudge from bed, and only players who are awake. After each nudge you wait out a cooldown, **30 seconds**
by default, before you can nudge anyone again; the panel's footer counts it down. The server can change the cooldown
or turn nudges off.

## The sleep vote { #sleep-vote }

Sometimes everyone is in bed except one player who is deep in a mine. Rather than nudging them again and again, the
players in bed can vote to skip the night.

**When you can vote**: the night needs more sleepers than it has, at least one player is in bed, and no more than
**one** player in the dimension is still up (the server can allow up to 10). The panel then shows a row under the
awake players: **Vote to skip the night**. Click it to vote. Everyone in bed sees each vote as it's cast, with the count.

**When everyone in bed has voted**, the players still up are asked in chat:

> Everyone in bed (3) would like to skip the night. Skip it now? With no answer it goes ahead in 30 seconds.
> **[Go ahead]** **[Not yet]**

- **Go ahead** says yes. Once everyone asked has said yes, the night is skipped straight away.
- **Not yet** keeps the night going. Everyone in bed is told, and nobody can start a new vote for a minute.
- **No answer** within the wait (30 seconds by default, 5 to 120 on the server) counts as a yes: the night is
  skipped.

A skipped night is skipped the game's own way: the clock moves to morning and everyone asleep wakes up, as if all had
slept. While waiting, the panel's vote row counts down the time the others have to answer.

The vote is called off if enough players go to bed by themselves, or more get up. A player who gets out of bed loses
their vote.

!!! tip
    The chat buttons run `/whycantisleep vote yes` and `/whycantisleep vote no`, which you can also type
    ([Commands](../server/commands.md)).

## Shared beds

Sleeping side by side has a bonus: monsters bother you less. If another player is **already asleep** in a bed within 3
blocks of yours, your bed lets up to **2** monsters by. Only a third monster in range keeps you up, and then all of them
are [shown to you](why-cant-i-sleep.md#monsters-in-the-way) as usual. The server can change how many are let by (0 to
10).
