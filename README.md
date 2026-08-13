\# Raksha – Women Safety \& Emergency Response System



Raksha is an Android app built to get help to someone as fast as possible during an emergency — with as little manual effort from the user as possible. Instead of requiring someone in distress to unlock their phone, open an app, and dial for help, Raksha automates the entire response chain from a single gesture.



\*\*One shake = live location sent via SMS to every saved emergency contact + an automatic call to the first one.\*\*



\## Why



In real emergencies, people rarely have the time or composure to interact with a phone in the usual way. Most existing safety apps still expect a manual button press and a stable internet connection — both of which can fail exactly when they're needed most. Raksha was built to close that gap using automation and offline-first communication (SMS) rather than asking more of the person in danger.



This project was published in the \*International Journal of Engineering Research \& Technology (IJERT)\*, Vol. 15, Issue 03, March 2026.



\## Features



\- \*\*Shake Detection\*\* — shake the phone to instantly trigger an SOS; a short countdown lets you cancel if it was accidental

\- \*\*SOS Alert System\*\* — sends your live GPS location via SMS to all saved emergency contacts and auto-calls the first one; since only one call can go out at a time, it prompts to call the next contact so no one is missed

\- \*\*Silent Mode\*\* — fires the SOS with zero sound or vibration, for situations where discretion matters

\- \*\*Fake Call\*\* — simulates a realistic incoming call to help exit an uncomfortable situation; shaking the phone during a fake call silently sends an SOS in the background

\- \*\*Safety Timer\*\* — set a custom countdown before walking somewhere alone; if not cancelled in time, SOS fires automatically

\- \*\*SOS History\*\* — every alert is logged with date, time, and location, and can be marked "Resolved"

\- \*\*Background Operation\*\* — shake detection, the safety timer, and silent SOS all continue working even when the app is minimized

\- \*\*Offline-first alerts\*\* — SMS-based, so alerts still go out without an internet connection

\- \*\*One-tap emergency call\*\* to 112 (police/ambulance/fire)



\## Tech Stack



\- \*\*Language:\*\* Kotlin

\- \*\*UI:\*\* Jetpack Compose, Material 3

\- \*\*Backend / Database:\*\* Firebase (Realtime Database / Firestore)

\- \*\*Location:\*\* Android GPS / Fused Location Provider

\- \*\*Communication:\*\* Android SMS Manager, TelephonyManager

\- \*\*Min SDK:\*\* 23 · \*\*Target SDK:\*\* 35



\## Getting Started



\### Prerequisites

\- Android Studio (latest stable)

\- A Firebase project with Realtime Database / Firestore enabled

\- Your own `google-services.json` (see below)



\### Setup

1\. Clone the repo

&#x20;  ```bash

&#x20;  git clone https://github.com/Sushmitadas1910/Raksha.git

&#x20;  ```

2\. Create a Firebase project at \[console.firebase.google.com](https://console.firebase.google.com), register an Android app with the package name used in `app/build.gradle.kts`, and download your own `google-services.json`.

3\. Place `google-services.json` inside the `app/` folder (this file is gitignored and \*\*not included\*\* in this repo for security reasons — see below).

4\. Open the project in Android Studio and let Gradle sync.

5\. Run on a device or emulator with SMS/location permissions granted.



> \*\*Note:\*\* This repo does not include a `google-services.json` file. You must supply your own Firebase config to build the project — this keeps API keys and project credentials out of version control.



\## Permissions Required



\- Location (fine + background, for GPS coordinates during SOS)

\- SMS (send + read, to deliver alerts)

\- Phone (to auto-call emergency contacts and 112)

\- Sensors (accelerometer, for shake detection)



\## Future Scope



\- AI-based automatic threat detection from behavior/voice

\- Voice-command SOS activation

\- Wearable device integration (smartwatches/bands)

\- Continuous live location sharing until marked safe

\- Direct integration with police/emergency services

\- Multilingual interface support



\## Author



Built solo, end-to-end, by \[Sushmita Das](https://github.com/Sushmitadas1910).

```




