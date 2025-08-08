Heck yes—let’s ship this in small, reversible slices. Here’s a step‑by‑step plan with clear checkpoints, fallbacks, and exact files to touch so we can bail out at any point without wrecking the rest of the app.

# Phase 0 — Prep & Guardrails (feature‑flag first)

**Goal:** Add the scaffolding so all new behavior can be turned off instantly.

* **Add a feature flag**

  * File: `MainViewModel.kt` (or a new `FeatureFlags.kt`)
  * Flag: `enableInteractiveRadar: Boolean = true`
  * Wire through to `MainScreen.kt` → `RadarCanvas(...)` so the composable can behave read‑only when off.
* **Acceptance:** Toggling the flag off gives today’s behavior; no new gestures.
* **Fallback:** Flip flag to `false`, redeploy. No code reverts required.

---

# Phase 1 — Domain Types (no UI changes)

**Goal:** Add the minimal types used by later steps.

* **Add enums & UI model**

  * File: `MatchResult.kt` or new `InteractionTypes.kt`
  * `LikeType { LIKE, SUPER_LIKE }`
  * `MatchStatus { NONE, LIKED, SUPER_LIKED, MUTUAL, CONNECTED }`
  * Optional `MatchUiState` if you don’t already have a per‑dot UI struct.
* **Acceptance:** App compiles; existing screens unaffected.
* **Fallback:** Keep types; they’re inert. No rollback needed.

---

# Phase 2 — Local “Like” State (no BLE yet)

**Goal:** Let taps/long‑presses mark a dot as liked/super‑liked locally.

* **Implement handlers in ViewModel**

  * File: `MainViewModel.kt`
  * `onDotTapped(id)` → sets status `LIKED`
  * `onDotLongPressed(id)` → sets status `SUPER_LIKED`
  * Maintain a `likedThem: MutableSet<String>`
* **Hook gestures**

  * File: `RadarCanvas.kt`
  * If `enableInteractiveRadar`:

    * Tap hit‑test → `onDotTap(id)`
    * Long‑press → `onDotLongPress(id)`
* **Acceptance:** Visual change of dot color/state on tap/long‑press; nothing else.
* **Fallback:** Set flag off → gestures disabled; code remains.

---

# Phase 3 — BLE Message Schema (shadow mode)

**Goal:** Define LIKE/SUPER\_LIKE/MUTUAL opcodes and serialize/parse, but **don’t act** on them yet.

* **Constants**

  * File: `BLEConstants.kt`
  * `LIKE = 0x01`, `SUPER_LIKE = 0x02`, `MUTUAL = 0x10`, `CONNECT_REQ = 0x20`
* **Serialization helpers**

  * New small `ProxiPayloads` helper or add to `BLEAdvertiser.kt` / `BLEScanner.kt`
* **Shadow receive path**

  * File: `BLEScanner.kt`
  * Parse inbound likes and **log/telemetry only** (no UI change)
* **Acceptance:** Logs show outbound/ inbound packets; UI unchanged.
* **Fallback:** Comment out parser registration or gate by flag; no UI impact.

---

# Phase 4 — Wire “send like” (single‑sided)

**Goal:** On local like/super‑like, send the BLE payload. Still no mutual logic.

* **Send on intent**

  * File: `MainViewModel.kt` + `BLEAdvertiser.kt`
  * In `likeInternal(targetId, type)`: call `sendLike(myId, targetId, type)`
* **Acceptance:** Verified BLE TX in logs / scanner tool; UI as Phase 2.
* **Fallback:** Wrap `sendLike` call in the feature flag; toggle off to stop emissions.

---

# Phase 5 — Mutual Detection (two paths)

**Goal:** When we receive a like that targets us **and** we already liked them, flip both sides to `MUTUAL`.

* **Receiver logic**

  * File: `MainViewModel.kt` (or wherever scanner callbacks land)
  * On inbound `(senderId, targetId)` where `targetId == myId`:

    * If `senderId in likedThem` → `markMutual(senderId)` and (optionally) emit `MUTUAL`.
* **UI effect**

  * File: `RadarCanvas.kt`
  * Add infinite rings animation for `status == MUTUAL`.
