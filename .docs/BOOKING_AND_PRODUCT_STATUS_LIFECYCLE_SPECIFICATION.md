# Booking and Product Status Lifecycle Specification

## Abstract

This document defines the **state model, lifecycle, and interaction rules** for **Booking** and **Product** entities in a booking system.

A **Booking** represents the overall reservation transaction created by a user.
A **Product** represents an individual service within the booking (e.g., transfer, hotel stay, excursion, etc.).

The system separates **BookingStatus** and **ProductStatus** to ensure clear domain boundaries:

* **BookingStatus** reflects the global state of the booking.
* **ProductStatus** reflects the lifecycle of each individual service.

The booking state is **derived from the collective states of its products**, ensuring consistent system behavior.

This specification supports:

* Human understanding of booking flows
* AI-driven validation, automation, and orchestration of booking state transitions.

---

# 1. Status Enumerations

## BookingStatus

```
CREATED
PROCESSING
CONFIRMED
PARTIALLY_CONFIRMED
CANCELLED
COMPLETED
EXPIRED
```

### Description

| Status              | Meaning                                   |
| ------------------- | ----------------------------------------- |
| CREATED             | Booking created but no processing started |
| PROCESSING          | Products are being placed with suppliers  |
| CONFIRMED           | All products confirmed                    |
| PARTIALLY_CONFIRMED | Some products confirmed and some rejected |
| CANCELLED           | Booking cancelled                         |
| COMPLETED           | Services completed                        |
| EXPIRED             | Booking expired due to timeout            |

---

## ProductStatus

```
PENDING
ACCEPTED
CONFIRMED
CANCELLED
REJECTED
```

### Description

| Status    | Meaning                                       |
| --------- | --------------------------------------------- |
| PENDING   | Product created and awaiting supplier request |
| ACCEPTED  | Supplier acknowledged request                 |
| CONFIRMED | Supplier confirmed the service                |
| CANCELLED | Product cancelled                             |
| REJECTED  | Supplier rejected request                     |

---

# 2. Booking Flow

The booking flow represents the **global progression of a reservation**.

Typical progression:

```
CREATED
   ↓
PROCESSING
   ↓
CONFIRMED
   ↓
COMPLETED
```

Alternative paths:

```
PROCESSING → PARTIALLY_CONFIRMED
PROCESSING → CANCELLED
PROCESSING → EXPIRED
```

---

# 3. Product Flow

The product flow represents the **supplier interaction lifecycle**.

Typical progression:

```
PENDING
   ↓
ACCEPTED
   ↓
CONFIRMED
```

Alternative paths:

```
ACCEPTED → REJECTED
ACCEPTED → CANCELLED
CONFIRMED → CANCELLED
```

---

# 4. Booking Life Cycle

The booking lifecycle defines **how a booking evolves over time**.

| Stage       | BookingStatus                   | Description                    |
| ----------- | ------------------------------- | ------------------------------ |
| Creation    | CREATED                         | Booking initialized            |
| Fulfillment | PROCESSING                      | Products being placed          |
| Resolution  | CONFIRMED / PARTIALLY_CONFIRMED | Supplier responses received    |
| Termination | CANCELLED / EXPIRED             | Booking cancelled or timed out |
| Completion  | COMPLETED                       | Services delivered             |

---

# 5. Product Life Cycle

The product lifecycle describes the **supplier response process**.

| Stage          | ProductStatus | Description               |
| -------------- | ------------- | ------------------------- |
| Initialization | PENDING       | Product created           |
| Acknowledgment | ACCEPTED      | Supplier accepted request |
| Confirmation   | CONFIRMED     | Service confirmed         |
| Failure        | REJECTED      | Supplier rejected request |
| Cancellation   | CANCELLED     | Product cancelled         |

---

# 6. Normal Successful Flow

### Scenario

All products are successfully confirmed.

