# 0010: Copy raw files to object storage with rclone

Status: Superseded by [0011](0011-free-hosting-azure-hugging-face.md) · 2026-10-31

## Context
The collector writes about 2.2 GB of raw files a day on a small VPS. The disk fills up in a few weeks, and if the VPS dies, the files are gone. They are the only copy of history I can't download again (0003).

## Decision
- Once a day, rclone copies new files from `data/raw` to a bucket on Hetzner Object Storage, same provider as the VPS.
- It only copies files older than 5 minutes and skips `.tmp` files, so it never uploads a file that is still being written.
- It runs with `--immutable`: an existing remote file is never replaced. Raw files never change, so a difference means something is wrong.
- After copying, files older than 7 days are moved, which removes them locally once rclone has checked they are in the bucket. Nothing is ever deleted from the bucket.
- rclone runs from the official image as a compose service. The bucket and keys live only in `.env`.

## Alternatives
- aws cli: works with any S3 endpoint, but has no built-in Hetzner settings, and `sync` either deletes or keeps everything, with no age filter.
- A bigger disk: cheaper to start with, but no copy off the machine.
- Upload from the collector itself: more Java code and a new dependency for something a standard tool does.

## Consequences
Bucket storage grows by about 66 GB a month, which fits in the base price for over a year. On the VPS a systemd timer runs it, and a failed run sends a Telegram message. The VPS keeps a week of files, so I have that long to fix a broken sync.
