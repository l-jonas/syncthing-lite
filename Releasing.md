# Releasing

- do tests
- update translations using ``tx pull -a -af`` (as extra merge request or branch for the case it does not build correctly)
- update the version name and version code of the app
- update the changelog at [app/src/main/play/en-GB/whatsnew](https://github.com/syncthing/syncthing-lite/blob/master/app/src/main/play/en-GB/whatsnew)
- create a tag/ release in GitHub with an changelog; The tag name should be the version number
- F-Droid picks up the release by the tag; additonally, the tag triggers a CI build which uploads the generated APK to Google Play