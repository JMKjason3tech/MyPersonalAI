# MyPersonalAI — Milestone 5D: Natural-Language Routing Upgrade

Base: Milestone 5C Settings implementation (`b58d8fe01d33b66cad7adfc717d261bd343f9f07`).

## Purpose
Move settings/device-information routing away from a small hard-coded trigger list toward a deterministic intent resolver that separates action language from the target.

## Changes
- Added `IntentResolver` with explicit intents: `OPEN_SETTINGS`, `DEVICE_INFO`, `UNKNOWN`.
- Added natural action vocabulary for navigation and configuration: open, access, show, view, launch, navigate, change, modify, configure, adjust, manage, customize, edit, control, set up, alter, update.
- Added target vocabulary for Wi-Fi, Bluetooth, battery, display, sound, location, storage, apps, notifications, accessibility, date/time, security and network.
- Added device-information targets for device info, network status and speed-test phrases.
- Removed the broad `keyword -> DeviceInfo` fallback behavior from `MockAgentEngine`.
- Unknown/ambiguous sentences now receive a clarification-style response instead of being routed to Device Info.
- Added unit tests covering configuration synonyms, navigation synonyms, arbitrary-word fallbacks, simple device targets and information requests.
- Changed notification Settings routing to prefer `Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS` on API 33+ so a general notification-app list is requested instead of MyPersonalAI's own notification page. The older notification action remains a fallback.

## Current limitation
This is still deterministic local routing. It is an intentional foundation for the later AI-provider/web-research layer; it is not yet the final semantic/LLM-based assistant.

## Testing required on the real phone
After building this branch, test:
- `Modify my Wi-Fi settings`
- `Configure my wireless connection`
- `Adjust my battery settings`
- `Manage my notification settings`
- `Customize my display`
- `Change my sound settings`
- `hello battery`
- `something about storage for me`
- `check my battery`
- `how much storage do I have`
- `what is my network status`
- `device info`
- `speed test`

Also repeat the original 12 Settings destinations, with special attention to general notification settings.
