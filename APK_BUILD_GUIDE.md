# APK Generation Guide

## Signed Release APK for Beta Testing

### Prerequisites
1. Android Studio installed
2. JDK 11 or higher
3. Keystore for signing (create if needed)

### Step 1: Create Keystore (First Time Only)
```bash
keytool -genkey -v -keystore cosmic-forge-release.keystore -alias cosmic-forge -keyalg RSA -keysize 2048 -validity 10000
```

**Store these credentials securely:**
- Keystore password
- Key alias: `cosmic-forge`
- Key password

### Step 2: Configure Signing in build.gradle

Add to `app/build.gradle`:

```gradle
android {
    ...
    
    signingConfigs {
        release {
            storeFile file("path/to/cosmic-forge-release.keystore")
            storePassword "YOUR_KEYSTORE_PASSWORD"
            keyAlias "cosmic-forge"
            keyPassword "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Step 3: Build Release APK

**Option A: Command Line**
```bash
cd cosmic-forge-android
./gradlew assembleRelease
```

**Option B: Android Studio**
1. Build → Generate Signed Bundle / APK
2. Select APK
3. Choose keystore
4. Enter passwords
5. Select "release" variant
6. Click Finish

### Step 4: Locate APK
```
app/build/outputs/apk/release/app-release.apk
```

### Step 5: Test APK
```bash
# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk

# Or upload to Google Play Internal Testing
```

## Debug APK (Quick Testing - No Signing)
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## ProGuard Configuration
Ensure `proguard-rules.pro` has:
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.sqlcipher.** { *; }

# Gson
-keepattributes Signature
-keep class com.cosmicforge.pos.data.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
```

## Beta Distribution Checklist
- [ ] Sign with release keystore
- [ ] Test on multiple devices
- [ ] Verify database encryption
- [ ] Check WiFi Direct pairing
- [ ] Test all payment flows
- [ ] Verify Myanmar Unicode SMS
- [ ] Print test receipts
- [ ] Run stress test suite

## APK Size Optimization
Current estimate: ~12-15 MB (compressed)

**Included:**
- SQLCipher native libs
- Jetpack Compose runtime
- Room database
- Hilt DI
- Coil image loading

---

**For Internal Beta Only**  
Version: 1.0.0-beta  
Min SDK: 26 (Android 8.0)  
Target SDK: 34 (Android 14)
