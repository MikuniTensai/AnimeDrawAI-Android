const functions = require("firebase-functions");
const functionsV1 = require("firebase-functions/v1");
const admin = require("firebase-admin");

// Lazy initialization helper to avoid runtime errors during deploy analysis
let dbInstance = null;
function getDB() {
  if (!dbInstance) {
    if (admin.apps.length === 0) {
      admin.initializeApp();
    }
    dbInstance = admin.firestore();
  }
  return dbInstance;
}

// Data vision yang akan diisi
const visionsData = [
  {
    id: "vision_1",
    vision: "She taking a puff from her pipe and blowing out heart-shaped" +
      " smoke with a smile",
    avoid: "blur, error, low quality, bad anatomy",
    category: "character",
    tags: ["character", "smoking", "heart", "smile"],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  },
  {
    id: "vision_2",
    vision: "A mystical forest with glowing mushrooms and floating" +
      " crystals under moonlight",
    avoid: "cartoon, simple, flat colors, low resolution",
    category: "landscape",
    tags: ["forest", "mystical", "mushrooms", "crystals", "moonlight"],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  },
  {
    id: "vision_3",
    vision: "Cyberpunk city skyline with neon lights reflecting on" +
      " wet streets at night",
    avoid: "daytime, rural, natural, soft lighting",
    category: "cityscape",
    tags: ["cyberpunk", "city", "neon", "night", "wet streets"],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  },
  {
    id: "vision_4",
    vision: "Ancient temple ruins overgrown with bioluminescent" +
      " plants and vines",
    avoid: "modern, clean, sterile, artificial lighting",
    category: "ruins",
    tags: ["temple", "ruins", "bioluminescent", "plants", "ancient"],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  },
  {
    id: "vision_5",
    vision: "Steampunk airship floating through clouds with brass" +
      " gears and steam engines",
    avoid: "contemporary, plastic, digital, minimalist",
    category: "vehicle",
    tags: ["steampunk", "airship", "brass", "gears", "steam"],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  },
];

/**
 * HTTP Cloud Function untuk seeding data vision ke Firestore
 * Dapat dipanggil dengan:
 * https://us-central1-drawai-mobile-app.cloudfunctions.net/seedVisions
 */
exports.seedVisions = functions.https.onRequest(async (req, res) => {
  // Set CORS headers
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  // Handle preflight requests
  if (req.method === "OPTIONS") {
    res.status(200).end();
    return;
  }

  try {
    console.log("Starting vision seeding process...");
    const db = getDB();

    const batch = db.batch();
    let addedCount = 0;

    for (const visionData of visionsData) {
      const docRef = db.collection("visions").doc(visionData.id);

      // Check if document already exists
      const doc = await docRef.get();
      if (!doc.exists) {
        batch.set(docRef, visionData);
        addedCount++;
        console.log(`Adding vision: ${visionData.id}`);
      } else {
        console.log(`Vision ${visionData.id} already exists, skipping...`);
      }
    }

    // Commit batch if there are new documents
    if (addedCount > 0) {
      await batch.commit();
      console.log(`Successfully added ${addedCount} visions`);
    }

    res.json({
      success: true,
      message: `Vision seeding completed. Added ${addedCount} new visions.`,
      totalVisions: visionsData.length,
      addedCount: addedCount,
    });
  } catch (error) {
    console.error("Error seeding visions:", error);
    res.status(500).json({
      success: false,
      error: error.message,
      message: "Failed to seed visions",
    });
  }
});

// ... autoSeedVisions commented out ...

/**
 * Cloud Function untuk membersihkan dan reset data visions
 */
