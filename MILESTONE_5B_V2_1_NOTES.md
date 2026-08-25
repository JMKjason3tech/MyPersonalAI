# MyPersonalAI — Milestone 5B v2.1

This patch is applied on top of `MyPersonalAI-Milestone5-Completion-v2.zip`.

## Changes

### Wi-Fi / SSID handling
- Stops assuming a missing SSID always means the location permission is missing.
- Distinguishes:
  - location permission not granted
  - location services disabled
  - Android did not provide the SSID
- Preserves graceful degradation when Android restricts SSID access.

### Network estimates
- Converts Android `linkDownstreamBandwidthKbps` / `linkUpstreamBandwidthKbps` to Mbps using floating-point division so fractional values such as `108.4 Mbps` are preserved.
- Labels these values as **estimated link download/upload**, not measured Internet speed.
- Keeps the separate `speed test` command for actual measured download/upload/latency.

### Number formatting
- Centralizes decimal formatting in `DeviceInfoTool`.
- Uses `Locale.US` so the decimal separator is consistently `.` regardless of the phone's locale.
- Battery voltage is displayed as volts with decimals plus the original mV value.
- Storage, refresh rate, estimated network rates, and speed-test results use the same stable formatting path.

### Tests
- Added tests for fractional network estimates.
- Added a test proving an unavailable SSID can report a specific reason rather than incorrectly claiming the permission is missing.

## Files in this patch
- `app/src/main/java/com/jason/mypersonalai/android/capabilities/DeviceInfo.kt`
- `app/src/main/java/com/jason/mypersonalai/android/adapters/AndroidNetworkInfoProvider.kt`
- `app/src/main/java/com/jason/mypersonalai/tools/impl/DeviceInfoTool.kt`
- `app/src/test/java/com/jason/mypersonalai/tools/impl/DeviceInfoToolTest.kt`
