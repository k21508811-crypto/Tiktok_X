# TikTok X

TikTok X is an independent Android companion interface for opening public TikTok content.

**This project is unofficial and is not affiliated with, endorsed by, or sponsored by TikTok or ByteDance.** It does not modify the official TikTok app, access private APIs, bypass login, or unlock restricted content.

## Current prototype
- Dark Android interface with red/cyan accents
- Shortcut to the public TikTok website
- Local demonstration favourites state
- UI preferences for autoplay and quality (these do not control playback on TikTok's website)
- GitHub Actions workflow for building a debug APK

## Build
Open the repository in Android Studio or run the GitHub Actions workflow **Android APK**. After a successful run, download the `TikTok-X-debug-apk` artifact from that workflow.

## Privacy
The prototype does not request TikTok credentials and does not implement its own account system. Opening TikTok sends the user to TikTok's public website in the browser.

## Website
The landing page source is in the `site/` directory. GitHub Pages deployment is configured in `.github/workflows/pages.yml`. Enable GitHub Pages with **Build and deployment → Source: GitHub Actions** in repository settings if it is not already enabled.

## Disclaimer
TikTok and its logos are trademarks of their respective owners. This is an independent community project.
