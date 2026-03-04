# LinguaKit — Android App

A focused, offline-first AI-powered translation app.

## AI & ML Integration 🤖

This app leverages **On-Device Artificial Intelligence** to provide fast, private, and offline-capable translation and vision features.

| AI Feature | Technology | How it Works |
|------------|------------|--------------|
| **Neural Machine Translation** | **Google ML Kit Translate** | Uses deep learning models (NMT) to translate text locally on the device. Models are downloaded once per language pair (~30MB) and then work entirely offline. |
| **Computer Vision (OCR)** | **Google ML Kit Text Recognition** | Employs AI-based vision models to detect and extract text from images captured via the camera or selected from the gallery. |
| **Speech Synthesis** | **Android Text-to-Speech (TTS)** | Utilizes on-device voice synthesis engines to read translated or recognized text aloud. |

---

## Key Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **AI Translation** | Translate text between 18 supported languages (including English, Hindi, Tamil, Telugu, Kannada, etc.). |
| 2 | **Text → Speech** | Read any text aloud using high-quality on-device voice synthesis. |
| 3 | **Photo → Text → Speech** | Use AI Vision to OCR a photo and then speak the extracted text. |
| 4 | **Offline History** | Automatically saves translations. If offline, the app checks history before requiring a connection. |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM with `AndroidViewModel` + `StateFlow` |
| Database | **Room Persistence Library** (History & Pack management) |
| Translation | **ML Kit Translate** (On-device AI) |
| OCR | **ML Kit Text Recognition** (Vision AI) |
| Speech | Android `TextToSpeech` API |
| Camera | CameraX |

---

## Project Structure

```
app/src/main/java/com/example/translateapp/
├── MainActivity.kt               # Entry point
├── data/                         # Room Database, DAOs, and Entities
├── service/                      # Model Download & Repository logic
├── viewmodel/
│   └── AppViewModel.kt           # All business logic, AI calls, TTS
└── ui/                           # Jetpack Compose Screens
```

---

## Supported Languages

English · Spanish · French · German · Italian · Portuguese · Chinese · Japanese · Korean · Arabic · Russian · Hindi · **Tamil** · **Telugu** · **Bengali** · **Marathi** · **Kannada** · **Vietnamese**

---

## Permissions

| Permission | Used For |
|-----------|---------|
| `CAMERA` | Capturing photos for AI Vision (OCR) |
| `INTERNET` | Downloading AI models on first use |

---

## Notes

- **Offline-First:** After downloading a language pack once, all AI features work without an internet connection.
- **Privacy:** Since AI processing happens on-device, your text and photos are never sent to a cloud server for processing.
