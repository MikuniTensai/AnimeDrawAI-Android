const admin = require('firebase-admin');
// const serviceAccount = require('./service-account-key.json'); // Handled in try-catch

// Initialize Firebase Admin
try {
    const serviceAccount = require('./service-account-key.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
} catch (e) {
    console.log('Service account key not found, trying Application Default Credentials...');
    admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        projectId: 'drawai-mobile-app' // Replace with actual project ID if known, or let it auto-detect
    });
}

const db = admin.firestore();

async function seedNews() {
    console.log('Seeding news data...');

    const newsCollection = db.collection('news');

    // Check if data exists
    const snapshot = await newsCollection.limit(1).get();
    if (!snapshot.empty) {
        console.log('News data already exists. Skipping seed.');
        return;
    }

    const batch = db.batch();

    // Event Item
    const eventRef = newsCollection.doc();
    batch.set(eventRef, {
        type: 'event',
        title: 'Generate Like Contest',
        description: 'Create the most liked generation and win $1! Join the community contest now.',
        date: admin.firestore.Timestamp.now(),
        imageUrl: 'https://placehold.co/600x400/png',
        actionUrl: 'https://discord.gg/animedrawai'
    });

    // Update Item
    const updateRef = newsCollection.doc();
    batch.set(updateRef, {
        type: 'update',
        title: 'Patch 1.0.39',
        description: 'Build 39: Added new Event & Update screen, improved UI performance, and fixed minor bugs.',
        date: admin.firestore.Timestamp.now(),
        version: '1.0.39'
    });

    await batch.commit();
    console.log('Successfully seeded news data!');
}

seedNews().catch(console.error);
