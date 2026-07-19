# Voice Changer — Android MVP

## এই প্রজেক্টে যা আছে
- একটা কাজ করা Android Studio প্রজেক্ট (Kotlin)
- Live মাইক ইনপুট capture (`AudioRecord`)
- Real-time pitch shifting (pure Kotlin, no external library) — `PitchShifter.kt`
- ৬টা প্রিসেট ভয়েস: Normal, Cute Girl, Deep Male, Chipmunk, Robot, Monster
- Start/Stop বাটন দিয়ে লাইভ প্রসেসিং শুরু/বন্ধ

## কীভাবে চালাবেন
1. Android Studio (Hedgehog বা তার পরের ভার্সন) দিয়ে এই ফোল্ডারটা ওপেন করুন
2. Gradle sync হতে দিন (প্রথমবার একটু সময় লাগবে)
3. একটা Android ফোনে (emulator-এ মাইক টেস্ট ভালো হয় না) Run করুন
4. মাইক পারমিশন দিন, Start চাপুন, প্রিসেট বাছুন, কথা বলুন — earpiece/speaker-এ পরিবর্তিত ভয়েস শুনবেন

## বর্তমান সীমাবদ্ধতা (গুরুত্বপূর্ণ)

### ১. Pitch shift কোয়ালিটি
এখন যেটা আছে সেটা একটা সিম্পল resampling-based pitch shift। এটা কাজ করে কিন্তু:
- "Cute girl" ভয়েস একটু chipmunk-এর মতো শোনাতে পারে (natural না)
- প্রফেশনাল কোয়ালিটির জন্য **SoundTouch** (C++ লাইব্রেরি, NDK দিয়ে integrate করতে হয়) অথবা **RVC/so-vits-svc** এর মতো AI মডেল লাগবে

**পরবর্তী ধাপ (কোয়ালিটি বাড়াতে):**
- SoundTouch লাইব্রেরি Android NDK-তে বসিয়ে formant-preserving pitch shift করুন (pitch বদলাবে কিন্তু কণ্ঠস্বর কম রোবোটিক শোনাবে)
- অথবা cloud-এ RVC মডেল হোস্ট করে WebSocket দিয়ে স্ট্রিম করুন (অনেক বেশি রিয়েলিস্টিক "cute girl" ভয়েস পাবেন, কিন্তু latency বেশি ও সার্ভার খরচ লাগবে)

### ২. গেম/লাইভ স্ট্রিমে ভয়েস পাঠানো (Virtual Mic)
এই ভার্সনে প্রসেসড ভয়েস শুধু ফোনের স্পিকারে বাজে। Discord/PUBG/Free Fire বা লাইভ স্ট্রিমিং অ্যাপে এই ভয়েসটাকে "মাইক" হিসেবে পাঠাতে হলে Android-এ একটা **Virtual Audio Routing** সমাধান লাগবে, যেমন:
- একটা virtual audio driver অ্যাপ বানানো (জটিল, প্রায়ই root লাগে)
- অথবা `AudioPlaybackCapture` API (Android 10+) ব্যবহার করে creative routing — এটাও জটিল এবং প্রতিটা টার্গেট অ্যাপের সাথে আলাদাভাবে কাজ নাও করতে পারে

এটা একটা বড় আলাদা কাজ, বর্তমান প্রজেক্টে নেই।

## ফাইল স্ট্রাকচার
```
VoiceChanger/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/voicechanger/
│       │   ├── MainActivity.kt      ← মাইক capture + playback লজিক
│       │   └── PitchShifter.kt      ← pitch shift অ্যালগরিদম + প্রিসেট
│       └── res/layout/activity_main.xml
├── build.gradle
└── settings.gradle
```
