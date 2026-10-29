# Security Specification — Saiphai Auto Ticket Booking App

## 1. Data Invariants
1. **Authentication Gate**: Unauthenticated callers (`request.auth == null`) cannot read or write any document in any collection (`fares`, `drivers`, `bookings`, `reviews`, `admins`).
2. **Multi-User Ownership Isolation**: Every document in `fares`, `drivers`, `bookings`, and `reviews` is scoped to `userId` matching `request.auth.uid`. Cross-user reads, lists, updates, and deletes are strictly rejected unless the caller is a verified administrator (`isAdmin()`).
3. **Strict Schema & Size Bounds**: Every `create` and `update` invokes `isValidFareRoute`, `isValidDriverProfile`, `isValidBookingTicket`, or `isValidRideReview`, enforcing field types, enum allowlists, numeric bounds, and string `.size()` limits.
4. **Immutable Creation Metadata**: `userId` and `createdAt` (plus relational keys on reviews/bookings) are strictly immutable across all `update` operations.
5. **Terminal State Locking**: Once a booking reaches `status == 'cancelled'`, further client updates are blocked. Once `status == 'completed'`, only post-ride rating/payment completion flags (`passengerRated`, `driverRated`, `paymentStatus`, `updatedAt`) may be updated.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated List Scrape**: Unauthenticated `.collection("fares").get()` -> Rejected (`request.auth != null`).
2. **Unfiltered Query Scrape**: Authenticated `.collection("bookings").get()` without `.where("userId", "==", uid)` -> Rejected (`resource.data.userId == request.auth.uid`).
3. **Identity Spoofing on Create**: Creating a booking with `userId: "victim_uid"` while signed in as `"attacker_uid"` -> Rejected (`incoming().userId == request.auth.uid`).
4. **Shadow Field Injection on Create**: Creating a fare route with an extra field `isFree: true` -> Rejected (`incoming().keys().hasOnly(...)`).
5. **Shadow Field Injection on Update**: Updating a driver profile with an undeclared field `adminOverride: true` -> Rejected (`affectedKeys().hasOnly(...)`).
6. **Ownership Transfer Attack**: Updating `userId` on an existing booking to another user -> Rejected (`incoming().userId == existing().userId`).
7. **Timestamp Forgery (Future Timestamp)**: Creating a document with `createdAt` in the future (`> request.time`) -> Rejected (`incoming().createdAt <= request.time`).
8. **CreatedAt Mutation on Update**: Modifying `createdAt` during an update -> Rejected (`incoming().createdAt == existing().createdAt`).
9. **Terminal State Reversal**: Updating a `'cancelled'` booking back to `'in_progress'` -> Rejected by Terminal State Lock.
10. **Enum Poisoning**: Creating a booking with `status: "hacked_status"` or `paymentMode: "crypto"` -> Rejected by enum allowlist.
11. **Value Boundary / Denial-of-Wallet String Overflow**: Sending a 5,000-character `comment` in `reviews` or negative `fareAmount` -> Rejected by `.size() <= 500` and `fareAmount >= 10`.
12. **Unverified Admin Email Spoofing**: Attempting admin access with an unverified email token (`email_verified == false`) -> Rejected (`request.auth.token.email_verified == true`).
