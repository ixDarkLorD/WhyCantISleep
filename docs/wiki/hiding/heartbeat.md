# The heartbeat minigame

If you hide with something hunting you, you have to keep your heart slow. A warning slams in ("Watch out!" or "Be
careful!"), the rest of the HUD fades, and a band of smoke opens across the screen with a pulse line through it.

- While a monster is within 12 blocks, heartbeats come one after another, each crossing the line from right to left.
- **Left-click as the beat passes through the calm window in the middle.** Each caught beat chimes a little higher
  through a streak ("Steady...", "Doing great!", "Cool as ice!").
- Clicking too early or too late, or letting a beat pass, is a **strike** and breaks one of your three hearts. Your
  second strike brings "Last chance!".
- At the third strike **you gasp**, as loud as you can be. Anything near enough to hear (as for [noise](noise.md#noise))
  finds you.
- When the hide time is up, it ends: "You held your nerve!"

While it runs, your movements make no noise: only your heart can give you away. A dark red edge and veins creep in from
the screen's edges with each strike, and fall back as a streak of caught beats calms you.

**Overtime**: if you missed the last beat when the hide time runs out, and monsters are still near, you get "One more
beat!": the hide time stretches for one more beat, which you must catch. This can happen at most twice. Catching an
overtime beat ends the minigame at once.

The server can turn the minigame off, and hunted players then just hide as anyone else does.
