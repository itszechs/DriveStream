# Drive Stream

Google drive client with only one purpose that is to stream video files unlike the official app, this app streams the actual files instead of transcoded streams.

This app is powered by [Google Identiy SDK](https://developers.google.com/identity/sign-in/android/start-integrating) and [ExoPlayer](https://github.com/google/ExoPlayer).

## Screenshots

Home|Pagination|Player
:-----:|:--------------:|:-----------:|
![home](https://user-images.githubusercontent.com/52543663/174775309-a40f70b1-f66d-413e-a2e2-10392b745e11.png) | ![pagination](https://user-images.githubusercontent.com/52543663/174775303-72179030-e769-4943-ab2f-ef9851053101.png) | ![player](https://user-images.githubusercontent.com/52543663/174775314-434db667-05e0-4af7-bdf6-5e33ba6e5152.png)

## Download

Go to the [Releases](https://github.com/itsZECHS/DriveStream/releases) to download the latest APK.

## How to Build?

- Create a Google Console Project
- [Configure OAuth Consent Screen](https://developers.google.com/workspace/guides/configure-oauth-consent)
- [Configure your project with your siging key](https://developers.google.com/identity/sign-in/android/start-integrating#configure_a_project)


## What scopes are used?

[List of all drive scopes](https://developers.google.com/identity/protocols/oauth2/scopes#drive)

This app uses `https://www.googleapis.com/auth/drive` scope as it needs permission to `get()` or download the file in order to stream it.

**Q. What operations does it perform using the drive scope?**

- This app for the most part only lists files granted which can be done using `drive.readonly` scope but it also needs permissions to download the file so the video player can stream it. So to answer the question, app performs two operations
    
    - List files
    - Download files

**Q. Why does OAuth screen says "This app isn't verified"?**

- Yes, that is because my console project is not verified and I don't intent on getting it verified as it requires you to have a website and privacy policy page etc. Since the project is not verified and uses sensitve scopes number of grants is limited to 100 so people are advised on creating their own project and using their own client-keys
