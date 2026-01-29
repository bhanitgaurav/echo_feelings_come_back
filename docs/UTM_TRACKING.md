# UTM Tracking Parameters Documentation

This document outlines the URL tracking strategy for referral links in the Echo app. We use a short `m` parameter in the client, which the backend redirects and expands into full Google Analytics UTM parameters (`utm_source`, `utm_medium`, `utm_campaign`).

## Parameter Mapping

| Source Screen | Client Param (`m`) | Backend Map (`utm_medium`) | Description |
| :--- | :--- | :--- | :--- |
| **Available Contacts** | `invite` | `invite_user_screen` | "Invite" button next to a contact in the find friends list. |
| **Refer & Earn (Credits)** | `invite` | `invite_user_screen` | "Share Link" button on the dedicated Rewards/Referral screen. |
| **Profile QR Code** | `qr_scan` | `qr_scan` | Scanned from the user's profile QR code dialog. |
| **Instagram Bio** | *(Static)* | `profile` | Special case for `instagram` code. |
| **Generic/Unknown** | *(None/Other)* | `user_share_generic` | Fallback for any other or missing parameter. |

## Detailed Flow

1.  **Client Generation**: The app appends `?m={value}` to the path-based referral URL (e.g., `https://api-echo.bhanitgaurav.com/r/CODE?m=invite`).
2.  **Backend Handling** (`ShortLinkRoutes.kt`):
    *   Receives `GET /r/{code}`.
    *   Extracts `m` parameter.
    *   Matches `m` to a specific `utm_medium` string.
3.  **Redirection**:
    *   Redirects user to the app scheme or play/app store URL (currently `/invite`).
    *   Appends standard UTM params:
        *   `utm_source=echo_app` (Fixed)
        *   `utm_campaign=user_referral` (Fixed)
        *   `utm_medium={mapped_value}` (Dynamic)
    *   **Passthrough**: Any other query parameters added to the original link are preserved and passed to the destination.
