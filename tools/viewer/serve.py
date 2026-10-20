"""Serves the model viewer (index.html beside this file) and the files it shows, on this machine only. Ported from
Starwoven's viewer and taught this mod's layout.

  python tools/viewer/serve.py [port]        (8766 if not given)

Started for the Browser pane by the "model-viewer" entry in .claude/launch.json. The page asks for a model and what goes
with it through /file?p=<path>; a path is taken from the repository's root unless it is whole, and only files inside the
repository or the system's temporary folder are given out.

/list gives every model there is to open, grouped by kind:
    armature   assets/<ns>/armatures/<name>.geo.json: creatures and animated props (CoolCatLib armatures), with their
               clips (<name>.animation.json), textures (textures/armatures/<name>.png and _glow.png) and the props of
               <name>.armature.json
    block      assets/<ns>/models/block/<name>.json: block models as the game builds them (elements, or the vanilla
               parents the mod uses)
    item       assets/<ns>/models/item/<name>.json
    assembly   tools/viewer/assemblies/<name>.json: several blocks (and an armature) put together, for the big ones
    obj        assets/<ns>/models/obj/<name>.obj
It answers from memory, looking again in the background when a request finds the list more than a few seconds old.

/bundle gives every armature at once, for the board (index.html ?board=1): each one's .geo.json, .animation.json and
.armature.json (null where there is none), and its texture and glow texture as data: URLs (null where there is none),
under the very paths the page would ask for them by, so the board is one request instead of a few hundred. It is built
afresh each time (a few dozen small files), and says how long that took and how big it is.

Connections are kept open (HTTP/1.1), and it listens on both 127.0.0.1 and ::1, so a browser's "localhost" (which tries
::1 first on Windows) never waits to fall back for each file.
"""
import base64
import socket
import glob
import http.server
import json
import mimetypes
import os
import sys
import tempfile
import threading
import time
import urllib.parse

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
TEMP = tempfile.gettempdir()
ALLOWED = [os.path.normcase(os.path.realpath(p)) for p in (ROOT, TEMP)]
ASSETS = [os.path.join(ROOT, module, "src", "main", "resources", "assets", "*") for module in ("common", "fabric", "neoforge")]
PATTERNS = []
for a in ASSETS:
    PATTERNS += [("armature", os.path.join(a, "armatures", "*.geo.json")), ("block", os.path.join(a, "models", "block", "*.json")),
                 ("item", os.path.join(a, "models", "item", "*.json")), ("obj", os.path.join(a, "models", "obj", "*.obj"))]
PATTERNS.append(("assembly", os.path.join(HERE, "assemblies", "*.json")))
STALE = 4.0

cache = {"at": 0.0, "models": [], "took": 0.0}
looking = threading.Lock()


def entry(whole, kind):
    name = os.path.basename(whole)
    for suffix in (".geo.json", ".json", ".obj"):
        if name.endswith(suffix):
            name = name[:-len(suffix)]
            break
    label = name
    if kind == "assembly":
        try:
            with open(whole, encoding="utf-8") as f:
                label = json.load(f).get("label", name)
        except (OSError, ValueError):
            pass
    return {"path": os.path.relpath(whole, ROOT).replace(os.sep, "/"), "name": label, "kind": kind, "group": "mod", "where": "",
            "modified": os.path.getmtime(whole)}


def look():
    with looking:
        if time.time() - cache["at"] < 0.5:
            return cache["models"]
        started = time.time()
        found = []
        for kind, pattern in PATTERNS:
            for whole in glob.glob(pattern):
                try:
                    found.append(entry(whole, kind))
                except OSError:
                    pass
        order = {"armature": 0, "assembly": 1, "block": 2, "item": 3, "obj": 4}
        found.sort(key=lambda m: (order[m["kind"]], m["name"]))
        cache.update(models=found, at=time.time(), took=time.time() - started)
        return cache["models"]


def read_json(rel):
    try:
        with open(os.path.join(ROOT, rel), encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def data_url(rel):
    try:
        with open(os.path.join(ROOT, rel), "rb") as f:
            return "data:image/png;base64," + base64.b64encode(f.read()).decode()
    except OSError:
        return None


def bundle():
    """Every armature's files and textures, keyed by the paths the page asks for them by (see index.html addArmature)."""
    started = time.time()
    files, images, names = {}, {}, []
    for m in models():
        if m["kind"] != "armature" or m["group"] != "mod":
            continue
        geo = m["path"]
        base = geo[:-len(".geo.json")]
        for rel in (geo, base + ".animation.json", base + ".armature.json"):
            files[rel] = read_json(rel)
        definition = files[base + ".armature.json"] or {}
        assets = geo[:geo.index("/armatures/")]
        res_root = geo[:geo.index("/assets/")]
        name = os.path.basename(base)

        def id_file(identifier):
            ns, path = identifier.split(":", 1) if ":" in identifier else ("minecraft", identifier)
            return f"{res_root}/assets/{ns}/{path}"

        def listed(key):
            value = definition.get(key)
            return id_file(value[0]) if isinstance(value, list) and value and isinstance(value[0], str) else None

        for rel in (listed("textures") or f"{assets}/textures/armatures/{name}.png",
                    listed("glow_textures") or f"{assets}/textures/armatures/{name}_glow.png"):
            images[rel] = data_url(rel)
        names.append(name)
    return {"files": files, "images": images, "names": names, "built_ms": round((time.time() - started) * 1000, 1)}


def models(fresh=False):
    if fresh or not cache["at"]:
        return look()
    if time.time() - cache["at"] > STALE and not looking.locked():
        threading.Thread(target=look, daemon=True).start()
    return cache["models"]


class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"                                       # keep the connection for the next file

    def do_GET(self):
        url = urllib.parse.urlparse(self.path)
        if url.path == "/bundle":
            return self.send(json.dumps(bundle(), separators=(",", ":")).encode(), "application/json")
        if url.path == "/list":
            return self.send(json.dumps(models("fresh=1" in url.query)).encode(), "application/json")
        if url.path in ("/", "/index.html"):
            return self.give(os.path.join(HERE, "index.html"))
        if url.path == "/file":
            wanted = urllib.parse.parse_qs(url.query).get("p", [""])[0]
            path = wanted if os.path.isabs(wanted) else os.path.join(ROOT, wanted)
            real = os.path.normcase(os.path.realpath(path))
            if not any(real == root or real.startswith(root + os.sep) for root in ALLOWED):
                return self.send_error(403, "outside the repository and the temporary folder")
            return self.give(path)
        self.send_error(404)

    def send(self, data, kind):
        self.send_response(200)
        self.send_header("Content-Type", kind)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def give(self, path):
        if not os.path.isfile(path):
            return self.send_error(404, "no such file")
        with open(path, "rb") as f:
            self.send(f.read(), mimetypes.guess_type(path)[0] or "application/octet-stream")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8766
    threading.Thread(target=look, daemon=True).start()
    print(f"model viewer on http://localhost:{port}/", flush=True)

    class Server6(http.server.ThreadingHTTPServer):
        address_family = socket.AF_INET6

    # ::1 as well as 127.0.0.1, in the background, where the machine has it.
    try:
        six = Server6(("::1", port), Handler)
        threading.Thread(target=six.serve_forever, daemon=True).start()
    except OSError:
        pass
    http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
