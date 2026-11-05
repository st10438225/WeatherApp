**Module: OPSC6312 POE**
**Github link:** https://github.com/st10438225/WeatherApp.git

**WeatherApp**
is a fully functional Android application that provides real-time weather updates, 5-day forecasts, and local event discovery based on the user's location. 
Built for the final POE submission, the app utilizes modern Android architecture,
including MVVM principles, Kotlin Coroutines, Firebase, and Room.

**Core Features Implemented**
  Authentication: Secure Email/Password login and registration powered by Firebase Auth. Includes Google Single Sign-On (SSO).
  Cloud Database: Uses Firebase Firestore to store and sync user preferences (temperature unit, app language, favorite city) across devices.
  Offline Mode: Utilizes Room Database to save favorite cities locally and caches the last known weather reading, allowing the app to function without an internet connection (displays an "OFFLINE" badge).
  Real-time Notifications: Implements WorkManager to schedule daily background weather notifications based on the user's last viewed city.
  Multi-language Support: Fully localized into three languages: English, isiZulu, and Afrikaans. Uses modern `AppCompatDelegate` for OS-level language persistence.
  REST API Integration: 
    OpenWeatherMap: Current weather, 5-day forecast, and city geocoding.
    Ticketmaster: Local event discovery.

**How to Run This Project**
  1. Clone the repository to your local machine.
  2. Open the project in Android Studio.
  3. Create a `local.properties` file in the root directory and add the required API keys:
      OPENWEATHER_API_KEY=your_key_here
      TICKETMASTER_API_KEY=your_key_here
