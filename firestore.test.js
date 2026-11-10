const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's private transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob_1").set({
      userId: BOB_UID,
      description: "Conta de Luz",
      type: "EXPENSE",
      category: "Moradia",
      amount: 150.0,
      isFixed: true,
      dueDate: "2026-11-20",
      date: "2026-11-08",
      isPaid: false,
      isBankSynced: false,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob_1").get());
});

test("Authenticated user: can create and read their own transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_alice_1");

  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      description: "Salário Mensal",
      type: "INCOME",
      category: "Salário",
      amount: 5000.0,
      isFixed: true,
      dueDate: "2026-11-05",
      date: "2026-11-05",
      isPaid: true,
      isBankSynced: false,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(txRef.get());
});

test("Authenticated user: can create and read their own savings goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1");

  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "Reserva de Emergência",
      targetAmount: 10000.0,
      currentAmount: 2500.0,
      deadline: "2026-12-31",
      category: "Poupança",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(goalRef.get());
});

// --- MULTI-USER WORKSPACE & AUDIT LOG TESTS ---

test("Multi-user: Alice creates shared workspace and invites Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const wsRef = aliceDb.collection("workspaces").doc("ws_almeida");

  await assertSucceeds(
    wsRef.set({
      name: "Família Almeida",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      inviteCode: "ALM777",
      createdAt: new Date(),
    })
  );
});

test("Multi-user: Both Alice and Bob can see shared transactions in workspace, but Charlie cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("workspaces").doc("ws_almeida").set({
      name: "Família Almeida",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      inviteCode: "ALM777",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const sharedTxRefAlice = aliceDb.collection("workspaces").doc("ws_almeida").collection("transactions").doc("tx_shared_1");
  const sharedTxRefBob = bobDb.collection("workspaces").doc("ws_almeida").collection("transactions").doc("tx_shared_1");
  const sharedTxRefCharlie = charlieDb.collection("workspaces").doc("ws_almeida").collection("transactions").doc("tx_shared_1");

  // Alice creates transaction in shared workspace
  await assertSucceeds(
    sharedTxRefAlice.set({
      userId: ALICE_UID,
      workspaceId: "ws_almeida",
      authorName: "Alice Almeida",
      description: "Supermercado Familiar",
      type: "EXPENSE",
      category: "Alimentação",
      amount: 450.0,
      isFixed: false,
      dueDate: "2026-11-10",
      date: "2026-11-08",
      isPaid: true,
      isBankSynced: false,
      createdAt: new Date(),
    })
  );

  // Bob can read it!
  await assertSucceeds(sharedTxRefBob.get());

  // Charlie (stranger) is blocked!
  await assertFails(sharedTxRefCharlie.get());
});

test("Audit Log: Immutable history cannot be modified or deleted", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("workspaces").doc("ws_almeida").set({
      name: "Família Almeida",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      inviteCode: "ALM777",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("workspaces").doc("ws_almeida").collection("audit_logs").doc("log_1");

  // Alice creates audit entry
  await assertSucceeds(
    logRef.set({
      workspaceId: "ws_almeida",
      userId: ALICE_UID,
      userName: "Alice Almeida",
      userEmail: "alice@almeida.com",
      action: "CRIOU",
      entityType: "TRANSAÇÃO",
      entityTitle: "Supermercado Familiar",
      details: "R$ 450,00 - Alimentação",
      createdAt: new Date(),
    })
  );

  // Attempt to tamper with / update audit log fails!
  await assertFails(
    logRef.update({
      details: "R$ 10,00 alterado",
    })
  );

  // Attempt to delete audit log fails!
  await assertFails(logRef.delete());
});
