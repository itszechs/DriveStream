<p align="center">
  <img src="https://user-images.githubusercontent.com/52543663/177340758-57d2b8a6-a6a5-4b87-935a-7986384a81c0.png" align="center" width="128" />
<p>
<h1 align="center">
  Drive Stream
</h1>
<p align="center">
  Google drive client with only one purpose that is to stream video files unlike the official app,
  this app streams the actual files instead of transcoded streams.
</p>

<p align="center">
  This app is powered by <a href="https://developers.google.com/drive/api">Drive API</a>, <a href="https://github.com/google/ExoPlayer">ExoPlayer</a> and <a href="https://github.com/mpv-android">mpv-android</a>.
</p>

<div align="center">
    <a href="https://github.com/itszechs/DriveStream/releases">

  <img alt="Download count" src="https://img.shields.io/github/downloads/itszechs/DriveStream/total?style=for-the-badge">
  </a>
      <a href="https://github.com/itszechs/DriveStream/latest">
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/itszechs/DriveStream?style=for-the-badge">
  </a>
  <a href="https://github.com/itszechs/DriveStream/blob/master/LICENSE">
    <img alt="GitHub" src="https://img.shields.io/github/license/itszechs/DriveStream?style=for-the-badge">
  </a>
  <img alt="Codefactor rating" src="https://img.shields.io/codefactor/grade/github/itszechs/DriveStream/master?style=for-the-badge">
</div>

<br>
<br>

## Screenshots

Home|Pagination|Player
:-----:|:--------------:|:-----------:|
![home](https://user-images.githubusercontent.com/52543663/174775309-a40f70b1-f66d-413e-a2e2-10392b745e11.png) | ![pagination](https://user-images.githubusercontent.com/52543663/174775303-72179030-e769-4943-ab2f-ef9851053101.png) | ![player](https://user-images.githubusercontent.com/52543663/174775314-434db667-05e0-4af7-bdf6-5e33ba6e5152.png)

## Download

Go to the [Releases](https://github.com/itsZECHS/DriveStream/releases) to download the latest APK.

## How to Build?

- Create a Google Console Project
- [Enable Drive API](https://developers.google.com/drive/api/guides/enable-drive-api#enable_the_drive_api)
- [Create OAuth Client](https://console.developers.google.com/apis/credentials/oauthclient) (
  Application type:
  Web application)
- Add `CLIENT_ID` & `CLIENT_SECRET` in `local.properties`

Note: You can configure all the constants
in [Constants.kt](https://github.com/itszechs/DriveStream/blob/master/app/src/main/java/zechs/drive/stream/utils/util/Constants.kt) (
client id, secret, redirect uri etc.)

## What scopes are used?

[List of all drive scopes](https://developers.google.com/identity/protocols/oauth2/scopes#drive)

This app uses `https://www.googleapis.com/auth/drive` scope as it needs permission to `get()` or
download the file in order to stream it.

**Q. What operations does it perform using the drive scope?**

- This app for the most part only lists files granted which can be done using `drive.readonly` scope
  but it also needs permissions to download the file so the video player can stream it. So to answer
  the question, app performs two operations

    - List files
    - Download files


Thanks to [mpv-android](https://github.com/mpv-android) for buildscripts to compile `libMPV`
and `MPVLib` `MPVView`.

Note: App releases are built using [rclone](https://github.com/rclone/rclone)'s client id.
