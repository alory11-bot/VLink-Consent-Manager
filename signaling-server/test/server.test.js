"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const { WebSocket } = require("ws");
const { createSignalingServer } = require("../src/server");

function token() { return crypto.randomBytes(32).toString("base64url"); }
function receive(ws) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("message timeout")), 1500);
    ws.once("message", data => { clearTimeout(timer); resolve(JSON.parse(data.toString())); });
    ws.once("error", reject);
  });
}
async function connect(url) {
  const ws = new WebSocket(url);
  await new Promise((resolve, reject) => { ws.once("open", resolve); ws.once("error", reject); });
  return ws;
}

test("room creation, invite authentication and SDP/ICE forwarding", async t => {
  const app = createSignalingServer();
  const address = await app.listen(0, "127.0.0.1");
  const url = "ws://127.0.0.1:" + address.port;
  const secret = token();
  const room = "ABCD2345";
  const master = await connect(url);
  const slave = await connect(url);
  t.after(async () => { master.close(); slave.close(); await app.close(); });

  const masterJoined = receive(master);
  master.send(JSON.stringify({ type: "create", room, token: secret }));
  const mj = await masterJoined;
  assert.equal(mj.type, "joined");
  assert.equal(mj.role, "master");

  const invalid = await connect(url);
  const invalidResponse = receive(invalid);
  invalid.send(JSON.stringify({ type: "join", room, token: token() }));
  assert.equal((await invalidResponse).code, "INVALID_OR_EXPIRED_INVITE");
  invalid.close();

  const slaveJoined = receive(slave);
  slave.send(JSON.stringify({ type: "join", room, token: secret }));
  const sj = await slaveJoined;
  assert.equal(sj.role, "slave");

  const forwarded = receive(slave);
  master.send(JSON.stringify({ type: "offer", to: sj.peerId, payload: { sdp: "offer-sdp" } }));
  const offer = await forwarded;
  assert.equal(offer.type, "offer");
  assert.equal(offer.from, mj.peerId);
  assert.equal(offer.payload.sdp, "offer-sdp");
});

test("rejects malformed room credentials", async t => {
  const app = createSignalingServer();
  const address = await app.listen(0, "127.0.0.1");
  const ws = await connect("ws://127.0.0.1:" + address.port);
  t.after(async () => { ws.close(); await app.close(); });
  const closed = new Promise(resolve => ws.once("close", code => resolve(code)));
  ws.send(JSON.stringify({ type: "create", room: "bad", token: "x" }));
  assert.equal(await closed, 4002);
});
