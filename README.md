# AnimeDrawAI - Native Android (Kotlin)

AnimeDrawAI is a powerful native Android application that combines artistic creativity with AI-driven character interactions. Built using modern Android development practices, it allows users to generate unique anime-style artwork and chat with dynamic AI personalities.

## 🚀 Key Features

- 🎨 **AI Image Generation**: Choose from over 40+ specialized workflows (Nova, Celestial, 3D, Cyberpunk, etc.) to create stunning anime and realistic art.
- 🤖 **Character AI Chat**: Interact with characters that have unique personalities, moods, and relationship progression.
- 📸 **AI Selfies**: Characters can generate and send "selfies" during chat interactions.
- 🖼️ **Community Gallery**: Explore, favorite, and share creations with the community.
- 🔐 **Secure Profiles**: Manage your account, subscriptions, and usage limits with Firebase integration.
- 💰 **Monetization**: Integrated Google AdMob and In-App Subscriptions for premium features.

## 🛠️ Technology Stack

- **Languange**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit + OkHttp
- **Local Storage**: DataStore
- **Images**: Coil (GIF support included)
- **Backend Services**: 
    - Firebase Auth (Authentication)
    - Firestore (Database)
    - Firebase Storage (Asset Hosting)
- **AI Integration**: Custom REST API integration for ComfyUI and Ollama.

## 📦 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 24 (Min SDK) / 35 (Target SDK)
- Firebase project configuration (`google-services.json`)

### Installation
1. Clone the repository.
2. Place your `google-services.json` in the `app/` directory.
3. Configure your API base URL in `DrawAIApiService.kt`.
4. Build and run on an Android device or emulator.

## 📝 License

Copyright © 2026 Nitedreamworks. All rights reserved.
