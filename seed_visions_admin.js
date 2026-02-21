// Script untuk mengisi data vision ke Firestore menggunakan Admin SDK
// Lebih aman karena menggunakan service account

const admin = require('firebase-admin');

// Data vision yang akan diisi
const visionsData = [
  {
    id: 'vision_1',
    vision: 'She taking a puff from her pipe and blowing out heart-shaped smoke with a smile',
    avoid: 'blur, error, low quality, bad anatomy',
    category: 'character',
    tags: ['character', 'smoking', 'heart', 'smile'],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: 'vision_2',
    vision: 'A mystical forest with glowing mushrooms and floating crystals under moonlight',
    avoid: 'cartoon, simple, flat colors, low resolution',
    category: 'landscape',
    tags: ['forest', 'mystical', 'mushrooms', 'crystals', 'moonlight'],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: 'vision_3',
    vision: 'Cyberpunk city skyline with neon lights reflecting on wet streets at night',
    avoid: 'daytime, rural, natural, soft lighting',
    category: 'cityscape',
    tags: ['cyberpunk', 'city', 'neon', 'night', 'wet streets'],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: 'vision_4',
    vision: 'Ancient temple ruins overgrown with bioluminescent plants and vines',
    avoid: 'modern, clean, sterile, artificial lighting',
    category: 'ruins',
    tags: ['temple', 'ruins', 'bioluminescent', 'plants', 'ancient'],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  },
  {
    id: 'vision_5',
    vision: 'Steampunk airship floating through clouds with brass gears and steam engines',
    avoid: 'contemporary, plastic, digital, minimalist',
    category: 'vehicle',
    tags: ['steampunk', 'airship', 'brass', 'gears', 'steam'],
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }
];

async function seedVisions() {
  try {
    // Inisialisasi dengan service account (akan di-setup oleh user)
    console.log('Menginisialisasi Firebase Admin SDK...');
    console.log('Pastikan Anda telah mengatur GOOGLE_APPLICATION_CREDENTIALS environment variable');
    console.log('atau menyediakan service account key file');
    
    admin.initializeApp({
      credential: admin.credential.applicationDefault()
    });

    const db = admin.firestore();

    console.log('Mulai mengisi data vision...');

    // Batch write untuk efisiensi
    const batch = db.batch();
    
    for (const visionData of visionsData) {
      const docRef = db.collection('visions').doc(visionData.id);
      batch.set(docRef, visionData);
      console.log(`Menambahkan vision: ${visionData.id}`);
    }

    // Commit batch
    await batch.commit();
    console.log('Selesai! Semua vision berhasil ditambahkan.');
    
  } catch (error) {
    console.error('Error mengisi data:', error);
  } finally {
    // Cleanup
    if (admin.apps.length > 0) {
      await admin.app().delete();
    }
  }
}

// Panduan penggunaan
console.log('=== PANDUAN PENGGUNAAN ===');
console.log('1. Unduh service account key dari Firebase Console');
console.log('   Project Settings > Service Accounts > Generate new private key');
console.log('2. Simpan file JSON tersebut sebagai service-account-key.json');
console.log('3. Set environment variable:');
console.log('   export GOOGLE_APPLICATION_CREDENTIALS="service-account-key.json"');
console.log('4. Jalankan script: node seed_visions_admin.js');
console.log('');
console.log('Atau gunakan Firebase Console untuk import data secara manual');
console.log('');

// Uncomment baris di bawah untuk menjalankan
// seedVisions();