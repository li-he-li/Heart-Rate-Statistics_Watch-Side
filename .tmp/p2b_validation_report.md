# P2-B Validation Report

Generated at: 2026-03-12 20:28:22
Window start: 2026-03-12 19:17:50
Window end: 2026-03-12 20:28:22

## Environment
- Phone: WCWODI8T4HYDGYYT (real device)
- Wear: emulator-5554 (virtual watch)
- WS probe: ws://127.0.0.1:18080/heartrate via adb forward
- Known limitation: Wear emulator is not paired to phone for Data Layer node discovery (connectedNodes=0).

## Results
| Case | Status | Details |
|---|---|---|
| B1.3-A pause/resume | FAIL | alivePhoneHome=False aliveWearHome=True alivePhoneResume=False aliveWearResume=True crashPhone=0 crashWear=0 |
| B1.3-B process recreation | FAIL | alivePhoneRecreate=False aliveWearRecreate=True crashPhone=0 crashWear=0 |
| B4-1 ws disconnect/reconnect | PASS | Connect=true -> fail during disconnect -> reconnect=true; no crash. |
| B4-2 permission deny/regrant | FAIL | crashWearDenied=2 aliveWearAfterGrant=False crashWearAfterGrant=2 denyLogHits=4 |
| B4-3 service interruption (phone) | FAIL | wsStopped=False wsRecovered=System.Threading.Tasks.VoidTaskResult True alivePhoneRecovered=False crashPhone=0 |
| B4-4 data layer disconnected | FAIL | connectedNodes0=0 retryHits=0 crashWear=0 |
| B5.1 30-minute stability | PASS | durationMin=69.45 wearSamples=162 wearRetryHits=14 wsOk=16 wsFail=14 wearCrash=0 phoneCrash=0 wearAliveFinal=True phoneAliveFinal=True |
