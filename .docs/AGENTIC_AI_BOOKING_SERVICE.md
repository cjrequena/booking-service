# Agentic AI on Booking Service (CQRS + Event Sourcing)

## Abstract

This document describes an agentic AI architecture layered on top of an existing CQRS and Event Sourcing booking service. 
Rather than replacing the underlying system, AI agents provide a natural language interface that interprets user intent 
and orchestrates the appropriate commands and queries. The Model Context Protocol (MCP) serves as the bridge between 
agents and the booking service APIs, enabling agents to discover and invoke operations as tools. This approach preserves 
the integrity of domain logic, event sourcing, and projections while adding intelligent reasoning capabilities that simplify 
user interactions.

## Overview

Traditional booking systems require clients to know exactly which API endpoints to call and in what sequence. With agentic AI, 
users express their intent in natural language, and specialized agents handle the orchestration. Two primary agents operate in 
this architecture:

- **Command Agent**: Interprets write operations (create, place, confirm, cancel bookings) and determines the correct sequence of commands to execute based on user intent.
- **Query Agent**: Handles read operations, deciding which queries to run, combining results, and synthesizing responses to user questions.

Both agents connect to their respective MCP Servers, which expose the booking-command-handler and booking-query-handler REST APIs 
as standardized tools. 

The MCP Servers can be auto-generated from the existing OpenAPI specifications, minimizing implementation effort. 

The underlying CQRS architecture, event store, domain aggregates, and projections remain unchanged—agents simply provide 
an intelligent interface layer above them.

## Architecture with AI Agents

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         User / External System                           │
│                                                                         │
│   "Manage booking BK-123,             "How many cancelled bookings      │
│    the customer wants to change       did we have this week?"           │
│    their flight"                                                        │
└───────────────────┬─────────────────────────────┬───────────────────────┘
                    │  Intent (natural language     │
                    │  or high-level command)       │
                    ▼                               ▼
┌───────────────────────────────┐   ┌───────────────────────────────────┐
│       COMMAND AGENT           │   │           QUERY AGENT             │
│                               │   │                                   │
│  ┌─────────────────────────┐  │   │  ┌─────────────────────────────┐ │
│  │     LLM (Reasoning)     │  │   │  │     LLM (Reasoning)         │ │
│  │                         │  │   │  │                             │ │
│  │  - Interprets intent    │  │   │  │  - Interprets the question  │ │
│  │  - Decides which        │  │   │  │  - Decides which queries    │ │
│  │    commands to call     │  │   │  │    to run                   │ │
│  │    and in what order    │  │   │  │  - Combines results         │ │
│  │  - Handles errors       │  │   │  │  - Synthesises response     │ │
│  └──────────┬──────────────┘  │   │  └──────────────┬──────────────┘ │
│             │                  │   │                 │                 │
│    Available tools:            │   │    Available tools:              │
│  ┌──────────▼──────────────┐  │   │  ┌──────────────▼──────────────┐ │
│  │ • createBooking()       │  │   │  │ • getBookingById()          │ │
│  │ • placeBooking()        │  │   │  │ • listBookings()            │ │
│  │ • confirmBooking()      │  │   │  │ • searchByCriteria()        │ │
│  │ • cancelBooking()       │  │   │  └──────────────┬──────────────┘ │
│  │ • completeBooking()     │  │   │                 │                 │
│  │ • expireBooking()       │  │   └─────────────────┼─────────────────┘
│  └──────────┬──────────────┘  │                     │
└─────────────┼──────────────────┘                    │
              │                                        │
              │  via MCP                               │  via MCP
              ▼                                        ▼
┌─────────────────────────────┐      ┌────────────────────────────────┐
│   MCP Server (Command)      │      │     MCP Server (Query)         │
│                             │      │                                │
│  Exposes command API        │      │  Exposes query API             │
│  as MCP tools               │      │  as MCP tools                  │
│                             │      │                                │
│  Auto-generated from        │      │  Auto-generated from           │
│  OpenAPI /v3/api-docs ✅    │      │  OpenAPI /v3/api-docs ✅       │
└──────────────┬──────────────┘      └────────────────┬───────────────┘
               │  HTTP calls                           │  HTTP calls
               ▼                                       ▼
