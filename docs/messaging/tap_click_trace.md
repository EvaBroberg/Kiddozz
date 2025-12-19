# Tap Click Trace Report

**Date:** 2024-11-05  
**Issue:** Tap does nothing - chat window doesn't open  
**Purpose:** Trace the click handler flow to identify where navigation fails

---

## Symptom

User taps a contact in MessagesListScreen → nothing happens (no navigation, no error visible).

---

## Trace Logs

### Instructions for Collection

1. Build and install LocalDebug variant: `./gradlew :app:assembleLocalDebug`
2. Open Messages screen
3. Tap one contact
4. In Logcat, filter by tags: `TapNav|RepoConv`
5. Copy the chronological log lines from the tap

### Expected Log Pattern

```
TapNav: CLICK contactId=<id>, name=<name>
TapNav: openDirectWith START contactId=<id>
RepoConv: createOrGetDirectConversation contactId=<id>
RepoConv: existingId=<id or null>
RepoConv: RETURN conversationId=<uuid> blank=false
TapNav: openDirectWith OK conversationId=<uuid> blank=false
TapNav: NAVIGATE attempt conversationId=<uuid>
```

---

## Conclusion

Based on the trace logs, identify which scenario occurred:

### A) openDirectWith threw (Exception)
- **Evidence:** `TapNav: openDirectWith ERROR contactId=...` appears
- **Next Step:** Check exception type and stack trace to identify root cause (DAO error, Room constraint violation, etc.)

### B) openDirectWith returned blank conversationId
- **Evidence:** `TapNav: openDirectWith OK conversationId= blank=true`
- **Next Step:** Check `RepoConv: RETURN conversationId= blank=true` - repository should never return blank (this would indicate a bug in the repository hardening)

### C) openDirectWith OK but navigation skipped
- **Evidence:** `TapNav: openDirectWith OK conversationId=<uuid> blank=false` followed by `TapNav: NAVIGATE SKIPPED reason=blankIdOrGuard`
- **Next Step:** This should not happen if repository is hardened correctly - investigate why blank check fails despite non-blank ID

### D) Navigation attempted but screen didn't open
- **Evidence:** `TapNav: NAVIGATE attempt conversationId=<uuid>` appears, but ConversationScreen never opens
- **Next Step:** Check navigation route/argument mismatch:
  - Verify `MessagingRoutes.conversation(id)` matches nav graph pattern
  - Check argument name: `"conversationId"` (case-sensitive)
  - Verify `MainActivity.kt` route extraction: `backStackEntry.arguments?.getString("conversationId")`

---

## Log Locations

- **TapNav logs:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt:120-141`
- **RepoConv logs:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:244-300`

---

## Next Steps

After identifying the scenario (A, B, C, or D), apply targeted fix:
- **Scenario A:** Handle exception gracefully, show user-visible error
- **Scenario B:** Fix repository to never return blank (should be impossible after hardening)
- **Scenario C:** Fix blank-ID guard logic (should not trigger on valid UUIDs)
- **Scenario D:** Fix navigation route/argument alignment

---

## Collected Logs

_Paste the actual logcat output here after running the app and tapping a contact:_

```
[Logs will be added here after manual testing]
```





