// Script untuk mengisi data vision ke Firestore
// Untuk menjalankan: node seed_visions.js

const { initializeApp } = require('firebase/app');
const { getFirestore, collection, addDoc } = require('firebase/firestore');

// Konfigurasi Firebase - akan diganti dengan konfigurasi project yang sebenarnya
const firebaseConfig = {
  // Konfigurasi ini akan diisi oleh user
};

// Data vision yang akan diisi
const visionsData = [
  {
    vision: "She taking a puff from her pipe and blowing out heart-shaped smoke with a smile",
    avoid: "blur, error, low quality, bad anatomy",
    category: "character",
    tags: ["character", "smoking", "heart", "smile"]
  },
  {
    vision: "A mystical forest with glowing mushrooms and floating crystals under moonlight",
    avoid: "cartoon, simple, flat colors, low resolution",
    category: "landscape",
    tags: ["forest", "mystical", "mushrooms", "crystals", "moonlight"]
  },
  {
    vision: "Cyberpunk city skyline with neon lights reflecting on wet streets at night",
    avoid: "daytime, rural, natural, soft lighting",
    category: "cityscape",
    tags: ["cyberpunk", "city", "neon", "night", "wet streets"]
  },
  {
    vision: "Ancient temple ruins overgrown with bioluminescent plants and vines",
    avoid: "modern, clean, sterile, artificial lighting",
    category: "ruins",
    tags: ["temple", "ruins", "bioluminescent", "plants", "ancient"]
  },
  {
    vision: "Steampunk airship floating through clouds with brass gears and steam engines",
    avoid: "contemporary, plastic, digital, minimalist",
    category: "vehicle",
    tags: ["steampunk", "airship", "brass", "gears", "steam"]
  }
];

async function seedVisions() {
  try {
    // Initialize Firebase
    const app = initializeApp(firebaseConfig);
    const db = getFirestore(app);

    console.log('Mulai mengisi data vision...');

    // Tambahkan setiap vision ke collection 'visions'
    for (const visionData of visionsData) {
      const docRef = await addDoc(collection(db, 'visions'), visionData);
      console.log(`Vision ditambahkan dengan ID: ${docRef.id}`);
    }

    console.log('Selesai! Semua vision berhasil ditambahkan.');
  } catch (error) {
    console.error('Error mengisi data:', error);
  }
}

// Jalankan script
console.log('Script seeding vision data');
console.log('Silakan update firebaseConfig dengan konfigurasi project Anda');
console.log('Untuk mendapatkan konfigurasi, kunjungi Firebase Console > Project Settings > General > Your apps > SDK setup and configuration');

// seedVisions(); // Uncomment untuk menjalankan