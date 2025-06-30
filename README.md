# SpeakEasy Progress Report

## Project Overview
SpeakEasy is a modern Android app designed to help users practice public speaking. It features a friendly, clean UI, level-based practice, recording, and history, using Material Design 3 and Jetpack libraries.

## Current Progress

### 1. Storyboards & Diagrams
- **User Flow Storyboard:** Created (see below)
- **UML/Class Diagram:** Created (see below)

### 2. Application Skeleton
- **Splash Screen:** Layout designed, matches branding and reference image
- **Login/Create Profile:** Layout complete, supports name entry and guest mode
- **Home Screen:** Layout planned, not yet implemented
- **Practice Screen:** Layout planned, not yet implemented
- **Recording History:** Layout planned, not yet implemented
- **Profile/Settings:** Dialog planned, not yet implemented

### 3. Resources
- **Colors, styles, and fonts:** Defined in `colors.xml`, `themes.xml`, and `res/font/`
- **Strings:** Centralized in `strings.xml`
- **Drawable assets:** Placeholders and some vector assets in place

### 4. Navigation
- Navigation flow mapped out, not yet implemented in code

### 5. Diagrams
- See attached Mermaid diagrams for user flow and class structure

## In Progress
- Hooking up navigation between activities
- Finalizing splash and login screen polish
- Preparing vector illustration for splash

## To Do
- Implement Home, Practice, and History screens
- Implement Room database for recordings
- Add ViewModels and Repository pattern
- Add Navigation Component XML graph
- Add audio recording functionality
- Polish UI/UX for all screens
- Add testing (unit and UI)

## Blockers/Issues
- Awaiting final illustration asset for splash screen
- Need to finalize prompt data for practice
- No major technical blockers at this stage

## Schedule for Completion
- **Splash & Login polish:** 1 day
- **Home/Practice/History layouts:** 2-3 days
- **Database & ViewModels:** 2 days
- **Navigation & logic:** 2 days
- **Audio recording & playback:** 2 days
- **Testing & polish:** 1-2 days
- **Estimated completion:** 7-10 days from now

## How to Run
1. Clone the repo and open in Android Studio
2. Sync Gradle
3. Add required assets (see `res/drawable/` for placeholders)
4. Run on emulator or device (minSdk 24)

## Notes
- All layouts use Material Design 3 and are responsive
- Fonts and colors are centralized for easy theming
- See diagrams below for architecture and flow

---

**User Flow Storyboard:**

```
(see Mermaid diagram in repo)
```

**UML/Class Diagram:**

```
(see Mermaid diagram in repo)
```
