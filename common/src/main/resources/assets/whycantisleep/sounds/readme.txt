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
  heartbeat/beat.ogg    hiding hunted: a beat comes in                      entity.warden.heartbeat    either
  heartbeat/sting.ogg   hiding hunted: the warning slams in                 block.bell.use             either
  heartbeat/ready.ogg   a beat about to reach the calm window: get ready    block.note_block.pling     either
  heartbeat/calm.ogg    a beat caught in the calm window (bass and bell)    block.amethyst_block.chime either
  heartbeat/click.ogg   a click in the heartbeat minigame                   ui.button.click            either
  heartbeat/heart_lost.ogg  a strike: a heart breaks                        entity.item.break          either
  heartbeat/gasp.ogg    the third strike: you gasp                          entity.player.hurt_drown   either
  terror/drone.ogg      a night terror: the drone under it (12 s, cut off)  ambient.cave               either
  terror/emerge.ogg     the shadow demon rising from the floor              entity.warden.emerge       either
  terror/whisper.ogg    whispers in the dark                                ambient.soul_sand_valley.mood either
  terror/creep.ogg      a shadow hand coming up beside the bed              block.sculk.charge         either
  terror/stare.ogg      face to face with the demon                         entity.enderman.stare      either
  terror/jolt.ogg       the terror's end: a huge hit and a shriek           entity.warden.roar         either
  terror/wake.ogg       waking from a terror: a gasp with a rising chord    block.amethyst_block.chime either

The heartbeat minigame's files (heartbeat/) are made by tools/gen_minigame_sounds.py, and the night terror's
(terror/) by tools/gen_terror_sounds.py; edit and run them to change them, or drop your own files over them.
