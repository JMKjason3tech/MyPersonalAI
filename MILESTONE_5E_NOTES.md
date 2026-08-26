# MyPersonalAI — Milestone 5E Conversation Intelligence

## Status

Implementation branch: `milestone-5e-conversation-intelligence`

## Scope

5E adds a deterministic conversation-context layer without replacing the existing 5D resolver.

### Implemented

- Explicit commands still resolve through `IntentResolver` first.
- Short follow-up phrases can reuse the previous actionable user intent.
- Settings follow-ups preserve the previous Settings destination.
- Device-information follow-ups preserve the previous information target.
- Explicit new intents always take precedence over conversation context.
- Unrelated text does not inherit an old intent.
- Contextual resolutions are converted to canonical commands before entering the existing tools, so 5E does not create a second execution path.
- Unit tests cover settings follow-ups, device-information follow-ups, explicit-intent precedence, stale-context rejection, and unsupported cross-intent follow-ups.

## Examples

```text
User: Open notification settings.
AI: [opens notification settings]
User: Open it.
AI: [repeats notification settings destination]
```

```text
User: Check my storage.
AI: [reports storage]
User: Show that again.
AI: [repeats storage information]
```

```text
User: Open notification settings.
AI: [opens notification settings]
User: Check my battery.
AI: [battery intent wins; no stale Settings context]
```

```text
User: Open notification settings.
AI: [opens notification settings]
User: Hello there.
AI: [UNKNOWN / normal fallback; no stale Settings inheritance]
```

## Intentionally deferred

The known 5D battery/device-information matching issue is not changed by this milestone. It remains a separate follow-up so that conversation-context work does not accidentally mask or alter the existing device-information matching rules.

## Verification required in Codespaces

```bash
./gradlew test
./gradlew assembleDebug
```

Real-device testing should verify at minimum:

1. `open notification settings`
2. `open it` after the first command
3. `check my battery`
4. `show that again` after a device-information response
5. an unrelated sentence between commands does not silently reuse stale intent
6. existing 5D Settings behavior remains unchanged
