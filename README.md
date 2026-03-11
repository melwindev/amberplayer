# AmberPlayer 🎵

**AmberPlayer** is a modern, high-performance Android music player that prioritizes a seamless blend of aesthetics and functionality. Built with a focus on immersive UI/UX, it features a unique **Liquid Glass** design and a dynamic theming engine.

---

## ✨ Key Technical Features

### 🎨 Dynamic Palette Theming
AmberPlayer doesn't just play music; it adapts to it. Using the **Android Palette API**, the app programmatically analyzes the album art of the currently playing track to:
* Extract dominant, vibrant, and muted color profiles.
* Synchronously update the global UI theme (backgrounds, buttons, and text) in real-time.
* Create a cohesive visual experience that changes with every song.

### 🧪 Liquid Glass Aesthetic
The interface is built on the principles of **Glassmorphism**, featuring:
* **Multi-layered Translucency:** Uses semi-transparent surfaces to create depth.
* **Real-time Gaussian Blur:** Implements high-quality background blurs that maintain performance.
* **Minimalist Navigation:** A clean, intuitive layout inspired by modern Linux environments like Pop!_OS.

### ⚙️ Core Functionality
* **Efficient Media Indexing:** Scans local storage for audio files with minimal memory overhead.
* **Background Service:** Uses `MediaSession` and `Foreground Services` to ensure uninterrupted playback and lock-screen controls.
* **Notification Integration:** Interactive media controls with dynamic color matching in the notification drawer.

---

## 🛠️ Tech Stack

* **Language:** Java / Kotlin (Android SDK)
* **IDE:** Android Studio
* **APIs & Libraries:**
    * `androidx.palette:palette` — For color extraction and dynamic theming.
    * `MediaSession` — For robust audio control and background services.
    * `Material Design 3` — For modern UI components and system integration.

---

## 🚀 Installation & Setup

1.  **Clone the Repo:**
    ```bash
    git clone [https://github.com/your-username/AmberPlayer.git](https://github.com/your-username/AmberPlayer.git)
    ```
2.  **Open in Android Studio:**
    `File > Open` and select the `AmberPlayer` directory.
3.  **Build and Run:**
    Ensure you have an Android device or emulator (API 24+) connected.

---

## 🛣️ Roadmap
- [ ] Add AI-driven playlist suggestions based on listening history.
- [ ] Implement a "Compact Mode" desktop widget.
- [ ] Integration with terminal-based Linux remote controls via Python.

---

## 👨‍💻 Author
**Melwin Shaju** *Computer Science & Engineering Student @ MNNIT Allahabad* [GitHub](https://github.com/melwindev)
