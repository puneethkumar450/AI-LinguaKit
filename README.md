# LinguaKit — Android App

A focused, offline-first AI-powered translation app.

## AI & ML Integration 🤖

This app leverages **On-Device Artificial Intelligence** to provide fast, private, and offline-capable translation and vision features.

### 🧠 How the AI Works (On-Device ML)
You might wonder why the app requires downloading language packages. These aren't just simple dictionaries; they are **Neural Machine Translation (NMT)** models—essentially compact "brains" trained to understand and translate languages.

*   **The "Download" is the AI Model:** When you download a language pack (~30MB), you are installing a pre-trained neural network onto your device.
*   **Local Inference:** When you press "Translate," your phone's processor runs your text through these mathematical neural layers locally. No data is ever sent to a cloud server.
*   **Privacy & Speed:** Because the "thinking" happens entirely on-device, your data stays private, and translations are near-instant even without an internet connection.

| AI Feature                     | Technology                         | How it Works                                                                                                     |
|--------------------------------|------------------------------------|------------------------------------------------------------------------------------------------------------------|
| **Neural Machine Translation** | **Google ML Kit Translate**        | Uses deep learning models (NMT) to translate text locally. Models are downloaded once and work entirely offline. |
| **Computer Vision (OCR)**      | **Google ML Kit Text Recognition** | Employs AI-based vision models to detect and extract text from images captured via the camera.                   |
| **Language Identification**    | **Google ML Kit Language ID**      | Uses a compact classifier model to automatically detect the source language of your input text.                  |
| **Speech Synthesis**           | **Android Text-to-Speech (TTS)**   | Utilizes on-device voice synthesis engines to read text aloud.                                                   |

---

## Key Features

| # | Feature                   | Description                                                                                             |
|---|---------------------------|---------------------------------------------------------------------------------------------------------|
| 1 | **AI Translation**        | Translate text between 18 supported languages (including English, Hindi, Tamil, Telugu, Kannada, etc.). |
| 2 | **Text → Speech**         | Read any text aloud using high-quality on-device voice synthesis.                                       |
| 3 | **Photo → Text → Speech** | Use AI Vision to OCR a photo and then speak the extracted text.                                         |
| 4 | **Offline History**       | Automatically saves translations to a local database.                                                   |

---

## Tech Stack

| Layer        | Technology                                               |
|--------------|----------------------------------------------------------|
| UI           | Jetpack Compose + Material 3                             |
| Architecture | MVVM with `AndroidViewModel` + `StateFlow`               |
| Database     | **Room Persistence Library** (History & Pack management) |
| Translation  | **ML Kit Translate** (On-device AI)                      |
| OCR          | **ML Kit Text Recognition** (Vision AI)                  |
| Speech       | Android `TextToSpeech` API                               |
| Camera       | CameraX                                                  |

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

| Permission | Used For                             |
|------------|--------------------------------------|
| `CAMERA`   | Capturing photos for AI Vision (OCR) |
| `INTERNET` | Downloading AI models on first use   |

---

## Notes

- **Offline-First:** After downloading a language pack once, all AI features work without an internet connection.
- **Privacy:** Since AI processing happens on-device, your text and photos are never sent to a cloud server for processing.
