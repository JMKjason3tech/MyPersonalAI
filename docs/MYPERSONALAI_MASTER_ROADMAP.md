# MyPersonalAI — Master Project Roadmap

> Master planning document. This roadmap is the long-term source of truth for the intended evolution of MyPersonalAI. It is a plan, not a claim that every future milestone is already implemented.

## Current checkpoint

**Stable tested baseline:** `main` @ `a2ec2c8`

**Milestone 5D:** Settings + natural-language routing is considered complete for the current scope and has been physically tested on the Android device.

**Known deferred issue:** device-info/battery natural-language intent resolution still needs refinement. It must be fixed later without regressing Settings routing.

---

# THE END-STATE VISION

```text
                         ┌──────────────────────────────┐
                         │       MYPERSONALAI           │
                         │   Personal Android AI       │
                         └──────────────┬───────────────┘
                                        │
             ┌──────────────────────────┼──────────────────────────┐
             │                          │                          │
          TEXT                       VOICE                     FUTURE INPUT
             │                          │                          │
             └──────────────┬───────────┴──────────────────────────┘
                            ▼
                 ┌──────────────────────┐
                 │ Conversation Engine  │
                 │ context + state      │
                 └──────────┬───────────┘
                            ▼
                 ┌──────────────────────┐
                 │ Agent / Intent Layer │
                 │ understand + plan    │
                 └──────────┬───────────┘
                            ▼
                 ┌──────────────────────┐
                 │ Risk / Confirmation  │
                 │ safety decisions     │
                 └──────────┬───────────┘
                            ▼
                 ┌──────────────────────┐
                 │ Tool Registry/Router │
                 └──────────┬───────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
     Android Tools     Web/Research       Local Memory
          │                 │                  │
          └─────────────────┼──────────────────┘
                            ▼
                 ┌──────────────────────┐
                 │ Response Generation  │
                 └──────────┬───────────┘
                            │
                 ┌──────────┴──────────┐
                 ▼                     ▼
             Graphical UI          Voice/TTS
```

---

# ROADMAP

## FOUNDATION — Milestones 1–4

### 1 — Android Foundation
- Kotlin Android application
- Gradle Kotlin DSL
- command-line buildability
- minimal Compose foundation
- Git/GitHub baseline
- zero unnecessary permissions

### 2 — Conversation Architecture
- conversation state
- message model
- input/output flow
- deterministic UI foundation

### 3 — Tool Registry and Routing
- tool abstraction
- tool registration
- routing layer
- capability separation

### 4 — Risk and Confirmation
- deterministic risk classification
- confirmation gates
- safe execution model
- no hidden/root/exploit mechanisms

**Status: complete.**

---

# CAPABILITY LAYER — Milestone 5

## 5A–5B — Android Device Capabilities
- battery information
- charging state/source
- battery health/temperature/voltage/technology where Android exposes them
- network/Wi-Fi information
- storage information
- device information
- network speed testing
- Android capability adapters

## 5C — Android Settings
- open general Android Settings
- open specific Settings destinations
- notification Settings routing
- safe Android intent fallbacks

## 5D — Natural-Language Settings Intelligence
- `open/access/show/view/launch/navigate` vocabulary
- configuration synonyms
- deterministic intent classification
- specific-settings precedence over generic Settings
- notification-settings distinction
- prevent arbitrary sentences from becoming device-info requests

**Current status: 5D Settings scope complete and physically tested.**

### Deferred 5D follow-up
- repair battery/device-info natural-language resolution
- accept natural battery-status requests reliably
- reject accidental matches from unrelated sentences
- preserve all currently working Settings behavior

---

# INTELLIGENCE LAYER

## 5E — Conversation Intelligence

**Goal:** make the assistant understand conversation rather than treating every input as an isolated command.

### Features
- conversational context
- follow-up requests
- references such as "that", "it", "the previous one"
- clarification questions
- intent continuity
- command/result awareness
- multi-turn interaction state
- deterministic fallback when intent is ambiguous

### Example
```text
User: Open Wi-Fi settings.
AI: Wi-Fi settings opened.
User: Now tell me my battery status.
AI: [battery capability]
```

### Acceptance
- context survives normal turns
- unrelated requests do not inherit stale intent
- ambiguous commands request clarification
- existing 5D routing remains intact

---

# VOICE LAYER

## 5F — Voice Input

**Goal:** convert spoken language into the same input pipeline used by text.

```text
Microphone → Speech Recognition → Text → Conversation Engine
                                      │
                                      ▼
                               Existing 5D Resolver
```

