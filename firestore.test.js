const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function nowTimestamp() {
  return new Date();
}

function validFarePayload(userId = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId,
    fromLocation: "Saiphai",
    toLocation: "N.Chawnpui",
    distanceKm: 5.0,
    fareAmount: 250,
    routeType: "inter_village",
    notes: "Standard Association Rate",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDriverPayload(userId = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId,
    name: "Lalrinfela Sailo",
    regNo: "MZ-05-A-1421",
    contactPhone: "+91 9862145102",
    standName: "Saiphai Bazar Stand",
    vehicleModel: "Bajaj RE Compact 4S",
    status: "available",
    ratingAvg: 4.8,
    totalTrips: 142,
    currentLat: 24.1954,
    currentLng: 92.7661,
    isVerified: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validBookingPayload(userId = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId,
    ticketCode: "SPH-2026-1001",
    passengerName: "Zorama Hmar",
    passengerPhone: "+91 8794112233",
    driverId: "driver_01",
    driverName: "Lalrinfela Sailo",
    driverRegNo: "MZ-05-A-1421",
    driverPhone: "+91 9862145102",
    pickupName: "Saiphai Bazar Veng",
    dropoffName: "N.Chawnpui Kawn",
    pickupLat: 24.1954,
    pickupLng: 92.7661,
    dropoffLat: 24.221,
    dropoffLng: 92.789,
    distanceKm: 5.0,
    fareAmount: 250,
    paymentMode: "cash",
    paymentStatus: "pending",
    status: "booked",
    otpPin: "4829",
    driverProgress: 0.0,
    passengerRated: false,
    driverRated: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validReviewPayload(userId = ALICE_UID) {
  const ts = nowTimestamp();
  return {
    userId,
    bookingId: "booking_01",
    targetId: "driver_01",
    targetName: "Lalrinfela Sailo",
    targetRole: "driver",
    reviewerName: "Zorama Hmar",
    rating: 5,
    comment: "Tlan tluang tak a ni, hun vawng dik hle.",
    routeSummary: "Saiphai -> N.Chawnpui",
    createdAt: ts,
    updatedAt: ts,
  };
}

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user cannot read or create fares, drivers, bookings, or reviews", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("fares").get());
  await assertFails(unauthDb.collection("drivers").get());
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("reviews").get());
  await assertFails(
    unauthDb.collection("fares").doc("fare_1").set(validFarePayload())
  );
});

test("Authenticated user can create and read their own fare, driver, booking, and review", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("fares").doc("fare_1").set(validFarePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("drivers")
      .doc("driver_1")
      .set(validDriverPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("bookings")
      .doc("booking_1")
      .set(validBookingPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("reviews")
      .doc("review_1")
      .set(validReviewPayload(ALICE_UID))
  );

  // Scoped queries succeed
  await assertSucceeds(
    aliceDb.collection("fares").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("drivers").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("bookings").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("reviews").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user cannot read or modify another user's documents", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("bookings")
      .doc("bob_booking")
      .set(validBookingPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("bookings").doc("bob_booking").get());
  await assertFails(
    aliceDb.collection("bookings").doc("bob_booking").update({
      status: "cancelled",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Unfiltered list query without userId filter is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("bookings").get());
  await assertFails(aliceDb.collection("fares").get());
});

test("Shadow field injection and invalid enum values are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("fares")
      .doc("fare_shadow")
      .set({ ...validFarePayload(ALICE_UID), ghostField: true })
  );
  await assertFails(
    aliceDb
      .collection("bookings")
      .doc("booking_bad_status")
      .set({ ...validBookingPayload(ALICE_UID), status: "invalid_status" })
  );
});

test("Terminal state lock prevents reverting cancelled booking", async () => {
  const initialTs = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("bookings")
      .doc("cancelled_booking")
      .set({
        ...validBookingPayload(ALICE_UID),
        status: "cancelled",
        createdAt: initialTs,
        updatedAt: initialTs,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("bookings").doc("cancelled_booking").update({
      status: "in_progress",
      updatedAt: nowTimestamp(),
    })
  );
});
