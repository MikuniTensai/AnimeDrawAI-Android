// Simple script to seed Vision data using Firebase Admin SDK
// This script will work if you have Firebase Admin SDK configured

const admin = require("firebase-admin");

// Try to initialize with application default credentials
// This works if you're authenticated with Firebase CLI
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
  console.log("✅ Firebase Admin initialized successfully");
} catch (error) {
  console.log("❌ Failed to initialize Firebase Admin:", error.message);
  console.log("📝 Make sure you're authenticated with Firebase CLI");
  console.log("   Run: firebase login");
  process.exit(1);
}

const db = admin.firestore();

// Vision data to seed
const visionsData = [
  {
    id: "vision_1",
    vision: "She taking a puff from her pipe and blowing out heart-shaped smoke with a smile",
    avoid: "blur, error, low quality, bad anatomy",
    category: "character",
    tags: ["character", "smoking", "heart", "smile"],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: "vision_2",
    vision: "A mystical forest with glowing mushrooms and floating crystals under moonlight",
    avoid: "cartoon, simple, flat colors, low resolution",
    category: "landscape",
    tags: ["forest", "mystical", "mushrooms", "crystals", "moonlight"],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: "vision_3",
    vision: "Cyberpunk city skyline with neon lights reflecting on wet streets at night",
    avoid: "daytime, rural, natural, soft lighting",
    category: "cityscape",
    tags: ["cyberpunk", "city", "neon", "night", "wet streets"],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: "vision_4",
    vision: "Ancient temple ruins overgrown with bioluminescent plants and vines",
    avoid: "modern, clean, sterile, artificial lighting",
    category: "ruins",
    tags: ["temple", "ruins", "bioluminescent", "plants", "ancient"],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: "vision_5",
    vision: "Steampunk airship floating through clouds with brass gears and steam engines",
    avoid: "contemporary, plastic, digital, minimalist",
    category: "vehicle",
    tags: ["steampunk", "airship", "brass", "gears", "steam"],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }
];

async function seedVisions() {
  try {
    console.log("🚀 Starting vision seeding process...");
    
    const batch = db.batch();
    let addedCount = 0;
    
    for (const visionData of visionsData) {
      const docRef = db.collection('visions').doc(visionData.id);
      
      // Check if document already exists
      const doc = await docRef.get();
      if (!doc.exists) {
        batch.set(docRef, visionData);
        addedCount++;
        console.log(`✅ Adding vision: ${visionData.id}`);
      } else {
        console.log(`⏭️  Vision ${visionData.id} already exists, skipping...`);
      }
    }

    // Commit batch if there are new documents
    if (addedCount > 0) {
      await batch.commit();
      console.log(`✅ Successfully added ${addedCount} visions`);
    } else {
      console.log("ℹ️  No new visions to add");
    }

    console.log(`🎉 Vision seeding completed!`);
    console.log(`📊 Total visions in collection: ${visionsData.length}`);
    console.log(`🆕 New visions added: ${addedCount}`);
    
  } catch (error) {
    console.error("❌ Error seeding visions:", error);
    process.exit(1);
  }
}

// Run the seeding
seedVisions().then(() => {
  console.log("🏁 Script completed successfully!");
  process.exit(0);
}).catch((error) => {
  console.error("💥 Script failed:", error);
  process.exit(1);
});