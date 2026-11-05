#!/bin/bash
# Quick local typecheck of the iOS sources against the macOS SDK (for Macs without Xcode).
# The command-line tools lack SwiftUI's macro plugin, so @State is swapped for a stand-in
# in a temporary copy. The real iOS build runs on GitHub Actions (.github/workflows/ios.yml).
cd "$(dirname "$0")"
SDK=$(xcrun --show-sdk-path)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
cp -R App Shared Widgets "$TMP"/ && cp ../mac/Sources/*.swift "$TMP"/App/
find "$TMP" -name "*.swift" -exec sed -i '' 's/@State /@LocalState /g' {} +
cat > "$TMP/Shared/_LocalState.swift" <<'SWIFT'
import SwiftUI
@propertyWrapper struct LocalState<V>: DynamicProperty {
    private final class Box { var v: V; init(_ v: V) { self.v = v } }
    private let box: Box
    init(wrappedValue: V) { box = Box(wrappedValue) }
    init() where V: ExpressibleByNilLiteral { box = Box(nil) }
    var wrappedValue: V { get { box.v } nonmutating set { box.v = newValue } }
    var projectedValue: Binding<V> { Binding(get: { box.v }, set: { box.v = $0 }) }
}
SWIFT
check() { # folders
  (cd "$TMP" && swiftc -typecheck -sdk "$SDK" -target arm64-apple-macos26.0 -parse-as-library $(for d in "$@"; do ls "$d"/*.swift; done) 2>&1) \
    | grep -E "^[^ ].*error:" | sed "s#$TMP/##" | sort -u
}
echo "== App";     check App Shared
echo "== Widgets"; check Widgets Shared
