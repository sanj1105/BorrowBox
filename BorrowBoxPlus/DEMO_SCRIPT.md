# BorrowBox Demo Script (5-7 minutes)

## Before you start (2 minutes, off camera)

1. Stop the app, delete the `data/` folder, and start `BorrowboxApplication`. This reseeds clean sample data (Asha, Ravi, Meena and five items).
2. Open http://localhost:8080/items in a fresh tab.
3. Pick dates in the future for every request so nothing is accidentally overdue. Below, "D+2" means two days from today, and so on.
4. Have IntelliJ's test panel (18 passed) and the H2 console ready in other tabs.
5. 
## Walkthrough

**1. Intro (30 sec)**
"BorrowBox replaces informal borrowing over messages with a structured workflow: listings, requests, approvals, and return tracking, with the rules enforced on the server."

**2. Browse and search (45 sec)** - acting as Asha
- Show the five items. Search "multi". Filter by category "Lab Equipment". Tick "Available only".
- Mention: data comes from a file-based H2 database.

**3. List an item and validation (45 sec)**
- Click **List an item**. Submit empty to show the validation messages.
- Add "Graphing Calculator", category "Stationery". It appears in the list.

**4. Request an item (45 sec)** - switch to Ravi
- Point out Ravi does not see a request form on his own items.
- Request Asha's Scientific Calculator, D+2 to D+5. You land on Requests with status PENDING.
- Try an end date before the start date, or a past start date, to show the error banner.

**5. Overlap prevention (60 sec)** - switch to Meena, then Asha
- As Meena, request the same calculator for D+4 to D+7 (overlapping).
- As Asha, open Requests. Approve Ravi's request. Then try to approve Meena's. The overlap error appears.
- Reject Meena's request.
- Say: "The conflict check lives in the service layer, so it cannot be bypassed from the UI."

**6. Loan lifecycle (45 sec)** - as Asha
- On Ravi's request click **Mark handed over** (ACTIVE), then **Mark returned** (RETURNED).
- Mention the allowed transitions: PENDING -> APPROVED -> ACTIVE -> RETURNED, or PENDING -> REJECTED. Skipping steps is rejected.

**7. Edit and availability (30 sec)**
- On Asha's item click **Edit**, change the description, save.
- Click **Mark unavailable**. The badge turns red. Switch to Ravi and show the request form is gone.

**8. Ownership check (30 sec)** - as Ravi
- Open `http://localhost:8080/items/1/edit` (an id of one of Asha's items). You are redirected with "Only the item owner can do this".
- Say: "Hiding buttons is not security, so ownership is checked in the service."

**9. Overdue (optional, 30 sec)**
- Show a screenshot of an overdue loan (ACTIVE plus red Overdue badge, tinted row), or backdate one in the H2 console.
- Explain: "Overdue is computed at display time from the end date, so there is no extra status or scheduled job."

**10. Tests and architecture (45 sec)**
- Show IntelliJ with 18 tests passing.
- Name the layers: controller, service (business rules), repository (Spring Data JPA), database.

**11. Limitations and next steps (30 sec)**
- No real login (user dropdown stands in for it), no notifications, no request cancellation.
- Next: Spring Security, email reminders, ratings, deployment.

## Likely questions

- **Why a service layer?** Rules stay in one tested place regardless of which controller calls them.
- **How is double-booking prevented?** On approval, the service queries APPROVED and ACTIVE requests for the item whose dates overlap and rejects the approval if any exist.
- **Is it secure?** Ownership and transitions are enforced server-side, but authentication is not implemented yet.
- **Why is overdue not a status?** It is derived data; storing it would let it go stale.
