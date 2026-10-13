Drop the mod's sound files here, then run from the repo root:

    powershell -File tools/gen_sounds_json.ps1

That rewrites ../sounds.json. A sound with a file here plays it; a sound without one keeps playing the game's
sound listed below as its stand-in. Nothing else needs changing.

Files must be Ogg Vorbis (.ogg), named in lower case. Make them MONO if the sound comes from a place in the world
(the game cannot place a stereo sound, it plays it at full volume everywhere); the HUD sounds may be stereo.
For several variants of one sound, number them: whisper1.ogg, whisper2.ogg, whisper3.ogg. The game picks one at random.

  File                  When it plays                                       Stand-in until then        Mono?
  blocked.ogg           a sleep blocked by monsters; the Lurker's heartbeat entity.warden.heartbeat    either
  notice.ogg            any other card appearing on the sleep HUD           ui.toast.in                either
  warning.ogg           the warning before a bed explodes                   block.note_block.bass      either
  nudge.ogg             another player in bed nudges you                    block.note_block.bell      either
  whisper.ogg           from a blocking monster, or from under a dark bed   particle.soul_escape       mono
  scare.ogg             the "monster under the bed" scare                   entity.enderman.stare,     mono
                                                                            block.sculk_shrieker.shriek
  lurker/ambient.ogg    the Lurker, now and then                            entity.warden.nearby_close mono
  lurker/hurt.ogg       the Lurker struck                                   entity.phantom.hurt        mono
  lurker/death.ogg      the Lurker killed                                   entity.phantom.death       mono
  lurker/emerge.ogg     the Lurker coming out into the world                entity.warden.emerge       mono
  lurker/vanish.ogg     the Lurker gone (daylight, or its player lost)      entity.enderman.teleport   mono

The heartbeat is played again and again, faster as the Lurker nears (every 1.7 seconds down to every half second),
so keep blocked.ogg to a single beat under half a second.
