# AGENTS

This document describes how the **Codex** agent is used in this project.

Project: **Android Live Wallpaper App**  
Language: **Kotlin**  
Primary Agent: **Codex (AI development agent)**

---

## 1. Agent: Codex

### 1.1. Name
**Codex**

### 1.2. Role
Codex is a **general-purpose development agent** embedded in the workflow of this Android live wallpaper app.  
It assists across the **entire development lifecycle**:

- Architecture & design
- Kotlin implementation
- Wallpaper engine and rendering logic
- Build & configuration (Gradle)
- Testing & QA
- Documentation
- Release preparation

Codex does **not** run in production on end-user devices. It is a **development-time assistant only**.

---

## 2. Responsibilities

### 2.1. Architecture & Design
- Propose high-level architecture for the app (modules, packages, separation of concerns).
- Suggest patterns (e.g. MVVM, clean architecture) adapted to:
  - A **WallpaperService**-based engine
  - A **settings / configuration UI** (Activity/Fragment/Compose screens)
- Help model data structures for:
  - Wallpaper configuration
  - Animation state
  - User preferences

### 2.2. UI & UX (Settings / Preview)

The live wallpaper itself is rendered by the wallpaper engine, but the app exposes a UI for:

- Wallpaper settings
- Live preview
- Onboarding / help screens

Codex helps to:

- Design and implement UI using **Jetpack Compose** (recommended) or classic views.
- Apply **Material Design** patterns:
  - Theming (light/dark)
  - Typography and color system
  - Standard components (buttons, switches, sliders, dialogs)
- Create a **preview screen** that shows how the wallpaper will roughly look with current settings.
- Implement form validation, state handling, and navigation between screens.

### 2.3. Wallpaper Engine & Rendering Logic

The core of the project is the live wallpaper:

- Implement a class extending `android.service.wallpaper.WallpaperService`.
- Implement `WallpaperService.Engine` to:
  - Handle lifecycle callbacks:
    - `onCreate()`
    - `onVisibilityChanged()`
    - `onSurfaceCreated()`
    - `onSurfaceChanged()`
    - `onSurfaceDestroyed()`
  - Manage the drawing loop (e.g. via `Handler`, `Choreographer`, or coroutines).
  - Render content using:
    - **Canvas** APIs (2D drawing), and/or  
    - **OpenGL / OpenGL ES** for more complex animations if needed.
- Use user preferences (colors, speed, density, etc.) to parameterize the wallpaper.
- Optimize for:
  - **Battery life** (pause when not visible, reduce frame rate, avoid unnecessary allocations).
  - **Performance** (avoid UI thread blocking, use efficient data structures).

Codex assists by:

- Writing the engine boilerplate.
- Proposing animation loops and rendering strategies.
- Managing touch events or sensor data (if the wallpaper reacts to interactions / accelerometer, etc.).
- Ensuring correct resource cleanup.

### 2.4. Data & Persistence

Codex helps define how the app stores configuration and state, using:

- **DataStore** (recommended) or SharedPreferences to store:
  - User-selected options (colors, animation speed, patterns, etc.).
  - Whether onboarding/tutorial has been completed.
- Serialization / deserialization of settings (e.g. using Kotlin `data class`es).

Responsibilities:

- Generate Kotlin models for preferences.
- Implement repository classes / use cases to read/write preferences.
- Ensure the wallpaper engine reacts correctly when preferences change.

### 2.5. Build Configuration (Gradle)

Codex manages and improves build configuration:

- Edit **Gradle** scripts (Kotlin DSL or Groovy) for:
  - `compileSdk`, `minSdk`, `targetSdk` versions.
  - Enabling **Jetpack Compose**.
  - Adding dependencies (AndroidX, Compose, testing libraries).
- Configure:
  - Build types: `debug`, `release`.
  - Signing configs for release.
  - ProGuard / R8 rules if needed.

Example Gradle concerns Codex handles:

- Setting up the Compose BOM.
- Applying and configuring `com.android.application` and `org.jetbrains.kotlin.android` plugins.
- Keeping dependencies consistent and updated.

### 2.6. Testing & Quality

Codex supports **test-first** and **test-later** approaches:

- **Unit tests** (JUnit) for:
  - Pure Kotlin logic (animation computations, configuration merging, etc.).
- **Instrumented tests** (AndroidX Test, Espresso) for:
  - Settings UI flow.
  - Onboarding and configuration screens.
