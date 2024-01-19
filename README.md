<p align="center">
  <img src="https://user-images.githubusercontent.com/52543663/177340758-57d2b8a6-a6a5-4b87-935a-7986384a81c0.png" align="center" width="128" />
<p>
<h1 align="center">
  Drive Stream
</h1>
<p align="center">
Stream your video files directly from Google Drive with this specialized client. Unlike the official app, this app streams the original files for a better viewing experience. Download now and start streaming your favorite videos from Google Drive.
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
  <a href="https://github.com/itszechs/DriveStream/blob/main/LICENSE">
    <img alt="GitHub" src="https://img.shields.io/github/license/itszechs/DriveStream?style=for-the-badge">
  </a>
  <img alt="Codefactor rating" src="https://img.shields.io/codefactor/grade/github/itszechs/DriveStream/main?style=for-the-badge">
</div>

<br>

## Screenshots

Splash|Home|Themes
:-----:|:-------------------------------:|:-----------:|
![splash](https://user-images.githubusercontent.com/52543663/229365138-dd843448-ec71-40ac-b2b8-af2080bc55d5.png)|![home](https://user-images.githubusercontent.com/52543663/229365159-992915d4-f5df-4429-9374-15c8656ac541.png)|![themes](https://user-images.githubusercontent.com/52543663/229365393-6f7dff70-aca9-4fdd-91a5-ab66e6505ed6.png)
Pagination|Players|Playback
![pagination](https://user-images.githubusercontent.com/52543663/229365224-e2e4a4b0-44ea-4c24-87d9-9e078bf0332a.png)|![playback](https://user-images.githubusercontent.com/52543663/229365431-b39c0d2c-34cc-4891-b7ee-8dda376f47a2.png)|![player](https://user-images.githubusercontent.com/52543663/229365226-606f6eb7-2041-4dcf-9efb-27f4abef0b27.png)


## Download

Go to the [Releases](https://github.com/itsZECHS/DriveStream/releases) to download the latest APK.


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

## Support the project

<a href="https://www.buymeacoffee.com/zechs"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=zechs&button_colour=FFDD00&font_colour=000000&font_family=Poppins&outline_colour=000000&coffee_colour=ffffff" /></a>

## Note

Starting [v1.3.1](https://github.com/itszechs/DriveStream/tree/1.3.1) users MUST create their own OAuth client to continue using the app.


<details>
  <summary>A step-by-step video guide on how to create an OAuth client.</summary>
  

  https://user-images.githubusercontent.com/52543663/229366054-29e67440-9920-4504-8e1d-4f8eda367f2b.mp4
  
</details>