| Step | BookingStatus | ProductStatus |
| ---- | ------------- | ------------- |
| 1    | CREATED       | PENDING       |
| 2    | PROCESSING    | PENDING       |
| 3    | PROCESSING    | ACCEPTED      |
| 4    | CONFIRMED     | CONFIRMED     |
| 5    | COMPLETED     | CONFIRMED     |

---

# 7. Rejected Product Flow

A product may be rejected by the supplier.

| Step | BookingStatus                    | ProductStatus |
| ---- | -------------------------------- | ------------- |
| 1    | CREATED                          | PENDING       |
| 2    | PROCESSING                       | ACCEPTED      |
| 3    | PARTIALLY_CONFIRMED or CANCELLED | REJECTED      |

Rules:

* If **all products rejected → CANCELLED**
* If **some confirmed and some rejected → PARTIALLY_CONFIRMED**

---

# 8. Partial Confirmation (Multi-Product Booking)

When multiple products exist, mixed outcomes may occur.

Example: Transfer + Tour

| Product  | Status    |
| -------- | --------- |
| Transfer | CONFIRMED |
| Tour     | REJECTED  |

Booking result:

```
BookingStatus = PARTIALLY_CONFIRMED
```

---

# 9. Cancellation Flow

Cancellation may be initiated by the user or system.

| Step | BookingStatus | ProductStatus |
| ---- | ------------- | ------------- |
| 1    | CREATED       | PENDING       |
| 2    | PROCESSING    | ACCEPTED      |
| 3    | CANCELLED     | CANCELLED     |

Cancellation cascades to all products.

---

# 10. Expired Booking

A booking may expire if the supplier does not respond in time.

| Step | BookingStatus | ProductStatus |
| ---- | ------------- | ------------- |
| 1    | CREATED       | PENDING       |
| 2    | PROCESSING    | PENDING       |
| 3    | EXPIRED       | PENDING       |

---

# 11. Visual Flow (Simplified)

## Booking Lifecycle

```
CREATED
   │
   ▼
PROCESSING
   │
   ├──► CONFIRMED
   │        │
   │        ▼
   │     COMPLETED
   │
   ├──► PARTIALLY_CONFIRMED
   │
   ├──► CANCELLED
   │
   └──► EXPIRED
```

---

## Product Lifecycle

```
PENDING
   │
   ▼
ACCEPTED
   │
   ├──► CONFIRMED
   │
   ├──► REJECTED
   │
   └──► CANCELLED
```

---

# 12. Important Rule (Recommended)

### Booking status must be derived from product statuses.

The booking state **must not be arbitrarily assigned**.
Instead it is computed using the following rules.

| Product State Combination | BookingStatus       |
| ------------------------- | ------------------- |
| All PENDING               | CREATED             |
| Any ACCEPTED              | PROCESSING          |
| All CONFIRMED             | CONFIRMED           |
| Mix CONFIRMED + REJECTED  | PARTIALLY_CONFIRMED |
| All REJECTED              | CANCELLED           |
| All CANCELLED             | CANCELLED           |

---

# 13. AI Specification Section

For AI-driven orchestration, the lifecycle rules can be expressed as structured data.

```yaml
booking_status_derivation_rules:

  - condition: all_products_confirmed
    result: CONFIRMED

  - condition: mix_confirmed_and_rejected
    result: PARTIALLY_CONFIRMED

  - condition: any_product_accepted
    result: PROCESSING

  - condition: all_products_rejected
    result: CANCELLED

  - condition: booking_timeout
    result: EXPIRED
```

---

# 14. Design Principles

1. **Separation of Concerns**

    * Booking represents transaction lifecycle
    * Product represents supplier lifecycle

2. **Derived State**

    * Booking status is computed from products

3. **Multi-Product Safety**

    * Supports partial confirmations

4. **Supplier Independence**

    * Product states represent external service behavior

---

# 15. Intended Use

This specification is intended for:

* Domain modeling
* Booking orchestration logic
* API documentation
* AI-driven validation and automation
* Integration with supplier systems
