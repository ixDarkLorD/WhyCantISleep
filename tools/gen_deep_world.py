"""Writes the Lucid Deep's terrain and what grows and lies on it: its noise settings (the shape of the islands), its
noises, its one biome, and the features that decorate it.

    python tools/gen_deep_world.py

The islands: broken, lopsided masses of dreamstone hanging at every height from 50 to 220, flatter on top and ragged
underneath, set far apart over the void. Their shape is a 3D noise squashed upright (so islands are wider than tall),
warped sideways by vanilla's shift noises (so no two are the same shape and none is round), and thinned by a slow 2D
noise (so they come in archipelagos with open void between). Each one is ashen moss over ashen soil on top and
dreamstone below, its underside veined and glowing.

On them: dead sleepwood trees, lucid blooms and despair thorns, broken dreamstone pillars, wake crystals, and hanging
veins and dread lanterns under their edges. Every decoration is placed by scanning down (or up) from a random height to
the nearest surface, so the lower islands are dressed as well as the top ones.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DATA = os.path.join(ROOT, "common", "src", "main", "resources", "data", "whycantisleep")
NS = "whycantisleep"


def write(path, data):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def block(name, **properties):
    state = {"Name": name if ":" in name else f"{NS}:{name}"}
    if properties:
        state["Properties"] = {k: str(v).lower() for k, v in properties.items()}
    return state


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def gradient(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient", "from_y": from_y, "to_y": to_y, "from_value": from_value, "to_value": to_value}


def noises():
    # The islands' bodies: middling octaves, so they are a few dozen blocks across with knobbly edges.
    write("worldgen/noise/island_shape.json", {"firstOctave": -6, "amplitudes": [1.0, 1.0, 0.6, 0.35]})
    # Where islands gather and where the void is open: very slow.
    write("worldgen/noise/island_mass.json", {"firstOctave": -9, "amplitudes": [1.0, 0.5]})
    # Which way each island leans, and how high it sits: slow, 2D.
    write("worldgen/noise/island_lift.json", {"firstOctave": -7, "amplitudes": [1.0, 0.4]})
    # Patches on the island tops, and veins on their undersides.
    write("worldgen/noise/ash_patches.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5]})


def terrain():
    shape = {"type": "minecraft:shifted_noise", "noise": f"{NS}:island_shape", "xz_scale": 1.0, "y_scale": 2.2,
             "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}
    mass = {"type": "minecraft:noise", "noise": f"{NS}:island_mass", "xz_scale": 1.0, "y_scale": 0.0}
    lift = {"type": "minecraft:noise", "noise": f"{NS}:island_lift", "xz_scale": 1.0, "y_scale": 0.0}
    # A band of height the islands live in, 0 outside it and 1 inside, with soft edges.
    band = mul(gradient(40, 80, 0.0, 1.0), gradient(190, 230, 1.0, 0.0))
    # Flatter on top, ragged below: density falls away faster going up than going down, so tops are cut level and
    # the undersides hang. `lift` moves that cut up and down from place to place, which also tilts each island.
    tilt = mul(add(gradient(0, 256, 1.2, -1.2), mul(lift, 0.5)), 0.35)
    body = add(add(mul(shape, 1.1), mul(mass, 0.75)), add(tilt, -0.74))
    final = {"type": "minecraft:squeeze", "argument": mul(0.64, {"type": "minecraft:interpolated", "argument":
             {"type": "minecraft:blend_density", "argument": add(mul(body, band), mul(add(band, -1.0), 1.5))}})}
    router = {k: 0.0 for k in ("barrier", "continents", "depth", "erosion", "fluid_level_floodedness", "fluid_level_spread",
                                "lava", "preliminary_surface_level", "ridges", "temperature", "vegetation", "vein_gap",
                                "vein_ridged", "vein_toggle")}
    router["final_density"] = final
    patches = {"type": "minecraft:noise_threshold", "noise": f"{NS}:ash_patches", "min_threshold": 0.25, "max_threshold": 9.0}
    surface = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                    "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:sequence", "sequence": [
             {"type": "minecraft:condition", "if_true": patches, "then_run": {"type": "minecraft:block", "result_state": block("ashen_soil")}},
             {"type": "minecraft:block", "result_state": block("ashen_moss")}]}},
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 2, "add_surface_depth": True,
                                                    "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:block", "result_state": block("ashen_soil")}},
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                    "secondary_depth_range": 0, "surface_type": "ceiling"},
         "then_run": {"type": "minecraft:sequence", "sequence": [
             {"type": "minecraft:condition", "if_true": patches, "then_run": {"type": "minecraft:block", "result_state": block("veined_dreamstone")}}]}},
    ]}
    write("worldgen/noise_settings/lucid_deep.json", {
        "sea_level": -64, "disable_mob_generation": True, "aquifers_enabled": False, "ore_veins_enabled": False,
        "legacy_random_source": False, "default_block": block("dreamstone"), "default_fluid": block("minecraft:air"),
        "noise": {"min_y": 0, "height": 256, "size_horizontal": 2, "size_vertical": 1},
        "noise_router": router, "spawn_target": [], "surface_rule": surface})


# ---- Features ----
def scan_down(steps=32):
    return {"type": "minecraft:environment_scan", "direction_of_search": "down", "max_steps": steps,
            "target_condition": {"type": "minecraft:solid"},
            "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}


def scan_up(steps=32):
    return {"type": "minecraft:environment_scan", "direction_of_search": "up", "max_steps": steps,
            "target_condition": {"type": "minecraft:has_sturdy_face", "direction": "down"},
            "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}


def on_surface(count, below=("ashen_moss", "ashen_soil")):
    """Count tries a chunk, at any height the islands reach, each dropped on to the island surface beneath it."""
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 50}, "max_inclusive": {"absolute": 230}}},
            scan_down(), {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": [f"{NS}:{b}" for b in below]}]}},
            {"type": "minecraft:biome"}]


def under_ledges(count):
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 40}, "max_inclusive": {"absolute": 220}}},
            scan_up(), {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1}, {"type": "minecraft:biome"}]


def patch(name, state):
    write(f"worldgen/configured_feature/{name}.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": state}}})


def spread(tries, xz):
    """After a patch's centre is found: this many tries scattered round it, each kept only where it can grow."""
    return [{"type": "minecraft:count", "count": tries},
            {"type": "minecraft:random_offset", "xz_spread": {"type": "minecraft:trapezoid", "min": -xz, "max": xz, "plateau": 0},
             "y_spread": {"type": "minecraft:trapezoid", "min": -2, "max": 2, "plateau": 0}},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": [f"{NS}:ashen_moss", f"{NS}:ashen_soil"]}]}}]


def features():
    placed = []

    def place(name, placement):
        write(f"worldgen/placed_feature/{name}.json", {"feature": f"{NS}:{name}", "placement": placement})
        placed.append(f"{NS}:{name}")

    # Veins of glowing stone through the islands.
    write("worldgen/configured_feature/ore_veined_dreamstone.json", {"type": "minecraft:ore", "config": {
        "size": 18, "discard_chance_on_air_exposure": 0.0,
        "targets": [{"state": block("veined_dreamstone"), "target": {"predicate_type": "minecraft:block_match", "block": f"{NS}:dreamstone"}}]}})
    write("worldgen/placed_feature/ore_veined_dreamstone.json", {"feature": f"{NS}:ore_veined_dreamstone", "placement": [
        {"type": "minecraft:count", "count": 10}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 40}, "max_inclusive": {"absolute": 230}}},
        {"type": "minecraft:biome"}]})

    # Dead sleepwood trees: a crooked forking trunk, a few tufts of withered leaves.
    write("worldgen/configured_feature/dead_sleepwood.json", {"type": "minecraft:tree", "config": {
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": block("sleepwood_log", axis="y")},
        "trunk_placer": {"type": "minecraft:forking_trunk_placer", "base_height": 4, "height_rand_a": 3, "height_rand_b": 2},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": block("withered_hedge")},
        "foliage_placer": {"type": "minecraft:acacia_foliage_placer", "offset": 0, "radius": 1},
        "below_trunk_provider": {"type": "minecraft:simple_state_provider", "state": block("ashen_soil")},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "decorators": [], "ignore_vines": True}})
    place("dead_sleepwood", on_surface(3))

    patch("lucid_blooms", block("lucid_bloom"))
    place("lucid_blooms", on_surface(1) + spread(6, 4))
    patch("despair_thorns", block("despair_thorns"))
    place("despair_thorns", on_surface(3) + spread(10, 6))

    # Broken pillars of the ruins: a column of bricks, cracked near the top, capped with a chiseled eye or nothing.
    write("worldgen/configured_feature/broken_pillar.json", {"type": "minecraft:block_column", "config": {
        "direction": "up", "prioritize_tip": True,
        "allowed_placement": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
        "layers": [
            {"height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}, "provider": {"type": "minecraft:simple_state_provider", "state": block("dreamstone_pillar", axis="y")}},
            {"height": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 2}, "provider": {"type": "minecraft:weighted_state_provider", "entries": [
                {"data": block("dreamstone_bricks"), "weight": 2}, {"data": block("cracked_dreamstone_bricks"), "weight": 3}]}},
            {"height": 1, "provider": {"type": "minecraft:weighted_state_provider", "entries": [
                {"data": block("chiseled_dreamstone"), "weight": 1}, {"data": block("dreamstone_brick_slab", type="bottom", waterlogged=False), "weight": 2},
                {"data": block("dreamstone_brick_wall", up=True, east="none", west="none", north="none", south="none", waterlogged=False), "weight": 2}]}}]}})
    place("broken_pillar", on_surface(1) + [{"type": "minecraft:rarity_filter", "chance": 2}])

    # Wake crystals, rare, on the island tops.
    write("worldgen/configured_feature/wake_crystal.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": block("wake_crystal", facing="up", waterlogged=False)}}})
    place("wake_crystal", on_surface(1) + [{"type": "minecraft:rarity_filter", "chance": 3}])

    # Under the islands: glowing veins hanging from the rock, and now and then a lantern on a chain of them.
    write("worldgen/configured_feature/hanging_veins.json", {"type": "minecraft:block_column", "config": {
        "direction": "down", "prioritize_tip": True, "allowed_placement": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
        "layers": [{"height": 1, "provider": {"type": "minecraft:simple_state_provider", "state": block("hanging_veins", waterlogged=False)}}]}})
    place("hanging_veins", under_ledges(28))
    write("worldgen/configured_feature/hanging_lantern.json", {"type": "minecraft:simple_block", "config": {
        "to_place": {"type": "minecraft:simple_state_provider", "state": block("dread_lantern", hanging=True, waterlogged=False)}}})
    place("hanging_lantern", under_ledges(1) + [{"type": "minecraft:rarity_filter", "chance": 4}])

    # Steps: 6 underground ores, 9 vegetal decoration. Placed features in the order they come.
    steps = [[] for _ in range(11)]
    steps[6] = [f"{NS}:ore_veined_dreamstone"]
    steps[9] = placed
    return steps


def biome(steps):
    empty = {k: [] for k in ["ambient", "axolotls", "creature", "misc", "monster", "underground_water_creature", "water_ambient", "water_creature"]}
    write("worldgen/biome/the_shallows.json", {
        "attributes": {
            "minecraft:audio/ambient_sounds": {
                "additions": {"sound": "minecraft:ambient.soul_sand_valley.additions", "tick_chance": 0.0111},
                "loop": "minecraft:ambient.soul_sand_valley.loop",
                "mood": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.soul_sand_valley.mood", "tick_delay": 6000}},
            "minecraft:visual/ambient_particles": [
                {"particle": {"type": "minecraft:ash"}, "probability": 0.09},
                {"particle": {"type": "minecraft:white_ash"}, "probability": 0.05},
                {"particle": {"type": "minecraft:crimson_spore"}, "probability": 0.01}],
            "minecraft:visual/fog_color": "#0c0f1a"},
        "carvers": [], "downfall": 0.0, "effects": {"water_color": "#7a0e1e"}, "features": steps,
        "has_precipitation": False, "spawn_costs": {}, "spawners": empty, "temperature": 0.5})


if __name__ == "__main__":
    noises()
    terrain()
    biome(features())
    print("wrote the Lucid Deep's terrain and features")
