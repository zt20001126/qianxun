#!/bin/sh
set -eu

REPO_ROOT="$(CDPATH= cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

ENV_FILE="deploy/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  printf '缺少 %s。请先从 deploy/.env.example 创建本地配置。\n' "$ENV_FILE" >&2
  exit 1
fi

BACKUP_DIR="${1:-backups}"
mkdir -p "$BACKUP_DIR"
umask 077
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_FILE="$BACKUP_DIR/qianxun-${TIMESTAMP}-$$.sql.gz"
TEMP_DUMP="$BACKUP_FILE.dump"

cleanup_incomplete_backup() {
  rm -f "$TEMP_DUMP" "$BACKUP_FILE"
}
trap cleanup_incomplete_backup EXIT
trap 'exit 1' HUP INT TERM

docker compose --env-file "$ENV_FILE" -f deploy/docker-compose.yml exec -T mysql \
  sh -ec 'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysqldump --single-transaction --quick --default-character-set=utf8mb4 --user="$MYSQL_USER" "$MYSQL_DATABASE"' \
  > "$TEMP_DUMP"

if [ ! -s "$TEMP_DUMP" ]; then
  printf '数据库导出文件为空，未生成备份。\n' >&2
  exit 1
fi
gzip -c "$TEMP_DUMP" > "$BACKUP_FILE"
rm -f "$TEMP_DUMP"
gzip -t "$BACKUP_FILE"
if [ ! -s "$BACKUP_FILE" ]; then
  printf '数据库备份文件为空：%s\n' "$BACKUP_FILE" >&2
  exit 1
fi
trap - EXIT HUP INT TERM
printf '数据库备份已生成并通过 gzip 完整性检查：%s\n' "$BACKUP_FILE"
