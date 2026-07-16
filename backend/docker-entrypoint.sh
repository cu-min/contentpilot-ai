#!/usr/bin/env bash
set -e

log() {
  printf '[entrypoint] %s\n' "$1"
}

export DISPLAY="${DISPLAY:-:99}"
XVFB_SCREEN="${XVFB_SCREEN:-1440x1000x24}"

mkdir -p /app/logs /app/browser-data/csdn /app/browser-data/zhihu

if [ -z "${VNC_PASSWORD:-}" ]; then
  log "ERROR: VNC_PASSWORD is required; refusing to start an unauthenticated VNC server."
  exit 1
fi

log "Starting Xvfb on ${DISPLAY} (${XVFB_SCREEN})"
Xvfb "${DISPLAY}" -screen 0 "${XVFB_SCREEN}" -ac +extension GLX +render -noreset &
sleep 1

log "Starting fluxbox window manager"
fluxbox >/tmp/fluxbox.log 2>&1 &

log "Starting x11vnc on port 5900 with password authentication"
x11vnc -storepasswd "${VNC_PASSWORD}" /tmp/x11vnc.pass >/dev/null 2>&1
x11vnc -display "${DISPLAY}" -forever -shared -rfbport 5900 -rfbauth /tmp/x11vnc.pass >/tmp/x11vnc.log 2>&1 &

log "Starting noVNC on port 6080"
if command -v novnc_proxy >/dev/null 2>&1; then
  novnc_proxy --listen 0.0.0.0:6080 --vnc localhost:5900 >/tmp/novnc.log 2>&1 &
else
  websockify --web=/usr/share/novnc 0.0.0.0:6080 localhost:5900 >/tmp/novnc.log 2>&1 &
fi

log "Starting Spring Boot application"
exec java ${JAVA_OPTS:-} -jar /app/app.jar
