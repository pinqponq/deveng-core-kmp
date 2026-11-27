#!/usr/bin/env python3
"""Builds a system map page from assets/template.html and a data file.

Usage:
    build_map.py DATA_JS OUTPUT_HTML --title TITLE --subtitle TEXT --footnote TEXT \
        [--storage-key KEY] [--search-hint TEXT]

DATA_JS is a JavaScript file that declares KINDS, EDGE_KINDS, LANES, STATUS,
NODES, EDGES, WHO, STEPS and PASSIVE_IDS (see references/example-data-pinq-center.js).
The script checks the data before it writes anything and stops with a message
when a reference points to a box that does not exist.
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

TEMPLATE = Path(__file__).resolve().parent.parent / "assets" / "template.html"
REQUIRED_NAMES = ["KINDS", "EDGE_KINDS", "LANES", "STATUS", "NODES", "EDGES", "WHO", "STEPS", "PASSIVE_IDS"]


def load_data(data_path: Path) -> dict:
    names = ", ".join(REQUIRED_NAMES)
    script = (
        "const fs=require('fs');"
        "const source=fs.readFileSync(process.argv[1],'utf8');"
        f"const result=new Function(source+'\\nreturn {{{names}}};')();"
        "process.stdout.write(JSON.stringify(result));"
    )
    completed = subprocess.run(["node", "-e", script, str(data_path)], capture_output=True, text=True)
    if completed.returncode != 0:
        raise SystemExit(f"The data file could not be evaluated:\n{completed.stderr.strip()}")
    return json.loads(completed.stdout)


def check_data(data: dict) -> list:
    problems = []
    node_ids = [node["id"] for node in data["NODES"]]
    duplicates = {node_id for node_id in node_ids if node_ids.count(node_id) > 1}
    for node_id in duplicates:
        problems.append(f"Box id used twice: {node_id}")
    known = set(node_ids)
    for node in data["NODES"]:
        if node["kind"] not in data["KINDS"]:
            problems.append(f"Box {node['id']} has unknown kind {node['kind']}")
        if not 0 <= node["lane"] < len(data["LANES"]):
            problems.append(f"Box {node['id']} has lane {node['lane']} outside the lane list")
        status = node.get("status")
        if status is not None and status not in data["STATUS"]:
            problems.append(f"Box {node['id']} has unknown status {status}")
    for source, target, _label, kind in data["EDGES"]:
        for end in (source, target):
            if end not in known:
                problems.append(f"Edge {source} to {target} points to unknown box {end}")
        if kind not in data["EDGE_KINDS"]:
            problems.append(f"Edge {source} to {target} has unknown kind {kind}")
    for index, step in enumerate(data["STEPS"], start=1):
        for entry in step["edges"]:
            for end in entry[:2]:
                if end not in known:
                    problems.append(f"Step {index} edge uses unknown box {end}")
        for end in step.get("extra", []):
            if end not in known:
                problems.append(f"Step {index} extra uses unknown box {end}")
    for entry in data["WHO"]:
        if entry["node"] not in known:
            problems.append(f"Who row {entry['who']} points to unknown box {entry['node']}")
    for passive in data["PASSIVE_IDS"]:
        if passive not in known:
            problems.append(f"PASSIVE_IDS has unknown box {passive}")
    return problems


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("data_js")
    parser.add_argument("output_html")
    parser.add_argument("--title", required=True)
    parser.add_argument("--subtitle", required=True)
    parser.add_argument("--footnote", required=True)
    parser.add_argument("--storage-key")
    parser.add_argument("--search-hint", default="Ara")
    arguments = parser.parse_args()

    data_path = Path(arguments.data_js)
    problems = check_data(load_data(data_path))
    if problems:
        print("\n".join(problems), file=sys.stderr)
        raise SystemExit(1)

    storage_key = arguments.storage_key or re.sub(r"[^a-z0-9]+", "-", arguments.title.lower()).strip("-") + "-view"
    page = TEMPLATE.read_text(encoding="utf-8")
    page = page.replace("/*MAP_DATA*/", data_path.read_text(encoding="utf-8"))
    for placeholder, value in {
        "{{TITLE}}": arguments.title,
        "{{SUBTITLE}}": arguments.subtitle,
        "{{FOOTNOTE}}": arguments.footnote,
        "{{STORAGE_KEY}}": storage_key,
        "{{SEARCH_HINT}}": arguments.search_hint,
    }.items():
        page = page.replace(placeholder, value)
    Path(arguments.output_html).write_text(page, encoding="utf-8")
    print(f"Wrote {arguments.output_html}")


if __name__ == "__main__":
    main()
