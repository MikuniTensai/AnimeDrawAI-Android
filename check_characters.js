const admin = require('firebase-admin');

admin.initializeApp({
    projectId: 'drawai-mobile-app'
});

const db = admin.firestore();

async function checkCharacters() {
    try {
        console.log('Fetching characters from Firestore...\n');

        const snapshot = await db.collection('characters').get();

        if (snapshot.empty) {
            console.log('No characters found in Firestore.');
            return;
        }

        console.log(`Found ${snapshot.size} characters:\n`);

        let deletedCount = 0;
        let activeCount = 0;

        snapshot.forEach(doc => {
            const data = doc.data();
            const isDeleted = data.isDeleted;

            if (isDeleted === true) {
                deletedCount++;
                console.log(`[DELETED] Character ID: ${doc.id}`);
            } else {
                activeCount++;
                console.log(`[ACTIVE] Character ID: ${doc.id}`);
            }

            console.log(`  Name: ${data.name || 'N/A'}`);
            console.log(`  UserId: ${data.userId || 'MISSING'}`);
            console.log(`  IsDeleted value: ${JSON.stringify(isDeleted)}`);
            console.log(`  IsDeleted type: ${typeof isDeleted}`);
            console.log('---');
        });

        console.log(`\nSummary:`);
        console.log(`  Active: ${activeCount}`);
        console.log(`  Deleted: ${deletedCount}`);
        console.log(`  Total: ${snapshot.size}`);

    } catch (error) {
        console.error('Error:', error.message);
    }

    process.exit(0);
}

checkCharacters();
