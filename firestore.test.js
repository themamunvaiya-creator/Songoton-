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
const ADMIN_EMAIL = "mnmamunmama@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("members").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notices").doc("n1").set({
      userId: ALICE_UID,
      title: "Unauth",
      message: "Blocked",
      priority: "NORMAL",
      authorName: "Test",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid UserProfile create & partial update succeed; ghost field rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "আব্দুল্লাহ আল মামুন",
      email: "alice@example.com",
      designation: "সাধারণ সম্পাদক",
      branchName: "পূর্ব হারবাং মুকিয়াঘোনা",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      designation: "সভাপতি",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Members: valid Kotlin-mirrored create/update/list succeed; cross-user & @DocumentId 'id' field rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").set({
      userId: ALICE_UID,
      fullName: "মোহাম্মদ করিম",
      role: "কোষাধ্যক্ষ",
      phone: "01812345678",
      bloodGroup: "O+",
      area: "মুকিয়াঘোনা",
      activeStatus: "ACTIVE",
      monthlyFeeBdt: 200,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").update({
      activeStatus: "INACTIVE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId 'id' key in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_2").set({
      id: "mem_2",
      userId: ALICE_UID,
      fullName: "অবৈধ সদস্য",
      role: "সদস্য",
      phone: "",
      bloodGroup: "A+",
      area: "হারবাং",
      activeStatus: "ACTIVE",
      monthlyFeeBdt: 100,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject cross-user read and write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("members").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").update({
      fullName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Transactions: positive amount required, future timestamp rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "মাসিক চাঁদা সংগ্রহ",
      txType: "INCOME",
      category: "মাসিক চাঁদা",
      amountBdt: 1500,
      memberName: "মোহাম্মদ করিম",
      dateLabel: "১৯ নভেম্বর ২০২৬",
      note: "নভেম্বর মাসের চাঁদা",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Negative/zero amount rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
      userId: ALICE_UID,
      title: "ভুল এন্ট্রি",
      txType: "INCOME",
      category: "মাসিক চাঁদা",
      amountBdt: 0,
      memberName: "",
      dateLabel: "আজ",
      note: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_future").set({
      userId: ALICE_UID,
      title: "ভবিষ্যৎ তারিখ",
      txType: "EXPENSE",
      category: "ত্রাণ ও মানবসেবা",
      amountBdt: 500,
      memberName: "",
      dateLabel: "কাল",
      note: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Events: terminal status COMPLETED locks further non-admin updates; unverified admin email spoof rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("ev_1").set({
      userId: ALICE_UID,
      title: "শীতবস্ত্র বিতরণ",
      category: "সমাজসেবা ও ত্রাণ",
      eventDate: "২৫ নভেম্বর ২০২৬",
      venue: "মুকিয়াঘোনা মাদরাসা প্রাঙ্গণ",
      description: "দরিদ্র পরিবারের মাঝে কম্বল বিতরণ",
      status: "UPCOMING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Transition UPCOMING -> COMPLETED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("ev_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Subsequent non-admin edit on COMPLETED event fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("ev_1").update({
      title: "পরিবর্তিত",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Email spoofing test: unverified admin email must be rejected
  const spoofAdminDb = testEnv.authenticatedContext("attacker", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("users").doc(ALICE_UID).collection("events").doc("ev_1").get());

  // Verified admin can read & update
  const realAdminDb = testEnv.authenticatedContext("real_admin", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("users").doc(ALICE_UID).collection("events").doc("ev_1").get());
});

test("Notices: valid create & partial update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notices").doc("notice_1").set({
      userId: ALICE_UID,
      title: "মাসিক সাধারণ সভা",
      message: "আগামী শুক্রবার বাদ আসর মুকিয়াঘোনা জামে মসজিদ প্রাঙ্গণে সভা অনুষ্ঠিত হবে।",
      priority: "URGENT",
      authorName: "সাধারণ সম্পাদক",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notices").doc("notice_1").update({
      priority: "NORMAL",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
