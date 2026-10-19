#!/usr/bin/env sh
set -eu
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
maven_version=3.9.11
cache_dir="$project_dir/.tools"
maven_dir="$cache_dir/apache-maven-$maven_version"
if [ ! -f "$maven_dir/.ready" ]; then
  mkdir -p "$cache_dir" "$maven_dir"
  archive="$cache_dir/apache-maven-$maven_version-bin.tar.gz"
  source_url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$maven_version/apache-maven-$maven_version-bin.tar.gz"
  echo "Downloading Apache Maven $maven_version from Maven Central..."
  curl --fail --location --proto '=https' --tlsv1.2 "$source_url" -o "$archive"
  curl --fail --location --proto '=https' --tlsv1.2 "$source_url.sha512" -o "$archive.sha512"
  expected=$(awk '{print $1}' "$archive.sha512")
  if command -v sha512sum >/dev/null 2>&1; then actual=$(sha512sum "$archive" | awk '{print $1}'); else actual=$(shasum -a 512 "$archive" | awk '{print $1}'); fi
  if [ "$expected" != "$actual" ]; then echo "Maven checksum mismatch" >&2; exit 1; fi
  tar -xzf "$archive" -C "$maven_dir" --strip-components=1
  touch "$maven_dir/.ready"
fi
exec "$maven_dir/bin/mvn" "$@"
