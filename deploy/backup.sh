#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p backups
stamp=$(date -u +%Y%m%dT%H%M%SZ)
docker compose stop app
trap 'docker compose start app >/dev/null' EXIT
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob --default-character-set=utf8mb4 "$MYSQL_DATABASE"' > "backups/db-$stamp.sql"
docker compose run --rm --no-deps --entrypoint tar -T app -C /app/private-media -czf - . > "backups/media-$stamp.tar.gz"
test -s "backups/db-$stamp.sql"
test -s "backups/media-$stamp.tar.gz"
echo "Backup completed: $stamp. Copy both files to independent encrypted storage."
