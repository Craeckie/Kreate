#!/usr/bin/env bash
cd "$(dirname "$0")"
./gradlew --stop
./gradlew assembleGithubArm64ProdRelease --max-workers=2
apksigner sign --ks ../my-debug.jks --ks-key-alias my-key --ks-pass "pass:$1" --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled false --out my-app-signed.apk "composeApp/build/outputs/apk/githubArm64Prod/release/Kreate Fixed-arm64-v8a.apk"
