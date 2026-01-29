# Marketing Shortlinks

This document lists the available shortlinks for tracking social media and other marketing campaigns.

## Social Media Bio Links

These links are designed to be placed in the "Bio" or "Website" section of social media profiles. They automatically track the source of the install.

| Platform | Shortlink | Redirects To | UTM Source | UTM Medium | UTM Campaign |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Instagram** | `/r/instagram` | `/invite?code=INSTAGRAM_BIO...` | `instagram` | `profile` | `bio_link` |
| **LinkedIn** | `/r/linkedin` | `/invite?code=LINKEDIN_BIO...` | `linkedin` | `profile` | `bio_link` |
| **Facebook** | `/r/facebook` | `/invite?code=FACEBOOK_BIO...` | `facebook` | `profile` | `bio_link` |
| **Reddit** | `/r/reddit` | `/invite?code=REDDIT_BIO...` | `reddit` | `profile` | `bio_link` |
| **Twitter / X** | `/r/twitter` | `/invite?code=TWITTER_BIO...` | `twitter` | `profile` | `bio_link` |
| **TikTok** | `/r/tiktok` | `/invite?code=TIKTOK_BIO...` | `tiktok` | `profile` | `bio_link` |
| **YouTube** | `/r/youtube` | `/invite?code=YOUTUBE_BIO...` | `youtube` | `profile` | `bio_link` |
| **Snapchat** | `/r/snap` | `/invite?code=SNAP_BIO...` | `snap` | `profile` | `bio_link` |

## Usage

Use the full URL format: `https://api-echo.bhanitgaurav.com{Shortlink}`

**Examples:**

*   **Instagram**: `https://api-echo.bhanitgaurav.com/r/instagram`
*   **LinkedIn**: `https://api-echo.bhanitgaurav.com/r/linkedin`
*   **Facebook**: `https://api-echo.bhanitgaurav.com/r/facebook`
*   **Reddit**: `https://api-echo.bhanitgaurav.com/r/reddit`
*   **Twitter / X**: `https://api-echo.bhanitgaurav.com/r/twitter`
*   **TikTok**: `https://api-echo.bhanitgaurav.com/r/tiktok`
*   **YouTube**: `https://api-echo.bhanitgaurav.com/r/youtube`
*   **Snapchat**: `https://api-echo.bhanitgaurav.com/r/snap`

## Dynamic Custom Links

The system is designed to be **dynamic**. You do not need to ask developers to add new links.

### How it works:
If you create a link with **any text** that contains **lowercase letters** (e.g., words, names), the system automatically treats it as a marketing campaign.

*   `.../r/aaratai` -> Tracks as Source: `aaratai`
*   `.../r/newsletter` -> Tracks as Source: `newsletter`
*   `.../r/launchparty` -> Tracks as Source: `launchparty`

This allows you to create unlimited custom tracking links on the fly.

**Note:** Standard user referral codes like `ABC1234` (all uppercase) will continue to work as normal user invites.