┌─────────────────────────────┐      ┌────────────────────────────────┐
│   booking-command-handler   │      │    booking-query-handler       │
│   (Write Side - Port 8080)  │      │    (Read Side  - Port 8081)    │
│                             │      │                                │
│  POST /bookings/create      │      │  GET /bookings/{id}            │
│  POST /bookings/place       │      │  GET /bookings/                │
│  POST /bookings/{id}/confirm│      │  GET /bookings/search          │
│  POST /bookings/{id}/cancel │      │                                │
│  POST /bookings/{id}/expire │      └────────────────┬───────────────┘
│                             │                       │
│  ┌─────────────────────┐   │                       │  Reads projections
│  │  Domain Aggregates  │   │                       ▼
│  │  Command Bus        │   │      ┌────────────────────────────────┐
│  └──────────┬──────────┘   │      │           MongoDB              │
│             │               │      │        (Projections)           │
│  ┌──────────▼──────────┐   │      └────────────────▲───────────────┘
│  │    Event Store      │   │                       │ Event sync
│  │    (PostgreSQL)     │◄──┼───────────────────────┘
│  └─────────────────────┘   │
└─────────────────────────────┘
```

---

## Key difference: deterministic vs. agent

```
                    TODAY (deterministic)
                    ─────────────────────
  Client            Command Handler         Event Store
    │                     │                     │
    │── POST /create ────►│                     │
    │                     │── persists ────────►│
    │◄── 201 Created ─────│                     │
    │                     │                     │
    │── POST /place ──────►│                    │
    │                     │── persists ────────►│
    │◄── 200 OK ───────────│                    │

    The client knows exactly what to call and in what order.


                    WITH AGENT + MCP
                    ────────────────
  User          Command Agent      MCP Server      Command Handler    Event Store
    │                │                 │                 │                │
    │── "Book a ────►│                 │                 │                │
    │   flight for   │ reasons...      │                 │                │
    │   John         │                 │                 │                │
    │   tomorrow"    │── createBooking()─►── POST /create ──►── persists ─►│
    │                │◄── bookingId ───────────────────────                │
    │                │                 │                 │                │
    │                │── placeBooking() ──► POST /place ───►── persists ─►│
    │                │◄── OK ──────────────────────────────               │
    │                │                 │                 │                │
    │◄── "Booking    │                 │                 │                │
    │    confirmed"──│                 │                 │                │

    The agent decides the sequence. The user only expresses intent.
```

---

## Why MCP?

MCP (Model Context Protocol) is the standard that defines how an agent discovers and calls tools in a consistent way. Instead of each agent having custom code to call your APIs, you define one **MCP Server per handler** — a thin layer that exposes your REST APIs as MCP tools.

```
Command Agent  ──MCP──►  MCP Server (Command)  ──HTTP──►  booking-command-handler
Query Agent    ──MCP──►  MCP Server (Query)    ──HTTP──►  booking-query-handler
```

### MCP vs. custom tools

| Option | When to use |
|---|---|
| **MCP Server** | Multiple agents or LLMs need to connect to the same services — your case |
| **Custom tools in code** | Agent lives in the same project and interoperability is not needed |

### Key advantage for your setup

Both handlers already expose OpenAPI specs at `/v3/api-docs`. There are tools that **auto-generate an MCP Server from an OpenAPI spec**, meaning you could have both MCP Servers running with almost no new code.

---

## What does NOT change

| Component | Status |
|---|---|
| Event Store (PostgreSQL) | ✅ Unchanged — remains the single source of truth |
| Domain Aggregates | ✅ Unchanged — business rules are not touched |
| Projections (MongoDB) | ✅ Unchanged — agents only read them as tools |
| Existing REST APIs | ✅ Unchanged — MCP Servers wrap them as tools |
| CQRS / Event Sourcing | ✅ Unchanged — agents live above this layer |

> The agents **do not replace** the architecture — they add a reasoning and intent layer on top of it. MCP is the standard bridge that connects them cleanly.
