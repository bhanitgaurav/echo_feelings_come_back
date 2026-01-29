# QR Code & Referral Flow Analysis

This document analyzes the current implementation of QR code scanning and referral link handling in the Echo application.

## 1. In-App Scanning (Working)

**Flow:** user opens Echo App -> Navigate to "Scan QR" -> Scans an Echo QR Code.

*   **Mechanism**: The app uses a built-in camera scanner (`ScanQrScreen`).
*   **Logic**:
    1.  The scanner reads the raw string from the QR code.
    2.  `ScanQrViewModel` parses the string using Regex:
        *   Matches pattern `.*/r/([A-Z0-9]+).*` -> Extracts Code.
        *   Alternative: checks for query param `?code=...`.
    3.  If a valid code is found, it calls `contactRepository.connectByReferral(code)`.
*   **Status**: **Functional**. The URL structure (`.../r/CODE...`) is correctly parsed by the regex.

## 2. External Scanning (Gap Identified)

**Flow:** User uses System Camera (iOS/Android) or 3rd party scanner -> Scans Echo QR Code.

*   **Mechanism**: The scanner opens the URL in the default browser (e.g., Chrome/Safari).
*   **URL Chain**:
    1.  **Initial URL**: `https://api-echo.bhanitgaurav.com/r/{CODE}?m=qr_scan`
    2.  **Server Behavior**: `ShortLinkRoutes.kt` processes this request.
    3.  **Redirection**: Server responds with `302 Found` to `/invite?code={CODE}&utm_medium=qr_scan...`.
*   **Issues**:
    *   **Server 404**: There is currently **no route handler** for `/invite` on the backend. The browser will receive a 404 Not Found error.
    *   **Deep Linking Missing**: The `AndroidManifest.xml` **does not** contain an `intent-filter` for the host `api-echo.bhanitgaurav.com`. This means the Android system will not prompt to open the Echo app automatically or handle the link, forcing the browser to attempt to load the (missing) page.

## 3. Summary of Cases

| Case | Action | Result | Note |
| :--- | :--- | :--- | :--- |
| **Echo App Scanner** | Scan QR | **Success** | Connects user directly via API. |
| **External Camera** | Scan QR | **Fail (404)** | Browser opens, redirects to /invite, fails. |
| **Share Link Click** | User clicks link | **Fail (404)** | Same flow as External Camera. |

## 4. Recommendations

To fix the External/Share flow, the following is required:

1.  **Deep Link Configuration**:
    *   **Android**: Add `intent-filter` with `autoVerify=true` for `api-echo.bhanitgaurav.com` in `AndroidManifest.xml`.
    *   **iOS**: Configure Associated Domains (`applinks:api-echo.bhanitgaurav.com`).
2.  **Fallback Page**:
    *   Implement a simple HTML page at `GET /invite` on the server.
    *   This page should handle cases where the app is not installed, providing "Download on Play Store" / "Download on App Store" buttons.
    *   Ideally, use a "Universal Link" / "App Link" strategy where the OS opens the app if installed, and falls back to this HTML page if not.