* **Acceptance:** Two devices can like each other (order irrelevant) → both show rings.
* **Fallback:** Gate the `markMutual` transition behind a sub‑flag `enableMutualTransition`. Turn off to revert to one‑sided likes only.

---

# Phase 6 — Rings Tap → Connect (stub)

**Goal:** Tapping the rings triggers a connection attempt (for now, a stubbed secure handshake).

* **UI gesture**

  * File: `RadarCanvas.kt`
  * Expand hit‑box when `MUTUAL` and call `onRingsTap(id)`
* **ViewModel**

  * File: `MainViewModel.kt`
  * `onRingsTapped(peerId)` → call `secureConnector.startHandshake(peerId)` (stub), set status `CONNECTED` if success; navigate to chat or connection screen.
* **Acceptance:** After mutual, tap rings → see toast/navigation; state becomes `CONNECTED` on success.
* **Fallback:** Gate `onRingsTapped` behind `enableConnectFromRings`. Off → rings are visual only.

---

# Phase 7 — UX Polish & A11y

**Goal:** Make it feel premium, but safe to skip if needed.

* Haptics on like/super‑like/mutual.
* Content descriptions per dot (“Name, 12m, mutual”).
* Confetti/snackbar on mutual.
* **Files:** `RadarCanvas.kt`, `MainScreen.kt`, `Color.kt`/theme if needed.
* **Fallback:** Pure UI; easy to revert.

---

# Phase 8 — Telemetry & Rate Limits

**Goal:** Protect BLE channel and measure usage.

* **Rate limit**

  * Simple debounce: prevent sending another like to same `peerId` for N seconds.
* **Analytics**

  * Counters for: likes sent/received, mutuals formed, rings taps, connect success/fail.
* **Fallback:** Disable analytics events; keep rate limit.

---

# Phase 9 — Secure Handshake (upgrade stub later)

**Goal:** Replace stub with real ECDH over GATT.

* Define characteristics: `PUB_KEY`, `NONCE`, `CONFIRM`, `ENCRYPTED_DATA`.
* Flow: Curve25519 → HKDF → session key → confirm.
* **Files:** new `SecureConnectorImpl`, `ProxiGattManager`.
* **Acceptance:** Keys derived match on both sides; encrypted echo works.
* **Fallback:** Flip back to stub by DI (constructor picks `NoopSecureConnector`).

---

# Phase 10 — QA Matrix

**Goal:** Validate across common states.

* Single device: likes only.
* Two devices: like → mutual (both orders).
* Edge cases: rapid double taps, long‑press cancel, out‑of‑range while connecting, app backgrounded.
* Accessibility: TalkBack focus, target size ≥ 44dp.
* Battery: confirm no continuous advertise spam (respect your existing scanning cadence).
* **Fallback:** If any fail catastrophically, revert to previous phase tag.

---

# Rollout Strategy (with “oh no” buttons)

1. **Git tags per phase:** `v-interactive-0` … `v-interactive-10`
2. **Flags:**

   * `enableInteractiveRadar`
   * `enableMutualTransition`
   * `enableConnectFromRings`
3. **Staged enablement:**

   * Internal dogfood: flag on for debug builds only.
   * Canary: 5–10% of users (if you have remote config).
4. **Instant rollback:** Flip `enableInteractiveRadar=false` remotely; if not available, revert to last tag.

---

# Concrete To‑Dos (day‑by‑day-ish)

* **Day 1:** Phase 0–2 (flag, types, local state) ✅
* **Day 2:** Phase 3–4 (BLE schema + send, shadow receive) ✅
* **Day 3:** Phase 5 (mutual + rings animation) ✅
* **Day 4:** Phase 6 (rings tap → stub connect) ✅
* **Day 5:** Phase 7–8 (polish + telemetry + rate limit) ✅
* **Later:** Phase 9 (real secure handshake), then QA pass (Phase 10)

---

Want me to start by patching **Phase 0–2** into your existing files (`MainViewModel.kt`, `RadarCanvas.kt`, `BLEConstants.kt`, maybe a tiny `FeatureFlags.kt`) so you can run it right away?

