# Cosmic Forge POS

[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

A modular, offline-first Point of Sale system designed for Myanmar's challenging infrastructure. Built with Jetpack Compose, WiFi Direct P2P networking, and encrypted local persistence.

## 🌟 Features

- **Offline-First**: Works without internet connectivity
- **P2P Mesh Networking**: WiFi Direct synchronization across 8+ devices
- **Modular Architecture**: Lego-style feature activation
- **RBAC**: Role-based access control (Owner, Manager, Waiter, Chief)
- **Chief Accountability**: Track preparation time and performance
- **Encrypted Storage**: SQLCipher database encryption
- **Myanmar Support**: Zawgyi & Unicode font support
- **Payment Integration**: Cash, KPay, CB Pay

## 🏗️ Architecture

- **UI**: Jetpack Compose
- **DI**: Hilt
- **Database**: Room + SQLCipher
- **Networking**: WiFi Direct + NSD fallback
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)

## 📦 Modules

| Bundle | Modules | Target Market |
|--------|---------|---------------|
| Starter | Core + Parcel + KPay | Kiosks, Ice Cream Shops |
| Growth | Core + Floor Map + CRM | Standard Restaurants |
| Elite | Everything + Reservations + Chief Audit | Hotels, High-End Dining |

## 🚀 Development Status

### ✅ Phase 1 Complete
- Project foundation and architecture
- Database schema with 6 entities
- Hilt dependency injection
- SQLCipher encryption
- Jetpack Compose UI foundation

### 🚧 Phase 2 In Progress
- WiFi Direct P2P networking
- NSD fallback for local network discovery
- Real-time sync protocol

## 🛠️ Tech Stack

- **Kotlin** 1.9.22
- **Jetpack Compose** 1.6.0
- **Room** 2.6.1
- **Hilt** 2.50
- **SQLCipher** 4.5.4
- **Coroutines** 1.7.3

## 📱 Installation

1. Clone the repository
```bash
git clone git@github.com:myoaung/RestaurantPOSByCosmicForge.git
cd RestaurantPOSByCosmicForge
```

2. Open in Android Studio Hedgehog or later

3. Build and run on Android tablet (landscape mode recommended)

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 📄 License

Proprietary - All rights reserved

## 👨‍💻 Author

Myo Aung

---

**Built for Myanmar. Built to Last.** 🇲🇲
