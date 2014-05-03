NotificationPeekPort
====================

Port Paranoid Android's Notification Peek feature to an apk and make it compatible with any Kit-Kat ROM.

The latest built apk can be found under the releases tab.

The original source code for Paranoid's Notification Peek is in **[here](https://github.com/AOSPA/android_frameworks_base/tree/kitkat/packages/SystemUI/src/com/android/systemui/statusbar/notification)**. 

#### What it is:
Notification Peek responses to incoming notifications, and uses gyroscope and proximity sensors to detect states of the device, and decide whether to wake up the device and display notifications in a minimalistic layout.


#### What I did:
* Removed several system level API uses, replaced with public API.
* Slightly tweaked the layout of the notification peek.
* Attempted to make it work without gyroscope sensor.

#### What is missing:
* Icon.
* Name.
* A lot of great features.

#### Installation Requirements:
* Android 4.4 KitKat
* Gyroscope sensor and priximity sensor. (Althought I want to make it work without gyroscope, I'm not sure at this point if it works)


#### Credits:
* **[Paranoid Android team](https://plus.google.com/+ParanoidAndroidCorner/about)** and their **[AOSPA project](https://github.com/AOSPA)**.
