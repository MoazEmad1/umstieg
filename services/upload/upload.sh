set -eu

: "${HF_DATASET:?set HF_DATASET in .env}"
: "${HF_TOKEN:?set HF_TOKEN in .env}"
ARCHIVE_DIR="${ARCHIVE_DIR:-/data/archive}"
RAW_DIR="${RAW_DIR:-/data/raw}"
CARD="${CARD:-/opt/upload/dataset-card.md}"

hf upload "$HF_DATASET" "$CARD" README.md --repo-type dataset --commit-message "Update dataset card" > /dev/null

check="$(mktemp -d)"
trap 'rm -rf "$check"' EXIT

find "$ARCHIVE_DIR" -name '*.manifest.json' | sort | while read -r manifest; do
    base="${manifest%.manifest.json}"
    archive="$base.tar.zst"
    name="$(basename "$base")"
    source="${name%-????-??-??}"
    date="${name#"$source"-}"
    year="${date%%-*}"
    repo_dir="$source/$year"

    if [ "$(echo "$date" | tr -d -)" -ge "$(date -u +%Y%m%d)" ]; then
        echo "$name is not over yet, skipping"
        continue
    fi

    for file in "$archive" "$manifest"; do
        hf upload "$HF_DATASET" "$file" "$repo_dir/$(basename "$file")" \
            --repo-type dataset --commit-message "Add $name" > /dev/null
        hf download "$HF_DATASET" "$repo_dir/$(basename "$file")" \
            --repo-type dataset --local-dir "$check" --quiet > /dev/null
        cmp -s "$file" "$check/$repo_dir/$(basename "$file")"
    done

    rm -f "$archive" "$manifest"
    rm -rf "$RAW_DIR/$source/$(echo "$date" | tr - /)"
    echo "$name uploaded and removed locally"
done
