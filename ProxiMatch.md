# ProxiMatch: High-Level Requirements (v1.0)

## 1. Introduction

### 1.1 Vision

To create a hyper-local, privacy-first dating application that connects individuals based on shared interests and personality traits through Bluetooth technology, fostering spontaneous and meaningful connections in the real world.

### 1.2 Core Principles

- **Proximity-Based:** The app's primary function is to discover potential matches within the user's immediate physical vicinity.

- **Privacy-First:** User identity and data are protected. The app operates anonymously until a mutual connection is established. No central server stores user locations or sensitive personal information.

- **User-Controlled:** Users have complete control over their criteria, who they connect with, and what information they share.

## 2. System and Functional Requirements

### 2.1 User Onboarding & Profile

- **2.1.1 Anonymous Setup:** Users can start using the app without creating a detailed public profile. Initial setup only requires setting matching criteria.

- **2.1.2 Profile Criteria ("Vibe Check"):**
  
  - Upon first use, the user selects their own attributes from a predefined list of 32 (or 64 for premium users) options.
  
  - These options will cover a range of categories: Interests & Hobbies, Lifestyle, Personality Traits, and Communication Styles.
  
  - The user will also select the attributes they are looking for in a potential match.

- **2.1.3 Private Profile:** A more detailed profile (name, photos, bio) can be created but is only shared *after* a successful, mutual connection is made.

### 2.2 Discovery & Matching

- **2.2.1 Bluetooth LE Broadcast:**
  
  - The app will use Bluetooth Low Energy (BLE) non-connectable advertising packets to broadcast the user's selected criteria.
  
  - This broadcast will contain an anonymized, compressed representation of the 32/64 criteria points. It will not contain any personally identifiable information.

- **2.2.2 Radar Interface:**
  
  - The main screen will be a radar-like interface that displays other active users in the vicinity.
  
  - Each nearby user will be represented by an anonymous dot or icon on the radar.

- **2.2.3 Match Calculation & Display:**
  
  - The app continuously scans for BLE broadcasts from other users.
  
  - When another user is detected, the app compares their broadcasted criteria against the user's own preferences.
  
  - A match percentage is calculated, and the anonymous icon on the radar is color-coded accordingly:
    
    - **Green:** High match compatibility (>75%)
    
    - **Yellow:** Medium match compatibility (40-75%)
    
    - **Grey:** Low match compatibility (<40%)

### 2.3 Connection & Communication

- **2.3.1 Connection Request:**
  
  - A user can tap on any dot on their radar to send a private, anonymous "interest" request.

- **2.3.2 Mutual Match:**
  
  - A connection is only established if both users express interest in each other.
  
  - If User A requests to connect with User B, User B will not be notified unless they also independently request to connect with User A.

- **2.3.3 Secure Chat:**
  
  - Once a mutual match is confirmed, a secure, end-to-end encrypted chat channel is opened between the two users.
  
  - At this point, users can choose to reveal their private profiles (names, photos, etc.) to each other within the chat.

## 3. Technical Requirements

- **3.1 Core Technology:** Bluetooth Low Energy (BLE).

- **3.2 Security:** End-to-end encryption for all post-match communication. No central database of user locations or real-time movements.

- **3.3 Platform:** Initial development for iOS and Android mobile operating systems.

## 4. Monetization Strategy

The app will operate on a "freemium" model. Core functionality will be free to encourage a large user base, with revenue generated through premium subscriptions.

### 4.1 Free Features

- Broadcasting and scanning with 32 criteria points.

- Radar interface with color-coded matches.

- Unlimited connection requests.

- Secure chat with mutual matches.

### 4.2 Premium Subscription ("ProxiMatch Plus")

- **4.2.1 Expanded Criteria:** Use up to 64 criteria points for a more refined and accurate matching algorithm.

- **4.2.2 Advanced Filters:** Filter the radar to only show users who meet a minimum match threshold (e.g., only show Green and Yellow matches).

- **4.2.3 "Second Look":** Get a daily/weekly summary of high-potential matches you passed by, giving you a second chance to connect.

- **4.2.4 Priority Beacon:** For a limited time, make your broadcast signal stronger, appearing on more users' radars in a crowded area.

## 5. Go-to-Market & Distribution Strategy

### 5.1 Target Audience & Phased Rollout

- **Initial Target:** Focus on hyper-dense, socially active communities where proximity is a natural advantage.
  
  - **Phase 1: University Campuses:** Target students during orientation/welcome week.
  
  - **Phase 2: Major Events:** Focus on attendees of music festivals, large concerts, and tech/creative conferences.
  
  - **Phase 3: Urban Hubs:** Expand to dense urban neighborhoods known for active social scenes.

- **Geographic Focus:** The launch will be city-by-city, starting with a single pilot city to prove the model before expanding.

### 5.2 Solving the "Cold Start" Problem

- **5.2.1 Event-Based Promotion:**
  
  - Partner with event organizers to be included in official event apps or promotional materials.
  
  - Run on-site promotions (e.g., booths, QR codes on posters) encouraging attendees to download the app for a chance to win prizes or unlock premium features.

- **5.2.2 Hyper-Local Marketing:**
  
  - Utilize geo-targeted social media ads aimed at users within specific campus boundaries or event venues.
  
  - Collaborate with local influencers and student organizations to generate buzz.

- **5.2.3 "Ignition Events":**
  
  - Host or sponsor small-scale events (e.g., mixers, silent discos) where the app is the primary tool for interaction. This guarantees a high density of active users in a controlled environment.

- **5.2.4 Viral Loop/Referral Program:**
  
  - Implement a "share with a friend" feature that rewards both the referrer and the new user with a temporary premium subscription (e.g., 7 days of "ProxiMatch Plus"). This encourages organic, peer-to-peer growth.~~~~
