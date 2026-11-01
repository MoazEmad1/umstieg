set -eu

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/bin" "$work/hub" "$work/raw/production/2026/10/30" "$work/raw/production/2026/10/31" "$work/archive/production/2026"

cat > "$work/bin/hf" <<'FAKE'
set -eu
command="$1"; repo="$2"; shift 2
case "$command" in
    upload)
        mkdir -p "$(dirname "$HUB/$2")"
        cp "$1" "$HUB/$2"
        echo "upload $2" >> "$HUB/log" ;;
    download)
        path="$1"; shift
        while [ $# -gt 0 ]; do
            if [ "$1" = "--local-dir" ]; then dir="$2"; fi
            shift
        done
        mkdir -p "$(dirname "$dir/$path")"
        if [ -n "${CORRUPT:-}" ]; then echo broken > "$dir/$path"; else cp "$HUB/$path" "$dir/$path"; fi ;;
esac
FAKE
chmod +x "$work/bin/hf"

for day in 30 31; do
    echo data > "$work/raw/production/2026/10/$day/100000.pb.gz"
    echo "archive $day" > "$work/archive/production/2026/production-2026-10-$day.tar.zst"
    echo "{}" > "$work/archive/production/2026/production-2026-10-$day.manifest.json"
done
today="$(date -u +%Y/%m/%d)"
mkdir -p "$work/raw/production/$today" "$work/archive/production/${today%%/*}"
echo data > "$work/raw/production/$today/100000.pb.gz"
echo "archive today" > "$work/archive/production/${today%%/*}/production-$(date -u +%F).tar.zst"
echo "{}" > "$work/archive/production/${today%%/*}/production-$(date -u +%F).manifest.json"

run() {
    PATH="$work/bin:$PATH" HUB="$work/hub" HF_DATASET=test/data HF_TOKEN=x \
        ARCHIVE_DIR="$work/archive" RAW_DIR="$work/raw" CARD="$(dirname "$0")/dataset-card.md" \
        sh "$(dirname "$0")/upload.sh"
}

cp -R "$work/archive" "$work/archive-copy"
if CORRUPT=1 run > /dev/null 2>&1; then echo "FAIL: corrupt download was accepted"; exit 1; fi
test -f "$work/archive/production/2026/production-2026-10-30.tar.zst" || { echo "FAIL: archive removed after failed check"; exit 1; }
test -d "$work/raw/production/2026/10/30" || { echo "FAIL: raw day removed after failed check"; exit 1; }

run > /dev/null
test -f "$work/hub/README.md" || { echo "FAIL: no dataset card"; exit 1; }
cmp -s "$work/hub/production/2026/production-2026-10-30.tar.zst" "$work/archive-copy/production/2026/production-2026-10-30.tar.zst" || { echo "FAIL: 10-30 not uploaded"; exit 1; }
test -f "$work/hub/production/2026/production-2026-10-31.manifest.json" || { echo "FAIL: 10-31 manifest not uploaded"; exit 1; }
test ! -e "$work/raw/production/2026/10/30" || { echo "FAIL: raw 10-30 kept"; exit 1; }
test ! -e "$work/archive/production/2026/production-2026-10-31.tar.zst" || { echo "FAIL: archive 10-31 kept"; exit 1; }
test -d "$work/raw/production/$today" || { echo "FAIL: today's raw files removed"; exit 1; }
if grep -q "$(date -u +%F)" "$work/hub/log"; then echo "FAIL: today was uploaded"; exit 1; fi

echo "upload tests passed"
