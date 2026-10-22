Drop the mod's sound files here, then run from the repo root:

    powershell -File tools/gen_sounds_json.ps1

That rewrites ../sounds.json. A sound with a file here plays it; a sound without one keeps playing the game's
sound listed below as its stand-in. Nothing else needs changing.

Files must be Ogg Vorbis (.ogg), named in lower case. Make them MONO if the sound comes from a place in the world
(the game cannot place a stereo sound, it plays it at full volume everywhere); the HUD sounds may be stereo.
For several variants of one sound, number them: nudge1.ogg, nudge2.ogg, nudge3.ogg. The game picks one at random.

  File                  When it plays                                       Stand-in until then        Mono?
  blocked.ogg           a sleep blocked by monsters                         entity.warden.heartbeat    either
  notice.ogg            any other card appearing on the sleep HUD           ui.toast.in                either
  warning.ogg           the warning before a bed explodes                   block.note_block.bass      either
  nudge.ogg             another player in bed nudges you                    block.note_block.bell      either
  heartbeat/beat.ogg    hiding hunted: a beat comes in (deeper when caught) entity.warden.heartbeat    either
  heartbeat/calm.ogg    a beat caught in the calm window                    block.amethyst_block.chime either
  heartbeat/click.ogg   a click in the heartbeat minigame                   ui.button.click            either
  heartbeat/heart_lost.ogg  a strike: a heart breaks                        entity.item.break          either
  heartbeat/gasp.ogg    the third strike: you gasp                          entity.player.hurt_drown   either
