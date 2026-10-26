# Noise and searches

Hidden, nothing can see you, but it can still hear you, and the monster on your heels may come and look.

## Noise

Monsters can't see you, but they can hear you. Trying to walk or jump, or swinging your view about, makes noise; it
fades again while you keep still. When the noise builds up with monsters near, the card says **Hold still!** "They'll
hear you. Don't move until they're gone", and its foot line shows how much noise you're making.

If the noise reaches its peak while something can hear it (a monster that was after you, or a monster within 12
blocks of the spot), **it heard you**: you're pulled out, and they all come for you at once. The server can turn this
off.

## Monsters searching

When you run into a spot with something on your heels, the nearest of your hunters comes looking. After a moment it
walks up to the front of your spot, stops, and looks around (now and then right at the spot). Then it either finds you
and drags you out ("It found you!"), or walks off.

The chance it finds you is the server's search chance, **25%** on Normal difficulty, half that on Easy, half as much
again on Hard and none on Peaceful, plus more the more noise you're making. While a search is under way, the hide time
waits for it to finish.

While you're playing the heartbeat minigame, the search **never** finds you by chance: only failing the minigame gives
you away.

## Simple Voice Chat { #voice-chat }

With Simple Voice Chat installed ([Compatibility](../../docs/compatibility.md)), hiding hears your voice too.

- **Your voice is noise.** Speaking while hidden adds to your [noise](#noise), as loud as you speak, even during the
  heartbeat minigame. Steady talking gives you away in about three seconds with a monster near, a shout in about one
  and a half.
- **Whispering is the safe way to talk.** Simple Voice Chat's whisper counts for much less, and a normal whisper never
  builds up to being heard.
- **Others hear you muffled.** A hiding player's voice reaches everyone else as their spot lets it out: through a
  chest's lid crack, a closet door's window, the open sides under a bed. If you're hiding yourself, you hear every
  voice through your own spot as well.

The server can stop voices from giving hiders away ([Configuration](../../docs/server/configuration.md)).
