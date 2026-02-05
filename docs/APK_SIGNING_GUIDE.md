# 🔐 APK RELEASE SIGNING GUIDE

## Step 1: Generate Production Keystore

### Create New Keystore
```bash
keytool -genkey -v -keystore cosmic-forge-release.keystore \
  -alias cosmic-forge \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Keystore Information Prompts
```
Enter keystore password: [CREATE STRONG PASSWORD]
Re-enter new password: [CONFIRM PASSWORD]

What is your first and last name?
  [Enter]: Cosmic Forge Development Team

What is the name of your organizational unit?
  [Enter]: Mobile Development

What is the name of your organization?
  [Enter]: Cosmic Forge

What is the name of your City or Locality?
  [Enter]: Yangon

What is the name of your State or Province?
  [Enter]: Yangon Region

What is the two-letter country code for this unit?
  [Enter]: MM

Is CN=Cosmic Forge Development Team, OU=Mobile Development, O=Cosmic Forge, L=Yangon, ST=Yangon Region, C=MM correct?
  [Enter]: yes

Enter key password for <cosmic-forge>
  [RETURN if same as keystore password]
```

---

## Step 2: Sign the APK

### Using jarsigner (Traditional Method)
```bash
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore cosmic-forge-release.keystore \
  app-release-unsigned.apk \
  cosmic-forge
```

**Enter keystore password when prompted.**

---

### Using apksigner (Recommended - Android SDK Build Tools)
```bash
# Location: Android SDK/build-tools/[version]/apksigner

apksigner sign \
  --ks cosmic-forge-release.keystore \
  --ks-key-alias cosmic-forge \
  --out app-release-signed.apk \
  app-release-unsigned.apk
```

**Enter keystore password when prompted.**

---

## Step 3: Verify Signature

### Verify with jarsigner
```bash
jarsigner -verify -verbose -certs app-release-signed.apk
```

**Expected Output:**
```
jar verified.
```

### Verify with apksigner
```bash
apksigner verify --verbose app-release-signed.apk
```

**Expected Output:**
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

## Step 4: Optimize APK (Optional but Recommended)

### Zipalign for Performance
```bash
# Location: Android SDK/build-tools/[version]/zipalign

zipalign -v 4 app-release-signed.apk app-release-signed-aligned.apk
```

**Verify alignment:**
```bash
zipalign -c -v 4 app-release-signed-aligned.apk
```

---

## Complete Signing Workflow

### Full Command Sequence
```bash
# 1. Generate keystore (one-time only)
keytool -genkey -v -keystore cosmic-forge-release.keystore \
  -alias cosmic-forge -keyalg RSA -keysize 2048 -validity 10000

# 2. Sign APK
apksigner sign \
  --ks cosmic-forge-release.keystore \
  --ks-key-alias cosmic-forge \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# 3. Verify signature
apksigner verify --verbose app-release-signed.apk

# 4. Optimize (optional)
zipalign -v 4 app-release-signed.apk cosmic-forge-v9-production.apk

# 5. Final verification
zipalign -c -v 4 cosmic-forge-v9-production.apk
```

---

## Gradle Automation (Alternative)

### Add to app/build.gradle
```gradle
android {
    signingConfigs {
        release {
            storeFile file("../cosmic-forge-release.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias "cosmic-forge"
            keyPassword System.getenv("KEY_PASSWORD")
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

### Build Signed APK with Gradle
```bash
# Set environment variables
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"

# Build signed release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk (already signed)
```

---

## 🔒 KEYSTORE SECURITY BEST PRACTICES

### 1. Secure Storage
- ✅ **Store keystore in secure location** (NOT in git repository)
- ✅ **Backup keystore to encrypted USB drive**
- ✅ **Store backup in physical safe**
- ❌ **NEVER commit keystore to version control**

### 2. Password Management
- ✅ **Use strong passwords** (minimum 12 characters)
- ✅ **Store passwords in password manager** (1Password, LastPass)
- ✅ **Share passwords only with authorized personnel**
- ❌ **NEVER hardcode passwords in scripts**

### 3. Access Control
- ✅ **Limit keystore access to 2-3 people maximum**
- ✅ **Document who has access**
- ✅ **Rotate passwords annually**

### 4. Backup Strategy
- ✅ **Create 3 copies**: Local, USB drive, Cloud (encrypted)
- ✅ **Test backup restoration quarterly**
- ✅ **Document keystore details** (alias, validity, creation date)

---

## ⚠️ CRITICAL WARNING

**IF YOU LOSE THE KEYSTORE:**
- You CANNOT update the app on user devices
- You must publish a NEW app with a different package name
- Users must uninstall old app and install new app
- All existing installations become orphaned

**KEYSTORE LOSS = CATASTROPHIC FAILURE**

**Action:** Backup keystore immediately after creation!

---

## Keystore Information Record

**Document and store securely:**

```
Keystore Filename: cosmic-forge-release.keystore
Keystore Password: [STORED IN PASSWORD MANAGER]
Key Alias: cosmic-forge
Key Password: [STORED IN PASSWORD MANAGER]
Validity: 10,000 days (27 years)
Created: February 5, 2026
Algorithm: RSA 2048-bit
Signature: SHA256withRSA

Backup Locations:
1. Primary: [LOCATION]
2. USB Drive: [LOCATION]
3. Cloud (Encrypted): [LOCATION]

Authorized Access:
1. [NAME] - [ROLE]
2. [NAME] - [ROLE]
```

---

## Quick Reference Card

### Sign APK (One Command)
```bash
apksigner sign --ks cosmic-forge-release.keystore \
  --ks-key-alias cosmic-forge \
  --out cosmic-forge-v9-signed.apk \
  app-release-unsigned.apk
```

### Verify APK (One Command)
```bash
apksigner verify --verbose cosmic-forge-v9-signed.apk
```

---

**Last Updated:** February 5, 2026  
**For Support:** CosmicForge Development Team