- Optionally **snapshot tests** for UI if integrated with the toolchain.

Additional quality tasks:

- Suggest lint fixes and refactorings.
- Help enforce code style and structure.
- Propose property- and state-based tests for critical logic.

### 2.7. Documentation

Codex contributes to documentation:

- **KDoc** for key classes and functions:
  - Wallpaper engine
  - Rendering loop
  - Settings handling
- Project-level docs in Markdown:
  - `README.md` (project purpose, setup, running, building).
  - `AGENTS.md` (this file).
  - Additional docs (e.g., `ARCHITECTURE.md`) as needed.

Goals:

- Make the codebase self-explanatory for future maintainers.
- Provide onboarding documentation for new contributors.

### 2.8. Release & Deployment

Codex helps prepare the app for distribution:

- Configure Gradle tasks for:
  - Signed APK / AAB generation.
  - Versioning (version code / version name).
- Propose CI/CD pipeline scripts (GitHub Actions, GitLab CI, etc.) to:
  - Build and test on each commit.
  - Optionally automate release builds.

Human developers still:

- Own the keystore and signing keys.
- Approve and push releases to the Play Store or other channels.

---

## 3. Tools & Technologies

Codex is expected to use and respect the following tech stack.

### 3.1. Languages & Runtime

- **Kotlin** (primary language)
- Android SDK

### 3.2. Android & UI

- **AndroidX** libraries (AppCompat, Core KTX, Lifecycle, etc.)
- **Jetpack Compose** for settings UI:
  - `androidx.compose.ui`
  - `androidx.compose.material` / `material3`
  - `androidx.compose.runtime`
- Optionally Views for legacy or specific needs.

### 3.3. Wallpaper APIs

- `android.service.wallpaper.WallpaperService`
- `WallpaperService.Engine`
- `SurfaceHolder`, `Canvas`, and/or OpenGL ES for rendering.

### 3.4. Persistence

- **DataStore** (preferences)
- Optionally `SharedPreferences` for simpler usage or migration.

### 3.5. Build & Dependencies

- **Gradle** (Android Gradle Plugin)
- Kotlin Gradle plugin
- Dependency management via Gradle (Compose BOM, AndroidX, test libs).

### 3.6. Testing

- **JUnit** for unit tests.
- **AndroidX Test / Espresso** for UI tests.
- Any additional frameworks approved by the team (e.g., MockK, Robolectric).

---

## 4. Operational Scope & Rules

### 4.1. Scope

Codex can:

- Read and write all files in the repository (Kotlin, Gradle, Markdown, etc.).
- Propose and refactor code, configuration, and documentation.
- Suggest new modules, packages, or files when justified.

Codex **cannot**:

- Directly push to production or publish releases.
- Modify external systems without explicit developer intent.

### 4.2. Principles

Codex should:

- Prefer **idiomatic Kotlin** and **Android best practices**.
- Prioritize:
  - Performance
  - Battery efficiency
  - Maintainability
- Respect project style conventions (naming, formatting).
- Avoid introducing large or unnecessary dependencies without clear benefit.

### 4.3. Human Oversight

- All Codex changes are **reviewed by a human** before merging.
- Critical flows (billing, privacy, permissions) require extra scrutiny.
- Codex can propose, but must not bypass security or privacy standards.

---

## 5. Example Prompts for Codex

To keep usage consistent, here are typical prompts developers might use:

- **Architecture:**
  > “Codex, propose a package structure for an Android live wallpaper app in Kotlin using WallpaperService and a Compose-based settings screen.”

- **Wallpaper Engine:**
  > “Codex, generate a Kotlin class extending WallpaperService with an inner Engine that draws a simple animated gradient using Canvas at 30 FPS.”

- **Settings UI:**
  > “Codex, create a Compose screen allowing the user to select primary color, animation speed, and density for the live wallpaper. Persist settings with DataStore.”

- **Gradle Config:**
  > “Codex, update the Gradle build scripts to enable Jetpack Compose, set compileSdk to 35, and add dependencies for Compose Material and JUnit.”

- **Testing:**
  > “Codex, write unit tests for the function that computes the next animation frame based on time delta and user settings.”

---

This `AGENTS.md` defines how **Codex** participates in building and maintaining the **Kotlin-based Android Live Wallpaper app**.  
All contributors should follow these guidelines when interacting with or relying on the agent.
