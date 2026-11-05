#!/usr/bin/env python3
"""Local Flight Explorer API — GET /flights serves fixtures/flights.json."""

from __future__ import annotations

import json
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FIXTURE = ROOT / "fixtures" / "flights.json"
HOST = "0.0.0.0"
PORT = 8080


def load_fixture() -> bytes:
    if not FIXTURE.is_file():
        raise FileNotFoundError(f"Missing fixture at {FIXTURE}")
    data = json.loads(FIXTURE.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or "flights" not in data:
        raise ValueError("Fixture must be a JSON object with a 'flights' key")
    return json.dumps(data, separators=(",", ":")).encode("utf-8")


BODY = load_fixture()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _cors(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")

    def do_OPTIONS(self) -> None:  # noqa: N802
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_GET(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        if path in ("/flights", "/flights/"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(BODY)))
            self._cors()
            self.end_headers()
            self.wfile.write(BODY)
            return
        if path in ("/", "/health"):
            msg = b'{"ok":true,"endpoint":"/flights"}\n'
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(msg)))
            self._cors()
            self.end_headers()
            self.wfile.write(msg)
            return
        self.send_response(404)
        msg = b'{"error":"not found"}\n'
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(msg)))
        self._cors()
        self.end_headers()
        self.wfile.write(msg)


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"Flight Explorer API listening on http://127.0.0.1:{PORT}/flights", flush=True)
    print(f"Serving {FIXTURE} ({len(json.loads(BODY)['flights'])} flights)", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.", flush=True)
        server.server_close()


if __name__ == "__main__":
    main()
