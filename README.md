# Kiselgram Desktop

JavaFX desktop client for [Kiselgram](https://kiselgram.ru) — a modern messaging platform.

## Requirements

- Java 21 (JDK)
- macOS (Apple Silicon or Intel) or Windows

## Build & Run

```bash
./gradlew build   # compile + create JAR
./gradlew run     # launch the app
```

## Distribution

Pre-built binaries are available at [desktop.kiselgram.ru](https://desktop.kiselgram.ru):
- `Kiselgram-mac-arm64.zip` — self-contained .app for Apple Silicon (includes Java 21 runtime)
- `Kiselgram-mac-intel.zip` — launcher for Intel Macs (requires Java 21)
- `Kiselgram-windows.zip` — launcher for Windows (requires Java 21)

## Features

- Login via password, QR code, or email
- Real-time messaging with read receipts, typing indicators, reactions
- File sharing (images, documents, audio, video)
- Group chats with member management
- Channel subscriptions
- Stories with views and likes
- Video calls (WebRTC)
- End-to-end saved messages
- Global search
- Customizable themes, fonts, and bubble colors
- System tray notifications

## API

Communicates with `https://api.kiselgram.ru/api.v2/api` using Bearer token auth.

## License

MIT
