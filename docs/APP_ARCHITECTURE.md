# Echo App Architecture & API Logic

This document serves as a reference for the application's logic, API endpoints, and data flow across
key screens.

## 1. Authentication & Onboarding

### Splash Screen

* **Logic**: Checks if a session exists locally (Token & User ID stored in `SessionRepository`).
* **Flow**:
    * **Has Session**: Navigate to `MainScreen`.
    * **No Session**: Navigate to `LoginScreen`.

### Login Screen (Phone Number)

* **Goal**: Initiate login via phone number.
* **API**: `POST /auth/otp`
* **Logic**:
    * Validates phone number format.
    * Calls API to send OTP.
    * On success, navigates to `VerifyOtpScreen`.

### Verify OTP Screen

* **Goal**: Verify the 6-digit code.
* **API**: `POST /auth/verify`
* **Logic**:
    * Sends Phone + OTP to server.
    * **Server Response**: Returns `AuthResponse` (`token`, `userId`, `isNewUser`, `user` object).
    * **App Logic**:
        * Saves Token & UserID to `DataStore` (Session).
        * **isNewUser = true**: Navigate to `CreateProfileScreen`.
        * **isNewUser = false**: Navigate to `MainScreen`.

### Create Profile Screen (Fresh Sign-up)

* **Goal**: Set initial Full Name and Username.
* **API**: `PUT /users/profile` (Update Profile)
* **Logic**:
    * User enters details.
    * App calls update profile API.
    * On success, navigates to `MainScreen`.

---

## 2. Main Navigation

The app starts on the `MainScreen`, hosting a Bottom Navigation Bar with:

1. **Echo** (Dashboard/Status)
2. **Inbox** (Messages)
3. **Connection** (Friends & Requests)
4. **Profile** (Settings)

---

## 3. Messaging (Chat & Inbox)

### Inbox Screen

* **Goal**: View list of received messages.
* **API**: `GET /messages/inbox`
* **Logic**:
    * Fetches messages where `receiver_id` matches current User ID.
    * **Server Join**:
        * Joins `Users` (on `sender_id`) to fetch `senderUsername`.
        * Joins `Replies` (on `message_id`) to attach any reply content.
    * **Display**: Shows `senderUsername` (or "Anonymous" if `isAnonymous` is true).
    * **Empty State**: Shows "No echos yet" if list is empty.

### Send Echo Screen (Chat)

* **Goal**: Send messages ("Echo") and view conversation history.
* **APIs**:
    * `POST /messages/send`: Send a new message.
    * `GET /messages/history`: Fetch conversation history.
* **Logic**:
    * **Sending**: Payload includes `receiverId`, `emotion`, `content`, `isAnonymous`.
    * **History**: Fetches messages between YOU and FRIEND (both sent and received).
    * **Server Join**: Joins `Users` (sender) to correctly identify message origin.
    * **Resolution**: Chat items display "To: [ReceiverName]" or "From: [SenderName]" based on
      context.

---

## 4. Contacts & Connections

### Connection Screen (Main Tab)

* **Goal**: Manage active friends and incoming requests.
* **APIs**:
    * `GET /contacts/connections`: Active friends (`status = CONNECTED`).
    * `GET /contacts/requests`: Pending incoming requests (`status = PENDING`,
      `initiated_by != YOU`).
* **Logic**:
    * Loads both lists in parallel via `ConnectionListViewModel`.
    * **Pending Requests**: displayed at the top. User can "Accept" (`POST /contacts/accept`) or "
      Reject".
    * **Connections**: displayed in a grid/list. Clicking opens `SendEchoScreen` for that user.

### Add Friend Screen (via + button)

* **Goal**: Connect with contacts who are **already on Echo**.
* **API**: `POST /contacts/sync` (via `getUnifiedContacts` repository method)
* **Repository Logic**:
    1. Fetch local device contacts.
    2. Filter out your own number.
    3. Hash phone numbers and sync with server.
    4. Update local objects with `isRegistered` flag based on server response.
* **Screen Logic**:
    * Filters unified list for `contact.isRegistered == true`.
    * **Action**: "Add" button sends request (`POST /contacts/request`).

### Invite to Echo Screen (via Invite button)

* **Goal**: Invite contacts who are **NOT on Echo**.
* **API**: `POST /contacts/sync` (Same source as Add Friend)
* **Screen Logic**:
    * Filters unified list for `contact.isRegistered == false`.
    * **Action**: "Invite" button launches system SMS app.

---

## 5. Settings & Profile

### Profile Screen

* **Goal**: View and Edit user details.
* **APIs**:
    * `GET /users/profile`: Load current details.
    * `PUT /users/profile`: Save updates.
* **Validation**:
    * **Full Name**: Max 50 chars, letters/spaces only.
    * **Username**: Exactly 8 chars, alphanumeric + underscores.
    * **Email**: Standard regex format.
* **Actions**:
    * **Logout**: Clears local session, navigates to Login.
    * **Delete Account**: Calls `DELETE /users/account`.

---

## Summary of Data Flow

* **Identity**: Users identified by `userId` (internal UUID) and `username` (public handle).
* **Privacy**: Contact matching is done via Phone Number Hashes (SHA-256).
* **Data Consistency**: The server performs necessary Joins (e.g., Messages + Users) to deliver
  complete DTOs (Data Transfer Objects) to the client, simplifying UI logic.
* **State Management**: Each screen has a dedicated ViewModel managing its specific data loading and
  UI state.
