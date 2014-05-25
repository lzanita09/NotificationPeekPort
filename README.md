NotificationPeekPort
====================

<img align="right" height="144"
   src="https://raw.githubusercontent.com/lzanita09/NotificationPeekPort/master/art/launcher_icon.png" />

Port Paranoid Android's Notification Peek feature to an APK and make it compatible with any KitKat ROM. 


<img width="240" src="https://raw.githubusercontent.com/lzanita09/NotificationPeekPort/master/art/b3f58dc2-2b50-472e-ae6e-b0b4de942c9d.png" />
<img width="240" src="https://raw.githubusercontent.com/lzanita09/NotificationPeekPort/master/art/e9c1ace6-dc5b-4dbb-b408-66514f1ac16b.png" />

#### Google+ Community
I have published the app on Google Play through Beta channel, feel free to join the **[community](https://plus.google.com/communities/115556559938393378451)** to get the latest version.

#### What it does
Notification Peek (Paranoid Android) is a nice feature for displaying notifications on your screen when they arrive. It uses the device's gyroscope and/or proximity sensors to detect the status of the device and display the notifications in a minimalistic UI for a short time.

#### What I did
* Removed several system APIs, and replaced them with public APIs.
* UI tweaks, changed LinearLayout to GridLayout for displaying large numbers of unread notifications.
* Added notification peek timeout option.
* Added an option to choose which sensors are monitored when a notification arrives.
* Added a methodology for dismissing notifications and locking the screen upon dismissing the final notification.

#### How to uninstall
Because the Device Administrator disables apps that is activated from being uninstalled, you need to do the following if you want to uninstall the app:
* Go to your System Settings.
* Select **Security**.
* Select **Device administrators**.
* Uncheck Peek.
* Now you can uninstall the app.

#### What is missing
* A lot of great features.

#### Known issue
* Compatibility issue with Whatsapp.

#### Credits
* **[Paranoid Android](http://paranoidandroid.co/)** and its open source **[AOSPA project](https://github.com/AOSPA)**.
* **[Jesús David Gulfo Agudelo](https://plus.google.com/111563823310494239719/about)** and his **[Peek](https://play.google.com/store/apps/details?id=com.jedga.peek)**
* Icon by **[Jeppe Foldager](https://plus.google.com/+JeppeFoldager/about)**
* **[AcDisplay](https://github.com/AChep/AcDisplay)**
* Evelio Tarazona Cáceres and his **[ RoundedAvatarDrawable](https://gist.github.com/eveliotc/6051367)**
