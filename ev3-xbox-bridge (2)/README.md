EV3-Xbox-Bridge (Minimal Project)
=================================
This is a minimal Android project prepared for building an APK that acts as a bridge
between an Xbox controller (Bluetooth) and a LEGO EV3 running an ev3dev server.

CONTENTS:
- app/src/main/java/com/example/ev3bridge/MainActivity.kt
- app/src/main/res/layout/activity_main.xml
- app/src/main/AndroidManifest.xml
- build.gradle (project)
- settings.gradle
- app/build.gradle (module)
- .github/workflows/android-build.yml

NOTE:
- This is a minimal skeleton prepared for you. Building an Android APK in GitHub Actions
  requires Android SDK components which the workflow attempts to set up. If the build fails,
  you (or someone helping you) can run the project locally in Android Studio to produce the APK.
- I included the full Kotlin source for MainActivity implementing the controller -> JSON send logic.
