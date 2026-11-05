#!/usr/bin/env python3
"""Exercise packaged infrastructure on loopback and discard its SQLite workspace."""

import http.client
import json
from pathlib import Path
import socket
import subprocess
import tempfile
import time


ROOT = Path(__file__).resolve().parents[2]
BOOT = ROOT / "oryxos-boot/target/oryxos.jar"
CLI = ROOT / "oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar"
PRIVATE = "fixture-private-prompt-value"


def request(port, path):
    connection = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
    try:
        connection.request("GET", path)
        response = connection.getresponse()
        return response.status, response.read().decode()
    finally:
        connection.close()


def free_ports():
    with socket.socket() as main, socket.socket() as management:
        main.bind(("127.0.0.1", 0))
        management.bind(("127.0.0.1", 0))
        return main.getsockname()[1], management.getsockname()[1]


def smoke_server(directory, docs):
    port, management = free_ports()
    output = directory / ("docs.log" if docs else "default.log")
    profile = "json-logs,local-docs" if docs else "json-logs"
    command = ["java", f"-Doryxos.home={directory / 'workspace'}", "-jar", str(BOOT),
               f"--spring.profiles.active={profile}", "--server.address=127.0.0.1",
               f"--server.port={port}", f"--management.server.port={management}"]
    with output.open("w") as log:
        process = subprocess.Popen(command, cwd=directory, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 120
            while True:
                if process.poll() is not None:
                    raise AssertionError("Boot exited during startup")
                try:
                    status, body = request(management, "/actuator/health")
                    if status == 200:
                        break
                except (OSError, http.client.HTTPException):
                    pass
                if time.monotonic() >= deadline:
                    raise AssertionError("Boot startup timed out")
                time.sleep(0.25)
            health = json.loads(body)
            assert health["status"] == "UP" and set(health) == {"status", "groups"}
            assert set(health["groups"]) == {"liveness", "readiness"}
            assert request(management, "/actuator/health/liveness")[0] == 200
            assert request(management, "/actuator/health/readiness")[0] == 200
            status, info = request(management, "/actuator/info")
            assert status == 200 and "code" not in json.loads(info)
            status, metrics = request(management, "/actuator/prometheus")
            assert status == 200 and "jvm_memory_used_bytes" in metrics
            assert request(management, "/actuator/env")[0] == 404
            assert request(port, "/actuator/health")[0] == 404
            status, body = request(port, "/api/v1/not-found?prompt=" + PRIVATE)
            assert status == 404
            assert set(json.loads(body)) == {"code", "message", "data", "timestamp"}
            assert json.loads(body)["code"] == "NOT_FOUND"
            status, body = request(port, "/v3/api-docs")
            assert status == (200 if docs else 404)
            if docs:
                assert json.loads(body)["openapi"].startswith("3.")
                assert "code" not in json.loads(body)
                config_status, config = request(port, "/v3/api-docs/swagger-config")
                assert config_status == 200 and json.loads(config)["url"] == "/v3/api-docs"
            assert request(port, "/swagger-ui/index.html")[0] == (200 if docs else 404)
            if not docs:
                for path in ("/webjars/swagger-ui/index.html",
                             "/webjars/swagger-ui/5.32.2/index.html",
                             "/webjars/%73wagger-ui/5.32.2/index.html"):
                    assert request(port, path)[0] == 404
        finally:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
    lines = output.read_text().splitlines()
    records = [json.loads(line) for line in lines if line.strip()]
    assert records and all(isinstance(record, dict) for record in records)
    assert PRIVATE not in output.read_text()
    requests = [record for record in records if "http_request" in record.get("message", "")]
    assert requests and all(record.get("traceId") for record in requests)
    print(f"PASS packaged HTTP, monitoring, JSON, docs={docs}; process stopped")


def main():
    with tempfile.TemporaryDirectory(prefix="oryxos-http-") as temporary:
        directory = Path(temporary)
        for jar in (BOOT, CLI):
            for option in ("--version", "--help", "init"):
                result = subprocess.run(
                    ["java", f"-Doryxos.home={directory / 'cli'}", "-jar", str(jar), option],
                    cwd=directory, capture_output=True, text=True, timeout=30, check=True)
                assert "INFO" not in result.stdout and "WARN" not in result.stdout
                if option == "--version":
                    assert "OryxOS" in result.stdout
            assert not (directory / "cli/oryxos.db").exists()
        print("PASS both CLI packages: version/help/init; no database or HTTP startup")
        smoke_server(directory, False)
        smoke_server(directory, True)


if __name__ == "__main__":
    main()
