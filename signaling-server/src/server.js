"use strict";

const crypto = require("node:crypto");
const http = require("node:http");
const { WebSocketServer, WebSocket } = require("ws");

const ROOM_TTL_MS = 5 * 60 * 1000;
const MAX_MESSAGE_BYTES = 16 * 1024;
const MAX_PEERS_PER_ROOM = 9;
const ROOM_RE = /^[A-Z2-9]{8}$/;
const TOKEN_RE = /^[A-Za-z0-9_-]{43}$/;

function tokenHash(token) {
  return crypto.createHash("sha256").update(token, "utf8").digest();
}
function safeEqual(a, b) {
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}
function validToken(value) {
  return typeof value === "string" && TOKEN_RE.test(value);
}
function createSignalingServer(options = {}) {
  const now = options.now || Date.now;
  const rooms = new Map();
  const server = options.server || http.createServer((req, res) => {
    if (req.url === "/health") {
      res.writeHead(200, { "content-type": "application/json", "cache-control": "no-store" });
      res.end(JSON.stringify({ ok: true, service: "vlink-signaling", mode: "signaling-only" }));
      return;
    }
    res.writeHead(404); res.end();
  });
  const wss = new WebSocketServer({
    server,
    maxPayload: MAX_MESSAGE_BYTES,
    perMessageDeflate: false
  });

  function send(ws, message) {
    if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(message));
  }
  function closePeer(ws, code = 1000, reason = "session closed") {
    try { ws.close(code, reason); } catch {}
  }
  function prune() {
    const t = now();
    for (const [id, room] of rooms) {
      // Invite expiry stops new joins; it must not tear down an already active P2P call.
      if (room.peers.size === 0 || (room.expiresAt <= t && room.peers.size === 1)) {
        for (const peer of room.peers.values()) closePeer(peer.ws, 4001, "session expired");
        rooms.delete(id);
      }
    }
  }
  const sweep = setInterval(prune, 30_000);
  sweep.unref?.();

  wss.on("connection", (ws) => {
    let membership = null;
    let messageCount = 0;
    let windowStart = now();

    ws.on("message", (raw) => {
      const t = now();
      if (t - windowStart >= 10_000) { windowStart = t; messageCount = 0; }
      messageCount += 1;
      if (messageCount > 60) return closePeer(ws, 4008, "rate limit");

      let msg;
      try { msg = JSON.parse(raw.toString("utf8")); } catch { return closePeer(ws, 4002, "invalid JSON"); }
      if (!msg || typeof msg !== "object" || Array.isArray(msg)) return closePeer(ws, 4002, "invalid message");

      if (msg.type === "create" || msg.type === "join") {
        if (membership) return closePeer(ws, 4003, "already joined");
        if (typeof msg.room !== "string" || !ROOM_RE.test(msg.room) || !validToken(msg.token)) {
          return closePeer(ws, 4002, "invalid room credentials");
        }
        let room = rooms.get(msg.room);
        if (msg.type === "create") {
          if (room) return send(ws, { type: "error", code: "ROOM_EXISTS" });
          room = { tokenHash: tokenHash(msg.token), expiresAt: t + ROOM_TTL_MS, peers: new Map() };
          rooms.set(msg.room, room);
        } else if (!room || room.expiresAt <= t || !safeEqual(room.tokenHash, tokenHash(msg.token))) {
          return send(ws, { type: "error", code: "INVALID_OR_EXPIRED_INVITE" });
        }

        const role = msg.type === "create" ? "master" : "slave";
        if (role === "master" && [...room.peers.values()].some(p => p.role === "master")) {
          return send(ws, { type: "error", code: "MASTER_EXISTS" });
        }
        if (room.peers.size >= MAX_PEERS_PER_ROOM) return send(ws, { type: "error", code: "ROOM_FULL" });
        const peerId = crypto.randomUUID();
        membership = { roomId: msg.room, peerId, role };
        room.peers.set(peerId, { ws, role });
        send(ws, { type: "joined", peerId, role, expiresAt: room.expiresAt });
        for (const [otherId, peer] of room.peers) {
          if (otherId !== peerId) send(peer.ws, { type: "peer-joined", peerId, role });
        }
        return;
      }

      if (!membership) return closePeer(ws, 4003, "join a room first");
      const room = rooms.get(membership.roomId);
      if (!room) return closePeer(ws, 4001, "session expired");
      if (!["offer", "answer", "candidate", "bye"].includes(msg.type)) {
        return closePeer(ws, 4002, "unsupported message type");
      }
      if (msg.type === "bye") {
        for (const [id, peer] of room.peers) if (id !== membership.peerId) send(peer.ws, { type: "peer-left", peerId: membership.peerId });
        room.peers.delete(membership.peerId);
        membership = null;
        return;
      }
      if (typeof msg.to !== "string" || typeof msg.payload !== "object" || msg.payload === null) {
        return closePeer(ws, 4002, "invalid signaling envelope");
      }
      const target = room.peers.get(msg.to);
      if (!target || target.role === membership.role) return send(ws, { type: "error", code: "INVALID_TARGET" });
      const encoded = JSON.stringify({ type: msg.type, from: membership.peerId, payload: msg.payload });
      if (Buffer.byteLength(encoded) > MAX_MESSAGE_BYTES) return closePeer(ws, 4009, "message too large");
      // Only SDP/ICE negotiation messages are forwarded. Application data never enters this server.
      send(target.ws, JSON.parse(encoded));
    });

    ws.on("close", () => {
      if (!membership) return;
      const room = rooms.get(membership.roomId);
      if (!room) return;
      room.peers.delete(membership.peerId);
      for (const peer of room.peers.values()) send(peer.ws, { type: "peer-left", peerId: membership.peerId });
      if (room.peers.size === 0) rooms.delete(membership.roomId);
    });
  });

  return {
    server, wss, rooms,
    listen(port = Number(process.env.PORT || 8080), host = process.env.HOST || "0.0.0.0") {
      return new Promise(resolve => server.listen(port, host, () => resolve(server.address())));
    },
    close() {
      clearInterval(sweep);
      for (const room of rooms.values()) for (const peer of room.peers.values()) closePeer(peer.ws);
      rooms.clear();
      return new Promise(resolve => wss.close(() => server.close(() => resolve())));
    }
  };
}

if (require.main === module) {
  const app = createSignalingServer();
  app.listen().then(address => process.stdout.write("VLink signaling listening on " + JSON.stringify(address) + "\n"));
}
module.exports = { createSignalingServer };