### Features
- microphone permission handled explicitly
- start/stop listening
- speech-to-text
- interim/final recognition state
- recognition errors
- offline/online capability awareness where supported
- cancellation
- no duplicate command submission
- voice input must reuse the existing conversation/intent pipeline

### Acceptance
- user can speak a command
- recognized text appears in conversation
- the same typed command and spoken command produce the same intent
- permissions and failures are clearly communicated

---

## 5G — Voice Output / TTS

**Goal:** allow the assistant to speak responses safely and naturally.

### Features
- Android Text-to-Speech abstraction
- speak/stop/pause/resume
- voice availability detection
- configurable speech rate/pitch
- output only after the assistant response is finalized
- prevent accidental duplicate speech
- respect user settings

### Architecture
```text
Assistant Response
       │
       ▼
   Response Model
       │
       ▼
      TTS
       │
       ▼
 Android Audio
```

---

# GRAPHICAL EXPERIENCE

## 5H — Assistant Graphical UI

**Goal:** transform the minimal Compose interface into a polished, highly visible and interactive personal-assistant application.

### Main screen
```text
┌──────────────────────────────────────────────┐
│  MyPersonalAI                         ⚙      │
│  ● Ready                                     │
├──────────────────────────────────────────────┤
│                                              │
│  AI                                          │
│  ┌────────────────────────────────────────┐  │
│  │ Hello. How can I help you today?       │  │
│  └────────────────────────────────────────┘  │
│                                              │
│                        YOU                   │
│  ┌────────────────────────────────────────┐  │
│  │ Open notification settings             │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  AI                                          │
│  ┌────────────────────────────────────────┐  │
│  │ Notification settings are open.        │  │
│  └────────────────────────────────────────┘  │
│                                              │
├──────────────────────────────────────────────┤
│  Type a message...                  🎙  ➤   │
└──────────────────────────────────────────────┘
```

### UI goals
- modern Compose UI
- strong visual hierarchy
- chat bubbles
- assistant/user distinction
- typing/listening/thinking/executing states
- microphone interaction
- send interaction
- scrollable conversation
- loading/progress states
- error/clarification cards
- action/result cards
- accessible controls
- responsive layout
- light/dark appearance support

### Dedicated Settings UI
- appearance
- voice input
- voice output
- speech rate/pitch
- AI provider configuration
- permissions/capabilities
- confirmation behavior
- privacy/security controls
- conversation/memory controls
- about/version information

**Important:** UI is a presentation layer over the existing architecture. It must not duplicate agent/tool logic.

---

# RESEARCH AND KNOWLEDGE

## 5I — Web / Research

**Goal:** give the assistant controlled access to current external information.

### Features
- web/research abstraction
- search tool
- page retrieval
- source extraction
- source-aware answers
- current-information handling
- result summarization
- explicit network failure handling
- source/citation support in the UI

### Safety
- no arbitrary dangerous actions from web content
- external instructions are untrusted data
- preserve risk/confirmation boundaries
- no credential exfiltration or hidden browser automation

---

# MEMORY AND PERSONALIZATION

## 5J — Local Memory

**Goal:** make the assistant capable of retaining useful information across conversations while preserving user control.

### Features
- local persistence
- conversation history
- explicit memories
- memory categories
- add/edit/delete memory
- search/retrieval
- memory consent/control
- clear-all memory
- privacy-aware storage

### Rule
The assistant should distinguish:
- current conversation context
- saved user memory
- temporary task state

It must not silently turn every conversation detail into permanent memory.

---

# ACTION AND AUTOMATION LAYER

## 6 — Multi-Step Agent Tasks

**Goal:** move from one-command execution toward controlled multi-step tasks.

### Features
- task planning
- tool sequencing
- intermediate state
- execution progress
- failure recovery
- confirmation before risky steps
- cancellation
- deterministic execution boundaries

Example:
```text
User request
    ↓
Understand
    ↓
Plan steps
    ↓
Ask confirmation if required
    ↓
Execute Tool A
    ↓
Execute Tool B
    ↓
Verify result
    ↓
Report outcome
```

---

## 7 — Android Automation / Expanded Capabilities

Add useful documented Android capabilities incrementally.

Potential capability groups:
- alarms/timers
- calendar
- reminders
- contacts
- media controls
- app launching
- sharing
- navigation intents
- files/documents where permitted
- connectivity controls where Android allows them

Every capability must use documented Android APIs and the least privilege necessary.

---

