# Cosmic Forge POS 🍽️

**A Modular, "Lego-Style" Android POS System for Myanmar**

Built to thrive in challenging infrastructure with offline-first architecture, WiFi Direct P2P networking, and comprehensive accountability features.

---

## 🎯 Project Vision

Cosmic Forge POS is designed specifically for Myanmar's restaurant industry, addressing:
- **Unreliable Internet**: Offline-capable with optional cloud sync
- **Multi-Device Sync**: 8+ tablets synchronized via WiFi Direct P2P
- **Accountability**: 8-chief claim system, parcel fee audit, comprehensive logging
- **Myanmar Language**: Full Unicode support for SMS and UI
- **Modular Design**: "Lego-style" architecture for future expansion

---

## ✅ Development Phases (All Complete)

### Phase 1: Project Foundation & Architecture ✓
- Kotlin + Jetpack Compose UI
- Room Database with SQLCipher encryption
- Hilt dependency injection
- 6 core entities and DAOs
- **Commit:** `fc60827`

### Phase 2: P2P Mesh Networking ✓
- WiFi Direct for 8+ device P2P sync
- NSD fallback for extended range
- "Whisper" protocol (sub-second sync)
- Conflict resolution
- **Commit:** `fc60827`

### Phase 3: Authentication & RBAC ✓
- 4-level PIN authentication (Owner/Manager/Waiter/Chief)
- Role-based access control (RBAC)
- Security audit trail
- 30-minute session timeout
- Manager override tracking
- **Commit:** `8b27439`

### Phase 4: Waiter App & Floor Map ✓
- Real-time floor map with instant table locking
- Category-based menu ordering
- Parcel toggle with auto-fee calculation
- Cash/KPay/CBPay payment processing
- Payment proof camera capture
- **Commit:** `10f57b9`

### Phase 5: KDS, Accountability & Audit Dashboard ✓
- Flexible parcel fee override with audit
- KDS with 8-chief claim system
- Performance timers (claim → ready)
- Owner dashboard with sales analytics
- Parcel adjustment audit log
- End-of-day revenue reports
- **Commit:** `2df8e3a`

### Phase 6: Final Polish & Market Readiness ✓
- Myanmar Unicode SMS integration
- 80mm/58mm thermal receipt printing
- Daily cloud backup (when internet available)
- Stress test monitoring
- SMS template editor (Manager-only)
- **Commits:** `0f2adab`, `6747ba7`

---

## 📊 Database Schema (Version 4)

### Core Entities

**1. UserEntity** - Staff authentication
- 4 role levels (Owner=1, Manager=2, Waiter=3, Chief=4)
- SHA-256 PIN hashing
- Last login tracking

**2. TableEntity** - Floor management
- Status: FREE, OCCUPIED, DIRTY, RESERVED
- Floor grouping
- Real-time sync

**3. MenuItemEntity** - Product catalog
- Category-based organization
- Prep station assignment
- Myanmar language support

**4. OrderEntity** - Order headers
- Types: DINE_IN, PARCEL
- Payment methods: CASH, KPAY, CBPAY
- **Custom parcel fee** tracking
- Payment proof status

**5. OrderDetailEntity** - Order line items
- Chief claim tracking (8-chief accountability)
- Performance timers
- Status: PENDING, COOKING, READY

**6. SecurityAuditEntity** - Audit trail
- All sensitive operations logged
- Chief performance tracking
- Parcel fee adjustments

**7. SMSTemplateEntity** *(v4 new)*
- Customizable SMS templates
- Myanmar Unicode support
- Dynamic variable substitution

---

## 🏗️ Architecture Features

### Offline-First
- SQLite with SQLCipher encryption
- Local-first data persistence
- Optional cloud sync when available

### P2P Networking
- WiFi Direct (primary)
- NSD service discovery (fallback)
- Sub-second synchronization
- Handles 8+ devices simultaneously

### Security
- Database encryption (SQLCipher)
- PIN-based authentication
- Role-based access control
- Comprehensive audit logging
- Session timeout (30 min)

### Accountability
- **8-Chief System**: Every dish tracked to specific chef
- **Parcel Fee Audit**: All manual adjustments logged with waiter name
- **Performance Metrics**: Cook time tracking per chef
- **End-of-Day Reports**: Complete reconciliation

---

## 🌏 Myanmar-Specific Features

### SMS Integration
- Reservation confirmations
- Thank you messages
- Order ready notifications
- Myanmar Unicode support
- Customizable templates (Manager-only)

### Receipt Printing
- 80mm and 58mm thermal printer support
- Myanmar language thank you messages
- Parcel fee line with custom indicator
- Payment proof status display

### Language Support
- Myanmar Unicode throughout
- Category names in Myanmar
- Menu items bilingual ready
- SMS fully localized

---

## 🔧 Tech Stack

**Frontend:**
- Jetpack Compose (declarative UI)
- Material 3 design
- Hilt (dependency injection)

