# 0011: Free hosting with Azure for Students and Hugging Face

Status: Accepted · 2026-10-31

## Context
I'm a student and want this to cost nothing. The Hetzner plan from 0003 and 0010 comes to about €12–13 a month. Free servers are easy to get, but storage isn't: as single gzip files the raw feed is about 2.2 GB a day, and no free plan holds 66 GB a month.

On 2026-10-31 I fetched 10 production snapshots one minute apart. As single gzip files they took 15.0 MB. Packed into one tar and compressed with `zstd -19 --long=27`, they took 1.04 MB, and unpacking gave back every file byte for byte. Consecutive snapshots are nearly identical, which is why packing them together helps so much. If that holds for a whole day, it's about 130 MB a day. Ten minutes isn't a day, so I'll check it on the first full day.

## Decision
- The collector runs on an Azure for Students VM (sign-up with a uni email, no card, $100 credit for 12 months, a small B-series VM listed as free).
- It keeps writing `.pb.gz` files and sidecars as before (0003).
- Once a night, the previous UTC day of each source is packed into one `{source}-{date}.tar.zst` with a manifest of names, sizes and sha256 hashes. The archive is unpacked and checked against the manifest before anything is deleted. Packing runs inside the collector, using commons-compress for tar and zstd-jni for zstd.
- The archives go to a public Hugging Face dataset. VBB data is CC BY 4.0, so sharing it is allowed as long as VBB is credited in the dataset card.
- Local files are removed only after the upload is confirmed.
- CI builds the images and pushes them to GitHub's container registry, because the free VM has little memory for a Gradle build.
- This replaces the Hetzner VPS in 0003 and the object storage sync in 0010.

## Alternatives
- Hetzner VPS and Object Storage: works, but costs money every month.
- Oracle Cloud Always Free: needs a card, may reclaim a VM whose CPU stays under 20% for a week, and offers about 200 GB of disk.
- My own machine: free, but I don't want that much data on it.
- A university VM: only chairs and project groups can request one. Worth asking, but I can't wait for it.
- Cloudflare R2 or Backblaze B2: about 10 GB free.
- Zenodo: 50 GB per record, meant for publishing finished datasets rather than daily uploads. It's my fallback, as one versioned record a month.

## Consequences
After 12 months the Azure credit ends, and I need another server by then. Hugging Face calls free public storage "best-effort", so if they object I move to Zenodo. The public dataset becomes part of the project and has to say clearly where the data comes from and how it is packed. Packing a day costs CPU time on a small VM, which I haven't measured yet. If it's too slow, I'll use a lower zstd level.
