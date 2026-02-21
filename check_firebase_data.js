// Script untuk cek data visions di Firebase
const admin = require("firebase-admin");

try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
  console.log("✅ Firebase Admin initialized successfully");
} catch (error) {
  console.log("❌ Failed to initialize Firebase Admin:", error.message);
  process.exit(1);
}

const db = admin.firestore();

async function checkVisions() {
  try {
    console.log("🔍 Checking visions collection...");
    const snapshot = await db.collection('visions').get();
    
    console.log(`📊 Found ${snapshot.size} documents in visions collection:`);
    
    if (snapshot.empty) {
      console.log("❌ No documents found in visions collection");
    } else {
      snapshot.forEach((doc) => {
        console.log(`\n📄 Document ID: ${doc.id}`);
        console.log(`   Vision: ${doc.data().vision?.substring(0, 50)}...`);
        console.log(`   Category: ${doc.data().category}`);
        console.log(`   Tags: ${doc.data().tags?.join(', ')}`);
      });
    }
    
    console.log("\n✅ Check completed successfully!");
    
  } catch (error) {
    console.error("❌ Error checking visions:", error);
    process.exit(1);
  }
}

checkVisions().then(() => {
  process.exit(0);
}).catch((error) => {
  console.error("💥 Script failed:", error);
  process.exit(1);
});