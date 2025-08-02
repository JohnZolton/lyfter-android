# Lyfter - Workout Tracking App

vibe codedc with Kimi-K2 for about $2

A comprehensive Android workout tracking application built with Kotlin and Jetpack Compose.

## Features

- **Exercise Management**: Browse and manage a comprehensive list of exercises
- **Workout Planning**: Create and manage workout plans
- **Workout Tracking**: Track sets, reps, and weights during workouts
- **Progress Tracking**: Monitor your fitness progress over time
- **Offline Support**: Full offline functionality with local database

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Dependency Injection**: Manual (via Application class)
- **Navigation**: Navigation Component for Compose

## Project Structure

```
lyfter-app/
├── app/
│   ├── src/main/java/com/lyfter/
│   │   ├── MainActivity.kt          # Main entry point
│   │   ├── LyfterApplication.kt     # Application class for DI
│   │   ├── model/                   # Data models
│   │   ├── data/                    # Database entities and DAOs
│   │   ├── repository/              # Repository layer
│   │   ├── ui/                      # UI components
│   │   │   ├── screens/            # Composable screens
│   │   │   ├── theme/              # Material Design theme
│   │   │   └── navigation/         # Navigation setup
│   │   ├── viewmodel/              # ViewModels
│   │   └── utils/                  # Utility classes
│   └── src/main/res/               # Android resources
├── build.gradle.kts                 # Project-level build config
├── app/build.gradle.kts            # App-level build config
└── settings.gradle.kts             # Gradle settings
```

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 31 or later
- JDK 11 or later

### Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Run the app on an emulator or physical device

### Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Database Schema

The app uses Room database with the following entities:

- **Exercise**: Exercise definitions and metadata
- **Workout**: Individual workout sessions
- **WorkoutExercise**: Exercises within a workout
- **Set**: Individual sets within an exercise
- **WorkoutPlan**: Predefined workout plans
- **WorkoutPlanExercise**: Exercises within a workout plan

## Development

### Adding New Exercises

Exercises are initialized in `DatabaseInitializer.kt`. To add new exercises:

1. Add the exercise to the `getDefaultExercises()` function
2. The app will automatically populate the database on first launch

### Customization

- **Theme**: Modify colors in `ui/theme/Color.kt`
- **Typography**: Update fonts in `ui/theme/Type.kt`
- **Icons**: Replace drawables in `res/drawable/`

## Testing

Run unit tests:

```bash
./gradlew test
```

Run instrumented tests:

```bash
./gradlew connectedAndroidTest
```

## License

This project is open source and available under the MIT License.
# lyfter-android