**Backend:**
- Room Database (ORM)
- SQLCipher (encryption)
- Coroutines + Flow (async)

**Networking:**
- WiFi Direct API
- NSD (Network Service Discovery)
- Custom "Whisper" sync protocol

**Security:**
- SHA-256 PIN hashing
- SQLCipher encryption
- RBAC with 4 levels
- Audit trail

**Additional:**
- Coil (image loading)
- Gson (JSON serialization)
- Android SMS Manager
- Thermal printer support

---

## 📱 Installation

### Requirements
- Android 8.0+ (API 26+)
- 3-8 tablets recommended
- WiFi router (optional, for NSD)
- 80mm or 58mm thermal printer
- SIM card (for SMS features)

### Build from Source
```bash
git clone git@github.com:myoaung/RestaurantPOSByCosmicForge.git
cd RestaurantPOSByCosmicForge/cosmic-forge-android
./gradlew assembleRelease
```

### Generate Signed APK
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Beta Testing Guide

### Setup (First Time)
1. Install APK on all tablets
2. Launch app on each device
3. Owner creates account (4-digit PIN)
4. Owner creates Manager/Waiter/Chief accounts
5. Configure tables (floor map)
6. Import menu items

### Daily Operations

**Morning:**
- Staff login with PIN
- Review previous day's report

**Service:**
- Waiters: Take orders on floor map
- Chiefs: Claim dishes in KDS
- Track cook times automatically

**Evening:**
- Owner: Generate end-of-day report
- Review parcel fee adjustments
- Check chief performance
- Cloud backup (if internet available)

### Stress Test Scenarios
- Rush hour: 50+ orders
- 8 devices P2P sync
- Monitor performance metrics
- Check slow operations log

---

## 📝 Key Workflows

### Order Flow
1. Waiter selects table → locks it
2. Adds items from category menu
3. Toggle parcel (auto-fee or custom)
4. Checkout → Choose payment
5. KPay/CBPay → Capture proof
6. Submit → Instant sync to all tablets

### KDS Flow
1. Chief sees pending orders
2. Filters by prep station
3. Claims dish → Timer starts
4. Cooks → Marks ready
5. Performance logged automatically

### Parcel Fee Override
1. Waiter toggles parcel (default 1,000 MMK)
2. Taps "Edit" icon
3. Enters custom amount
4. Sees audit warning
5. Confirms → Logged with waiter name
6. Owner sees in dashboard

### SMS Customization (Manager Only)
1. Manager opens Settings
2. SMS Template Editor (RBAC locked)
3. Edits Myanmar Unicode text
4. Uses variables: {shop_name}, {total}, etc.
5. Validates required variables
6. Saves → Used in automation

---

## 🔐 Security & Audit

### Access Levels
1. **Owner** (Level 1): Full access, license control
2. **Manager** (Level 2): Override permissions, SMS templates
3. **Waiter** (Level 3): Order entry, floor map
4. **Chief** (Level 4): KDS only

### Audit Trail
- All logins/logouts
- Manager overrides
- Void actions
- Price changes
- Parcel fee adjustments
- Chief performance
- Session timeouts

---

## 📊 Analytics & Reporting

### Owner Dashboard
- **Sales Summary**: Total, Cash, KPay, CBPay
- **Parcel Adjustments**: Manual fee changes audit
- **Chief Performance**: Dishes completed, avg cook time
- **Real-time**: Live updates across devices

### End-of-Day Report
```
═══ SALES SUMMARY ═══
Total: 450,000 MMK
  Cash: 200,000 | KPay: 150,000 | CBPay: 100,000

Orders: 45 (30 Dine-In, 15 Parcel)

═══ PARCEL FEES ═══
Total Adjustments Impact: +300 MMK
(3 manual overrides logged)
```

---

## 🛠️ Troubleshooting

### WiFi Direct Not Connecting
- Enable Location permissions
- Check WiFi is enabled
- Restart app on all devices
- Falls back to NSD automatically

### Database Migration Issues
- Uninstall and reinstall (test only!)
- Production: Migrations handled automatically

### SMS Not Sending
- Check SEND_SMS permission
- Verify Myanmar phone format (09X or +959X)
- Check SIM card active

---

## 🚧 Future Enhancements (Phase 7+)

- Module store (Lego-style activation)
- Inventory management
- Supplier integration
- Advanced analytics
- Multi-language UI
- Cloud POS synchronization
- Integration with delivery apps

---

## 📄 License

Cosmic Forge POS - Proprietary
© 2026 Cosmic Forge. All rights reserved.

Contact for licensing inquiries.

---

## 🙏 Acknowledgments

Built with passion for Myanmar's restaurant industry.

**Key Technologies:**
- Android Jetpack
- Room Database
- SQLCipher
- WiFi Direct
- Material Design 3

---

## 📞 Support

For beta testing support, please report issues via GitHub Issues.

**Version:** 1.0.0-beta  
**Database:** v4  
**Last Updated:** January 27, 2026  
**Status:** ✅ Ready for Internal Beta Testing
