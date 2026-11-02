#!/usr/bin/env bash
# Format all Java code with pinned google-java-format AOSP (4-space) style.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="1.28.0"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/labgear"
JAR="$CACHE_DIR/google-java-format-${VERSION}-all-deps.jar"

mkdir -p "$CACHE_DIR"
if [[ ! -s "$JAR" ]]; then
  TEMP_JAR="$(mktemp "$CACHE_DIR/.google-java-format.XXXXXX")"
  trap 'rm -f "$TEMP_JAR"' EXIT
  curl --fail --location --retry 3 --silent --show-error \
    "https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/${VERSION}/google-java-format-${VERSION}-all-deps.jar" \
    --output "$TEMP_JAR"
  mv "$TEMP_JAR" "$JAR"
fi

cd "$REPO_ROOT"
mapfile -d '' JAVA_FILES < <(find src/main/java src/test/java -type f -name '*.java' -print0 | sort -z)
if [[ "${#JAVA_FILES[@]}" -eq 0 ]]; then
  echo "No Java files found" >&2
  exit 1
fi

java \
  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
  -jar "$JAR" --aosp --replace \
  --skip-removing-unused-imports \
  --skip-reflowing-long-strings \
  --skip-javadoc-formatting \
  "${JAVA_FILES[@]}"
