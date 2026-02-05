# 🛡️ HARDWARE RECOVERY & SESSION PURGE COMMANDS

## Purpose
Clear stuck admin sessions on test devices and restore Profile Selection screen.

## ADB Session Purge Command

### Full App Data Wipe
```bash
adb shell pm clear com.cosmicforge.rms
```

**What This Does**:
- Clears all app data (database, preferences, cache)  
- Removes stuck session state
- Forces app to start fresh with Profile Selection screen
- **WARNING**: Deletes all local orders and data!

### Selective Session Clear (Alternative)
```bash
# Clear only SharedPreferences (preserves database)
adb shell run-as com.cosmicforge.rms rm -rf /data/data/com.cosmicforge.rms/shared_prefs
```

## Testing Checklist

### After Session Purge
1. Launch app via ADB or tablet UI
   ```bash
   adb shell am start -n com.cosmicforge.rms/.MainActivity
   ```
2. ✅ Verify "Select Your Profile" screen appears
3. ✅ Select a user (Manager/Waiter/Chief)
4. ✅ Enter PIN
5. ✅ Confirm role-based dashboard loads
6. ✅ Logout and verify return to Profile Selection

### Security Verification
- [ ] No auto-login on app restart
- [ ] PIN required for every session
- [ ] Role-based sidebar (Chiefs see only Kitchen)
- [ ] Admin panel requires Manager/Owner PIN

## Production Deployment Notes

**Before deploying v9.2**:
1. Test session purge on 1 tablet
2. Verify PIN authentication works
3. Test all 4 roles (Owner, Manager, Waiter, Chief)
4. Confirm logout returns to Profile Selection

**During deployment** (all 8 tablets):
1. Run `adb shell pm clear com.cosmicforge.rms` on each tablet
2. Install new APK
3. First launch → Profile Selection
4. Create user profiles for staff
5. Test authentication flow

## Emergency Recovery

If tablets get stuck after deployment:
```bash
# Connect via ADB
adb devices

# Clear app data
adb shell pm clear com.cosmicforge.rms

# Relaunch app
adb shell am start -n com.cosmicforge.rms/.MainActivity
```

---

**Created**: February 6, 2026  
**Status**: Ready for testing
