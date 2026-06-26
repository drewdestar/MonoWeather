# MonoWeather

<svg xmlns="http://www.w3.org/2000/svg" width="120" height="48" viewBox="0 0 120 48">
  <rect width="120" height="48" rx="6" fill="#000000"/>
  <text x="60" y="32" font-family="Inter, Helvetica, Arial, sans-serif" font-size="20" font-weight="700" fill="#FFFFFF" text-anchor="middle" letter-spacing="4">NEWR</text>
</svg>

Very Simple Edition.

---

Version 1.0.0  
Platform Android  
License MIT  
Built by NEWR Labs

---

## About

MonoWeather is the Very Simple Edition of a weather application.

It shows temperature, UV index, wind speed, humidity, and visual weather conditions (sunny, rainy, cloudy, etc.). No radar. No air quality index. No news feed. No graphs. No ads. Just the weather.

This edition is intentionally limited. It is designed to be readable, fast, and calm.

Black and white interface. No colors. No gradients. Only text and space.

---

## Features

- Current temperature for your location
- UV Index for sun safety awareness
- Wind speed and humidity data
- Visual weather icons (sun, rain, clouds, etc.)
- Search for any city or country by tapping the current city name
- Reset to current location by tapping the "Search Location" button or reset control
- Hourly and daily forecast
- Weather notifications for significant changes (rain alert, extreme temperatures, etc.)
- Pure black and white display
- Minimal APK size

---

## Location & Search

- Tap on the displayed city name to search for a new city or country.
- Tap the "Search Location" button (or equivalent reset control) to return to your current GPS location.
- Location permissions are required only for the current location feature and are handled securely.

---

## Notifications

MonoWeather can send notifications for significant weather updates, such as:
- Rain alerts
- Extreme temperature warnings
- Strong wind advisories

Users can enable or disable notifications from the app settings. Notifications are minimal and respect user privacy.

---

## Screenshots

Screenshots will be added upon release.

---

## Build from Source

### Requirements

- Android Studio (latest version)
- Android SDK
- Internet connection

### Steps

Clone the repository

git clone https://github.com/NEWR-LABS/MonoWeather.git
cd MonoWeather

Set up API key

Copy local.properties.template to local.properties
Add your OpenWeatherMap API key inside the file:
WEATHER_API_KEY=your_api_key_here

Build the project

Open in Android Studio and select Build > Make Project
Or build via terminal:
./gradlew assembleRelease

Run on device

Connect an Android device or start an emulator, then click Run.

---

## Developer Notice

This project includes a build-time verification system using newr_secret.gradle. If the project is built without this file, the application will enter Chaos Mode. This is intended to discourage unauthorized copying or cloning.

---

## Inquiries & Collaboration

For access requests, collaboration inquiries, or any questions regarding the code, please reach out through the official NEWR Labs channels:

- X (Twitter): @NEWROPLY
- Instagram: @NEWROPLY
- Discord: https://discord.gg/vZU3KzEh6a

All requests will be reviewed by the NEWR Labs team. We welcome genuine collaboration and contributions.

---

## Technology Stack

Language: Kotlin  
UI Framework: Jetpack Compose  
Networking: Retrofit with OkHttp  
Image Loading: Glide or Coil  
Weather Data: OpenWeatherMap API  
Architecture: MVVM with Repository pattern

---

## Connect with NEWR Labs

X (Twitter): @NEWROPLY  
Instagram: @NEWROPLY  
TikTok: @NEWROPLY  
Threads: @NEWROPLY  
Discord: https://discord.gg/vZU3KzEh6a  
GitHub: https://github.com/NEWR-LABS

---

## License

MIT License  
Copyright (c) 2026 NEWR Labs

---

## About NEWR Labs

NEWR Labs is a digital studio building simple, functional, and visually clean applications.

---

MonoWeather. Very Simple Edition.