# PERSONAL ASSISTANT LAYER

## 8 — Proactive Assistant

**Goal:** allow useful proactive behavior without becoming intrusive.

### Features
- reminders
- scheduled tasks
- task status
- notifications
- recurring tasks
- background-safe execution
- user-controlled quiet hours
- explicit permission and opt-in behavior

No covert monitoring or hidden background behavior.

---

# AI PROVIDER AND REASONING LAYER

## 9 — AI Provider Abstraction

**Goal:** separate the assistant architecture from any single AI provider.

### Features
- provider interface
- configurable provider
- local/mock provider for tests
- remote AI provider adapters
- streaming responses where supported
- error/rate-limit handling
- token/context management
- model configuration

The agent architecture must remain usable without hard-coding one provider into Android UI code.

---

# SECURITY / RELIABILITY / PRODUCTION

## 10 — Security and Privacy Hardening

- least-privilege permissions
- secure API-key handling
- no secrets in source control
- encrypted sensitive local data where appropriate
- audit/logging strategy
- safe tool execution
- prompt-injection resistance for web content
- confirmation policy verification
- input validation
- error isolation

## 11 — Production Reliability

- unit tests
- integration tests
- Android instrumentation tests where useful
- deterministic mocks
- regression suite for every milestone
- lifecycle/rotation/process-death testing
- offline/network failure testing
- permission-denial testing
- accessibility testing
- performance profiling
- crash handling

## 12 — Final Assistant UX / Release Polish

- complete visual design system
- polished animations used only where helpful
- empty/loading/error/success states
- onboarding
- permissions education
- settings polish
- accessibility
- localization readiness
- app icon/branding
- release build configuration
- privacy documentation
- user-facing help/about

---

# FINAL END STATE

```text
                         MYPERSONALAI
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
      INPUT                 BRAIN                OUTPUT
        │                     │                     │
   ┌────┴────┐        ┌───────┴────────┐       ┌────┴────┐
   │         │        │                │       │         │
  TEXT     VOICE   Conversation      Agent   TEXT      VOICE
                     Context         Planner  UI        TTS
                         │             │
                         └──────┬──────┘
                                ▼
                         Risk / Confirmation
                                │
                                ▼
                           Tool Router
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
          Android            Web/Research       Memory
          Tools              Tools              Store
             │                  │                  │
             └──────────────────┼──────────────────┘
                                ▼
                         Result Verification
                                │
                                ▼
                       Response + Explanation
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
                Graphical UI             Voice
```

---

# NON-NEGOTIABLE PROJECT RULES

1. Kotlin remains the primary language.
2. The project must remain command-line Gradle buildable.
3. Android Studio is not required for the build workflow.
4. Use documented Android APIs.
5. Use least-privilege permissions.
6. No root mechanisms.
7. No hidden APIs.
8. No exploit mechanisms.
9. No covert surveillance.
10. Dangerous/destructive actions require deterministic confirmation.
11. UI must remain separated from agent/tool execution logic.
12. Voice input must feed the same conversation/intent pipeline as text.
13. New milestones must preserve previously verified behavior.
14. A milestone is not considered physically complete until the APK is tested on the real Android device when device behavior is involved.
15. ZIP handoffs use short, version-oriented filenames.
16. The user's Codespaces commit remains the authoritative project progression; generated ZIPs are handoff artifacts.
17. Known deferred bugs must be recorded and revisited rather than silently forgotten.

---

# CURRENT BACKLOG

### Deferred from 5D
- [ ] Battery status natural-language resolution
- [ ] Battery health query resolution
- [ ] Device-info phrase classification
- [ ] Reject accidental device-info matches while preserving valid short commands

### Future enhancement
- [ ] Conversation Intelligence
- [ ] Voice Input
- [ ] Voice Output / TTS
- [ ] Graphical Assistant UI
- [ ] Web / Research
- [ ] Local Memory
- [ ] Multi-step Agent Tasks
- [ ] Expanded Android Capabilities
- [ ] Proactive Automation
- [ ] AI Provider Abstraction
- [ ] Security/Privacy Hardening
- [ ] Production Reliability
- [ ] Final UX / Release Polish

---

## Definition of Done for the whole project

MyPersonalAI is complete when it is a reliable, visually polished Android personal assistant that can accept text and voice, understand conversational context, safely execute documented Android capabilities, research current information, retain user-controlled memory, perform controlled multi-step tasks, speak responses, and provide a coherent graphical interface — while preserving deterministic safety, least privilege, privacy, testability, and command-line buildability.
