const admin = require('firebase-admin');
// Mocking initializeApp since we might not have credentials locally
const originalInit = admin.initializeApp;
admin.initializeApp = () => { console.log("Mock initializeApp called"); };
admin.firestore = () => ({
    batch: () => ({}),
    collection: () => ({ doc: () => ({}) })
});
admin.firestore.FieldValue = { serverTimestamp: () => "TIMESTAMP" };

try {
    console.log("Checking firebase-functions/v1 ...");
    const functionsV1 = require('firebase-functions/v1');
    if (functionsV1.pubsub && functionsV1.pubsub.schedule) {
        console.log("SUCCESS: functionsV1.pubsub.schedule exists!");
    } else {
        console.log("FAIL: functionsV1.pubsub.schedule MISSING");
        console.log("Keys:", Object.keys(functionsV1));
    }

    console.log("Attempting to require index.js...");
    const index = require('./index.js');
    console.log("Successfully required index.js");
} catch (error) {
    console.error("Error:", error);
}
