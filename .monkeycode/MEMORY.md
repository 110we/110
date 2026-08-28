# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.
- This helps avoid redundant entries and keeps the memory file tidy.

## Entries

[Project Knowledge Summary]
- Date: 2026-08-28
- Context: Discovered by Agent while implementing History/TaskBackup/Shizuku features for CrawlerApp
- Category: Build Methods
- Instructions:
  - The environment has no JDK and no Android SDK (`ANDROID_HOME` empty), so Gradle cannot compile; all verification must be static code review. Compile externally with `./gradlew assembleDebug`.
  - The app module is `:app` (rootProject.name = "CrawlerApp"), Kotlin 2.0 / AGP 8.5.0 / KGP 1.9.24, minSdk 26, targetSdk 34, JDK 17.
  - kotlinx-serialization is used in reflective mode (`CrawlTaskEntity` has no `@Serializable`); use `Json.decodeFromString(typeOf<List<...>>(), json)` style.
  - Room database uses `fallbackToDestructiveMigration()` on version upgrades.
  - Many existing UI files wrongly referenced `CrawlerTheme.colorScheme` (CrawlerTheme is a composable function, not an object); all 8 files were migrated to `MaterialTheme.colorScheme` (2026-08-28). Avoid reintroducing `CrawlerTheme.colorScheme`.
  - Shizuku integration uses `dev.rikka.shizuku:api/provider:13.1.5`; Shizuku package name is `moe.shizuku.privileged.api`; provider authorities `${applicationId}.shizuku`.