exports.resetVisions = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");

  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  try {
    console.log("Resetting visions collection...");
    const db = getDB();

    const visionsCollection = db.collection("visions");
    const snapshot = await visionsCollection.get();

    const batch = db.batch();
    snapshot.docs.forEach((doc) => {
      batch.delete(doc.ref);
    });

    await batch.commit();

    console.log(`Deleted ${snapshot.size} vision documents`);

    res.json({
      success: true,
      message: `Successfully reset ${snapshot.size} visions`,
    });
  } catch (error) {
    console.error("Error resetting visions:", error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * Update Leaderboards (Call cron or manually)
 * Aggregates data for Top Creators
 */
/**
 * Logic untuk update leaderboards (bisa dipanggil manual atau scheduler)
 */
async function updateLeaderboardsLogic() {
  console.log("Starting leaderboard update logic...");
  const db = getDB();

  // --- 1. Top Creators (Based on totalGenerations in generation_limits) ---
  console.log("Updating Top Creators...");
  const limitsSnapshot = await db.collection("generation_limits")
    .orderBy("totalGenerations", "desc")
    .limit(50)
    .get();

  const topCreatorsEntries = [];

  for (const doc of limitsSnapshot.docs) {
    const data = doc.data();
    const userId = doc.id;
    const score = data.totalGenerations || 0;

    if (score > 0) {
      const userDoc = await db.collection("users").doc(userId).get();
      let userName = "Anonymous";
      let userPhoto = null;

      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.displayName || userData.email ||
          userData.name || "Anonymous";
        userPhoto = userData.photoUrl || userData.photoURL || null;
      }

      topCreatorsEntries.push({
        userId: userId,
        userName: userName,
        userPhoto: userPhoto,
        score: parseFloat(score),
      });
    }
  }

  // Write to leaderboards/top_creators
  await db.collection("leaderboards").doc("top_creators").set({
    entries: topCreatorsEntries,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  console.log(`Updated Top Creators: ${topCreatorsEntries.length}`);

  // --- 2. Top Romancers (Total Affection from all characters) ---
  console.log("Updating Top Romancers...");
  const charactersSnapshot = await db.collection("characters").get();

  const affectionByUser = {};
  charactersSnapshot.docs.forEach((doc) => {
    const data = doc.data();
    if (data.isDeleted === true) return;

    const userId = data.userId;
    const affection = data.relationship?.affectionPoints || 0;

    if (userId) {
      affectionByUser[userId] = (affectionByUser[userId] || 0) + affection;
    }
  });

  const topRomancersEntries = [];
  for (const [userId, totalAffection] of Object.entries(affectionByUser)) {
    if (totalAffection > 0) {
      const userDoc = await db.collection("users").doc(userId).get();
      let userName = "Anonymous";
      let userPhoto = null;

      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.displayName || userData.email ||
          userData.name || "Anonymous";
        userPhoto = userData.photoUrl || userData.photoURL || null;
      }

      topRomancersEntries.push({
        userId: userId,
        userName: userName,
        userPhoto: userPhoto,
        score: parseFloat(totalAffection),
      });
    }
  }

  topRomancersEntries.sort((a, b) => b.score - a.score);
  const top50Romancers = topRomancersEntries.slice(0, 50);

  await db.collection("leaderboards").doc("top_romancers").set({
    entries: top50Romancers,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  console.log(`Updated Top Romancers: ${top50Romancers.length}`);

  // --- 3. Community MVP (Total shared posts) ---
  console.log("Updating Community MVP...");
  const postsSnapshot = await db.collection("community_posts").get();

  const postsByUser = {};
  const userInfoByUser = {};

  postsSnapshot.docs.forEach((doc) => {
    const data = doc.data();
    const userId = data.userId;

    if (userId) {
      postsByUser[userId] = (postsByUser[userId] || 0) + 1;
      if (!userInfoByUser[userId]) {
        userInfoByUser[userId] = {
          username: data.username || "Anonymous",
          userPhotoUrl: data.userPhotoUrl || null,
        };
      }
    }
  });

  const communityMVPEntries = [];
  for (const [userId, postCount] of Object.entries(postsByUser)) {
    if (postCount > 0) {
      const userInfo = userInfoByUser[userId];
      communityMVPEntries.push({
        userId: userId,
        userName: userInfo.username,
        userPhoto: userInfo.userPhotoUrl,
        score: parseFloat(postCount),
      });
    }
  }

  communityMVPEntries.sort((a, b) => b.score - a.score);
  const top50MVP = communityMVPEntries.slice(0, 50);

  await db.collection("leaderboards").doc("community_mvp").set({
    entries: top50MVP,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  console.log(`Updated Community MVP: ${top50MVP.length}`);

  // --- 4. Hall of Fame (Likes/Downloads per Category) ---
  console.log("Updating Hall of Fame...");
  const categories = ["anime", "background", "animal", "flower",
    "food", "general"];

  for (const category of categories) {
    const likesByUser = {};
    const downloadsByUser = {};
    const userInfoByCategory = {};

    postsSnapshot.docs.forEach((doc) => {
      const data = doc.data();
      const workflow = (data.workflow || "").toLowerCase();
      const userId = data.userId;

      if (workflow.includes(category) && userId) {
        likesByUser[userId] = (likesByUser[userId] || 0) +
          (data.likes || 0);
        downloadsByUser[userId] = (downloadsByUser[userId] || 0) +
          (data.downloads || 0);

        if (!userInfoByCategory[userId]) {
          userInfoByCategory[userId] = {
            username: data.username || "Anonymous",
            userPhotoUrl: data.userPhotoUrl || null,
          };
        }
      }
    });

    // Likes
    const likesEntries = [];
    for (const [userId, totalLikes] of Object.entries(likesByUser)) {
      if (totalLikes > 0) {
        const userInfo = userInfoByCategory[userId];
        likesEntries.push({
          userId: userId,
          userName: userInfo.username,
          userPhoto: userInfo.userPhotoUrl,
          score: parseFloat(totalLikes),
        });
      }
    }
    likesEntries.sort((a, b) => b.score - a.score);
    const top50Likes = likesEntries.slice(0, 50);

    await db.collection("leaderboards").doc(`likes_${category}`).set({
      entries: top50Likes,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Downloads
    const downloadsEntries = [];
    for (const [userId, totalDls] of Object.entries(downloadsByUser)) {
      if (totalDls > 0) {
        const userInfo = userInfoByCategory[userId];
        downloadsEntries.push({
          userId: userId,
          userName: userInfo.username,
          userPhoto: userInfo.userPhotoUrl,
          score: parseFloat(totalDls),
        });
      }
    }
    downloadsEntries.sort((a, b) => b.score - a.score);
    const top50Downloads = downloadsEntries.slice(0, 50);

    await db.collection("leaderboards")
      .doc(`downloads_${category}`).set({
        entries: top50Downloads,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
  }

  // --- 5. Rising Stars (Recent likes in last 7 days) ---
  console.log("Updating Rising Stars...");
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  const recentLikesByUser = {};
  postsSnapshot.docs.forEach((doc) => {
    const data = doc.data();
    const createdAt = data.createdAt?.toDate();
    const userId = data.userId;

    if (createdAt && createdAt >= sevenDaysAgo && userId) {
      recentLikesByUser[userId] = (recentLikesByUser[userId] || 0) +
        (data.likes || 0);
    }
  });

  const risingStarsEntries = [];
  for (const [userId, recentLikes] of Object.entries(recentLikesByUser)) {
    if (recentLikes > 0) {
      const userDoc = await db.collection("users").doc(userId).get();
      let userName = "Anonymous";
      let userPhoto = null;

      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.displayName || userData.email ||
          userData.name || "Anonymous";
        userPhoto = userData.photoUrl || userData.photoURL || null;
      }

      risingStarsEntries.push({
        userId: userId,
        userName: userName,
        userPhoto: userPhoto,
        score: parseFloat(recentLikes),
      });
    }
  }

  risingStarsEntries.sort((a, b) => b.score - a.score);
  const top50RisingStars = risingStarsEntries.slice(0, 50);

  await db.collection("leaderboards").doc("rising_stars").set({
    entries: top50RisingStars,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  console.log(`Updated Rising Stars: ${top50RisingStars.length}`);

  return {
    topCreators: topCreatorsEntries.length,
    topRomancers: top50Romancers.length,
    communityMVP: top50MVP.length,
    risingStars: top50RisingStars.length,
    categoriesProcessed: categories.length,
  };
}

/**
 * Update Leaderboards (Manual HTTP Trigger)
 */
exports.updateLeaderboards = functions.https.onRequest(async (req, res) => {
  try {
    const stats = await updateLeaderboardsLogic();
    res.json({
      success: true,
      message: "Leaderboards updated successfully (Manual)",
      stats: stats,
    });
  } catch (error) {
    console.error("Error updating leaderboards:", error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * Scheduled Leaderboard Update (Runs every 60 minutes)
 */
exports.scheduledLeaderboardUpdate = functionsV1.pubsub
  .schedule("every 60 minutes")
  .onRun(async (context) => {
    try {
      console.log("Running scheduled leaderboard update...");
      const stats = await updateLeaderboardsLogic();
      console.log("Scheduled update completed successfully:", stats);
      return null;
    } catch (error) {
      console.error("Error in scheduled leaderboard update:", error);
      return null;
    }
  });
