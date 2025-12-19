# Technical Documentation - Kiddozz App

## 1. Introduction

Kiddozz is a mobile application designed for kindergartens in Finland, aiming to provide a superior alternative to existing daycare management apps. It will feature separate interfaces for educators and parents, focusing on ease of use, robust functionality, and data privacy.

## 2. Architecture

*   **Client-Side (Mobile App)**: Native Android application developed using Kotlin and Jetpack Compose for the UI.
*   **Backend**: To be decided (e.g., Firebase, custom REST API with a cloud provider like GCP or AWS).
*   **Database**: To be decided (e.g., Firestore, PostgreSQL, MySQL).
*   **MVVM (Model-View-ViewModel)**: Will be the primary architectural pattern for the Android app to ensure separation of concerns and testability.

## 3. Technology Stack

*   **Programming Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose
*   **Navigation**: Jetpack Navigation Compose
*   **Asynchronous Operations**: Kotlin Coroutines & Flow
*   **Networking**: Retrofit (if using a custom backend) or Firebase SDKs

## 4. Database Partitioning

The `kid_absences` table is partitioned by year for performance and archival purposes. See [backend/docs/absences_partitioning.md](backend/docs/absences_partitioning.md) for detailed information about:

- How partitioning works
- Archival procedures
- Performance benefits
- Troubleshooting guide

## 5. Testing Database Policy

- **CI (GitHub Actions)** always runs backend tests against **PostgreSQL** by starting a PG service and setting `DATABASE_URL` accordingly; migrations run before pytest.
- **Local developers** may run tests on SQLite (default local `DATABASE_URL`) for speed.
- Tests that depend on PG-only features (partitions, `pg_tables`, `ALTER ... PARTITION`) must be guarded with:
  - `if engine.dialect.name != "postgresql": pytest.skip(...)`
- A session-scoped fixture prints the active test DB: see `tests/conftest.py`.

This policy prevents flaky results and keeps Postgres-specific features validated in CI. See [backend/docs/TESTING.md](backend/docs/TESTING.md) for more details.
*   **Image Loading**: Coil or Glide
*   **Local Storage**: Room (for caching or offline data), Android SharedPreferences (for settings)
*   **Calendar Integration**: Google Calendar API
*   **Push Notifications**: Firebase Cloud Messaging (FCM)
*   **Backend Options**:
    *   Firebase (Firestore, Firebase Authentication, Cloud Functions, FCM, Cloud Storage)
    *   Custom backend (e.g., Ktor, Spring Boot) hosted on a cloud platform.

## 4. Data Management

*   **User Data**: Securely stored, with clear distinctions between educator and parent data.
*   **Child Data**: Handled with utmost care, respecting privacy regulations (e.g., GDPR). Consent mechanisms will be crucial.
*   **Image Storage**: Cloud-based storage (e.g., Firebase Cloud Storage, AWS S3) with appropriate access controls.
*   **Offline Support**: Basic offline capabilities for viewing cached data (e.g., messages, calendar events).

## 5. Security and Privacy

*   **Authentication**: Secure authentication for educators and parents (e.g., Firebase Authentication, OAuth).
*   **Authorization**: Role-based access control to ensure users only see data relevant to them.
*   **Data Encryption**: Data in transit (TLS/SSL) and at rest (if necessary beyond platform defaults).
*   **Consent Management**: Granular controls for parents regarding image sharing and usage.
*   **Compliance**: Adherence to Finnish privacy laws and GDPR.

## 6. API Design (If Custom Backend)

*   RESTful APIs.
*   Clear versioning.
*   Secure endpoints using token-based authentication (e.g., JWT).

## 7. Key Features Breakdown

(This section will detail the technical implementation aspects of each feature mentioned by the user, e.g., how image sharing works, how calendar sync is implemented, notification system, etc.)

*   User Roles (Educator, Parent)
*   Child Profiles & Guardian Info
*   In-App Camera & Gallery Integration
*   Image Sharing & Approval Workflow
*   Google Calendar Integration
*   Events Section (Upcoming, Past, Editing, Image Attachments)
*   Menu Section
*   Educator Profiles
*   Parent View: Child Galleries, Messages, Calendar (read-only), Events, Child Profiles
*   Caregiver Profiles & Consents
*   Settings (Daycare Info, Notifications, Language, Parent Consents)
