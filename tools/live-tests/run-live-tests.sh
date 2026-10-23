#!/usr/bin/env bash
# Runs the opt-in live tests on a connected emulator/device.
#
#   tools/live-tests/run-live-tests.sh [TestClass ...]
#
# Set LOCAL_DATABASE_URL (a localhost Postgres) to also run the staff-area test.
# Prerequisites: a debug build pointed at a backend the device can reach (e.g. a local one:
#   ./gradlew installDebug installDebugAndroidTest -Pclinicai.env=dev), a running emulator, and Node 18+.
# It creates throw-away Clerk dev-instance users with `+clerk_test` addresses (no real e-mail sent).
set -euo pipefail
cd "$(dirname "$0")"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"

eval "$(node create-users.mjs)"

# Staff screens need a staff account, which can only be created in a LOCAL database (an invite row is inserted).
STAFF_ARGS=()
if [[ -n "${LOCAL_DATABASE_URL:-}" ]]; then
  eval "$(DATABASE_URL="$LOCAL_DATABASE_URL" node create-staff.mjs)"
  # The throw-away staff invite lives in the LOCAL database only; remove it when the run ends, however it ends.
  trap 'psql "$LOCAL_DATABASE_URL" -tAc "DELETE FROM \"StaffUser\" WHERE email = lower('"'"'$STAFF_EMAIL'"'"')" >/dev/null 2>&1 || true' EXIT
  # A real patient books through the API first, so the staff test can look for that exact appointment.
  eval "$(node book-for-staff.mjs)"
  STAFF_ARGS=(-e staffEmail "$STAFF_EMAIL" -e staffPassword "$STAFF_PASSWORD" -e bookedRef "$BOOKED_REF" -e bookedPatient "$BOOKED_PATIENT" -e bookedDoctor "$BOOKED_DOCTOR")
fi

CLASSES="${*:-com.example.clinicai.live.LiveAccountFlowTest com.example.clinicai.live.LiveBookingFlowTest com.example.clinicai.live.LiveStaffAreaTest}"
for cls in $CLASSES; do
  echo "=== $cls"
  "$ADB" shell am instrument -w \
    -e liveAuth true \
    -e qaEmail "$QA_EMAIL" -e qaPassword "$QA_PASSWORD" \
    -e resetEmail "$RESET_EMAIL" -e resetOldPassword "$RESET_OLD_PASSWORD" -e resetNewPassword "$RESET_NEW_PASSWORD" \
    "${STAFF_ARGS[@]}" \
    -e class "$cls" \
    com.example.clinicai.test/androidx.test.runner.AndroidJUnitRunner
done
