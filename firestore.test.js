const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_yoga_123";
const BOB_UID = "bob_yoga_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Alice",
    experienceLevel: "brand_new",
    primaryGoal: "Back & Posture",
    preferredMinutes: 15,
    streakDays: 3,
    totalMinutes: 45,
    totalSessions: 3,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validRoutine(uid) {
  return {
    userId: uid,
    title: "Morning Spine Wakeup",
    subtitle: "Gentle stretches to ease stiffness.",
    focusArea: "Back & Posture",
    level: "brand_new",
    durationMinutes: 15,
    targetHrMin: 75,
    targetHrMax: 105,
    isAiGenerated: true,
    isFavorite: false,
    poses: [
      {
        name: "Cat-Cow Stretch",
        sanskritName: "Marjaryasana-Bitilasana",
        durationSeconds: 60,
        instruction: "Flow slowly with each breath.",
        modification: "Place a folded towel under your knees.",
        breathCue: "Inhale lift chest, exhale round spine",
      },
    ],
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validSession(uid) {
  return {
    userId: uid,
    routineId: "routine_1",
    routineTitle: "Morning Spine Wakeup",
    focusArea: "Back & Posture",
    durationMinutes: 15,
    posesCompleted: 6,
    avgHeartRate: 84,
    peakHeartRate: 96,
    syncedFromWatchH9: true,
    feeling: "just_right",
    createdAt: new Date(),
  };
}

function validWatchSync(uid) {
  return {
    userId: uid,
    deviceName: "Watch H9",
    deviceAddress: "AA:BB:CC:11:22:33",
    heartRateBpm: 72,
    batteryPercent: 86,
    stepsToday: 3420,
    recoveryStatus: "Ready for gentle flow",
    source: "ble_gatt",
    createdAt: new Date(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles or routines", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("routines").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Authenticated user can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      watchH9PairedName: "Watch H9",
      lastRestingHr: 68,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update with ghost field on profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, routines, sessions, or watch syncs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("routines").doc("r1").set(validRoutine(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routines").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("routines").doc("r2").set(validRoutine(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("s1").set(validSession(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("watch_syncs").doc("w1").set(validWatchSync(BOB_UID))
  );
});

test("Authenticated user can create, list, and toggle favorite on own routines", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const routinesCol = aliceDb.collection("users").doc(ALICE_UID).collection("routines");
  await assertSucceeds(routinesCol.doc("r1").set(validRoutine(ALICE_UID)));
  await assertSucceeds(routinesCol.get());
  await assertSucceeds(
    routinesCol.doc("r1").update({
      isFavorite: true,
      updatedAt: new Date(),
    })
  );
});

test("Session logs are append-only (create succeeds, update fails) and watch syncs succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionsCol = aliceDb.collection("users").doc(ALICE_UID).collection("sessions");
  const syncsCol = aliceDb.collection("users").doc(ALICE_UID).collection("watch_syncs");

  await assertSucceeds(sessionsCol.doc("s1").set(validSession(ALICE_UID)));
  await assertSucceeds(sessionsCol.get());
  await assertFails(sessionsCol.doc("s1").update({ durationMinutes: 90 }));

  await assertSucceeds(syncsCol.doc("w1").set(validWatchSync(ALICE_UID)));
  await assertSucceeds(syncsCol.get());
});
