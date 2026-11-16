#!/usr/bin/env bash
# Runs the Surv.Core NUnit tests with the user-local .NET SDK (PLAN.md, Odstępstwa).
set -euo pipefail
export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1
DOTNET="${DOTNET:-$HOME/.dotnet/dotnet}"
exec "$DOTNET" test "$(dirname "$0")/tests/Surv.Core.Tests/Surv.Core.Tests.csproj" "$@"
