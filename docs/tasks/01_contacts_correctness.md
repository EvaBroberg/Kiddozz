# Task 01: Lock Contacts Correctness

**Milestone:** 1  
**Status:** Pending  
**Owner:** TBD  
**Goal:** Ensure contact filtering is correct and stable before adding server-side messaging.

---

## Current State

- Contact tabs exist: Parents, Educators, Groups
- Parents tab: Shows only parents in `session.groupIds` (self excluded)
- Educators tab:
  - If educator: Shows all educators except self
  - If parent: Shows only educators in `session.groupIds`
- Session management: `session.role`, `session.userId`, `session.groupIds` are correctly derived from auth and data

---

## Steps

### 1. Verify Existing Tests

Check that unit tests cover the contact matrix:

- [ ] `MessagingRepositoryImplTest.kt`:
  - [ ] `parents_in_my_groups_only()` - Parent + Parents tab
  - [ ] `parent_educators_tab_shows_only_educators_from_parent_groups()` - Parent + Educators tab
  - [ ] `educator_parents_tab_shows_only_parents_from_educator_groups()` - Educator + Parents tab
  - [ ] `observeAllEducatorsExcept_returns_all_educators_except_self()` - Educator + Educators tab

- [ ] `MessagingViewModelTest.kt`:
  - [ ] `parent_parents_tab_shows_only_parents_from_parent_groups()` - Parent + Parents tab
  - [ ] `parent_educators_tab_shows_only_educators_from_parent_groups()` - Parent + Educators tab
  - [ ] `educator_parents_tab_shows_only_parents_from_educator_groups()` - Educator + Parents tab
  - [ ] `educator_educators_tab_shows_all_educators_except_self()` - Educator + Educators tab

### 2. Add Missing Test Cases

If any test cases are missing, add them:

```kotlin
// Example: Add test for parent viewing parents tab
@Test
fun parent_parents_tab_shows_only_parents_in_my_groups() = runTest {
    // GIVEN: Parent 10 in group 7
    // WHEN: View Parents tab
    // THEN: Should see only parents of kids in group 7 (excluding self)
}
```

### 3. Run Tests

```bash
# Android unit tests
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingRepositoryImplTest*"
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingViewModelTest*"
```

### 4. Manual Verification

1. **Login as Parent (id 10):**
   - Open Messages → Parents tab
   - Verify: Only parents sharing group 7 are shown (self excluded)
   - Open Messages → Educators tab
   - Verify: Only educators in group 7 are shown

2. **Login as Educator (id 27):**
   - Open Messages → Educators tab
   - Verify: All educators except 27 are shown
   - Open Messages → Parents tab
   - Verify: Only parents from educator's groups are shown

3. **Take Screenshots:**
   - Screenshot of Parents tab (as Parent 10)
   - Screenshot of Educators tab (as Parent 10)
   - Screenshot of Educators tab (as Educator 27)
   - Screenshot of Parents tab (as Educator 27)

---

## Acceptance Criteria

- [ ] All unit tests pass
- [ ] Manual verification confirms correct contact scoping
- [ ] Screenshots/documentation of UI scope
- [ ] No server messaging dependencies present
- [ ] No placeholder IDs in contact flows

---

## Files to Review

- `app/src/test/java/fi/kidozz/app/features/messaging/data/MessagingRepositoryImplTest.kt`
- `app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`

---

## Deliverable

- Green test suite
- Screenshots confirming UI scope
- Documentation of contact filtering behavior

