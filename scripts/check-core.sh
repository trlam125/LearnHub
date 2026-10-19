#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
./mvnw -q -DskipTests test-compile
java -cp target/classes:target/test-classes vn.learnhub.CoreSelfTest
java -cp target/classes:target/test-classes vn.learnhub.MediaRulesSelfTest
