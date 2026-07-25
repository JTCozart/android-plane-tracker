# Privacy Policy

**Effective date:** July 24, 2026

## Overview

PlaneTracker is a hobby app that shows aircraft flying near your current location using publicly available ADS-B data. This policy explains what data the app uses and how.

## Data collected

**Location**
PlaneTracker accesses your device's GPS location while the app is running. Your location is used only to center the aircraft search radius. It is never stored on any server, shared with third parties, or retained after the app is closed.

**Aircraft data**
The app queries [adsb.lol](https://adsb.lol), a free public ADS-B aggregator, with your approximate coordinates and search radius to retrieve nearby aircraft positions. The adsb.lol service may log these requests in accordance with their own privacy policy.

**Advertising**
PlaneTracker displays banner ads served by Google AdMob on the Live, Map, and History tabs. AdMob may collect your advertising ID and other identifiers to serve and measure ads, and may use this data in accordance with Google's own privacy policy. You can review or reset your advertising ID, or opt out of personalized ads, in your device's Google Settings.

## Data not collected

- No account or registration is required.
- No personal information is collected or transmitted by PlaneTracker itself.
- No analytics or crash reporting SDKs are included.
- No data is stored outside your device by PlaneTracker.

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Determine your position to search for nearby aircraft |
| `ACCESS_COARSE_LOCATION` | Fallback if fine location is unavailable |
| `FOREGROUND_SERVICE` | Keep scanning while the app is in the background |
| `POST_NOTIFICATIONS` | Alert you when aircraft enter your radius |
| `com.google.android.gms.permission.AD_ID` | Required by Google AdMob to serve and measure ads |

## Third-party services

PlaneTracker uses [adsb.lol](https://adsb.lol) to retrieve aircraft data. Requests include your latitude, longitude, and search radius. Please review adsb.lol's own privacy policy for details on how they handle API requests.

PlaneTracker uses [Google AdMob](https://policies.google.com/privacy) to display ads. Review Google's privacy policy for details on how they handle data collected through AdMob.

## Children's privacy

This app does not knowingly collect any information from anyone, including children under 13.

## Changes

If this policy changes, the updated version will be posted here with a new effective date.

## Contact

Questions or concerns? Open an issue at the project repository or contact the developer directly.
