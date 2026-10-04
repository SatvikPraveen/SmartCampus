#!/bin/sh
# Downloads the 24 public instances of the ITC-2007 post-enrolment course timetabling track
# (track 2, comp-2007-2-1.tim ... comp-2007-2-24.tim) and verifies their SHA-256 checksums.
#
# The files are not redistributed with this repository because the competition did not publish
# a licence for them. They are fetched from the organisers' site at Queen's University Belfast,
# falling back to the Internet Archive's 2010 snapshot of the original www.cs.qub.ac.uk site.
# Both sources serve byte-identical files (checked 2026-10-04).
#
# Usage: scripts/fetch-itc2007.sh [target-dir]      (default: data/itc2007)

set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST=${1:-"$ROOT/data/itc2007"}
PRIMARY="https://www.eeecs.qub.ac.uk/itc2007/postenrolcourse/initialdatasets"
ARCHIVE="https://web.archive.org/web/2010id_/http://www.cs.qub.ac.uk:80/itc2007/postenrolcourse/initialdatasets"

CHECKSUMS='
87610769d43f4e4e18509deced4177d474f97dae4582dcff16e4fc941c3e785c  comp-2007-2-1.tim
d2d3c89f87622ef68d7d522853f7ea3eb0a31f2a8ca2b647104dd57f8aa8a451  comp-2007-2-2.tim
9efa3f6b1fdc202a1d097a2c650936415143965b639981876ad0d4ce656e379c  comp-2007-2-3.tim
f501f74aa3c1d9ccbae493057ee991727d662cbef07d3559fa2d427a1e21ca8c  comp-2007-2-4.tim
1a74bdeec5458df3817b3867e44ab7b5007bb105a5d8e243b22da2d954892e50  comp-2007-2-5.tim
0df42123e0c6b0083da9d23939332efe3c01ef88bfe86f546b768a3525d3ad7c  comp-2007-2-6.tim
d3c4089c4d525969e8ba7650223f3863e48829be387333761e8a561e5a24b178  comp-2007-2-7.tim
71f5a98dd93a15bf2c604e2a5fe9d030e95df43c309cd0254af0d09f781adc18  comp-2007-2-8.tim
eaebe35326f95af14bdf270a04482c01a19cfd38158d2a90492d4942b0069380  comp-2007-2-9.tim
fc751f44c155714131f121ef813f2de4110665d772a26f33a573f5a8539e63db  comp-2007-2-10.tim
c2b41da8b9279618d06e8427273e6427ea9feacc2fb18e5bd561278ff93f0dc9  comp-2007-2-11.tim
50e31ab94dcc557d218f388a829a0a6da3f3d338000670e1da449a30cd3db529  comp-2007-2-12.tim
dcf10458bdb1f02ab9e85e21cd152d4b993e882043a76f26b9f9d8326b67d2e1  comp-2007-2-13.tim
db83f60d21a62d4ba04f11a15672bede7e97fca7236f509394f0f79e0f28e4b1  comp-2007-2-14.tim
c125d9fea17af5026a55910911d5e77cb18c0571b82fb2bbb781362c159d96df  comp-2007-2-15.tim
d09a0f5ff1a5488b44cd92ced9b541dd537320f5118eab3be48987515fb08821  comp-2007-2-16.tim
807210f791578345e4d671870f8fac6e8a6b421a77d981706211f4eaa3711f62  comp-2007-2-17.tim
0418810b38c18b62be0c453b2e14652087999cefe79ab23f2fcf2810d596f861  comp-2007-2-18.tim
a454043fa9ff3143a628732a4b9a9d79bbd2a223ccaa1d41ee92dd241a5805ae  comp-2007-2-19.tim
2c713c9051837cd066a7248b7dc72a391e5df21673dfd4b8c4e3b6597029da2e  comp-2007-2-20.tim
c50520a12b8afea70f6f260e8319392300c5f3923670e493ffd9f54a4d5bfef8  comp-2007-2-21.tim
9caef42a970953389b2a427db0bd9dcc0fbe72737560bca0d81e5a3e53e1dd6a  comp-2007-2-22.tim
d709d4fafce2db937a872b517ab8dbfc59716dd7ba07ca3af4161fc7f213771f  comp-2007-2-23.tim
285299e491a68a223303af6a3718c36b5dcc8ace21a3a3d7fc84f67b2e494142  comp-2007-2-24.tim
'

if command -v sha256sum >/dev/null 2>&1; then
    sha256() { sha256sum "$1" | cut -d' ' -f1; }
elif command -v shasum >/dev/null 2>&1; then
    sha256() { shasum -a 256 "$1" | cut -d' ' -f1; }
else
    echo "error: need sha256sum or shasum" >&2
    exit 1
fi

if command -v curl >/dev/null 2>&1; then
    download() { curl -fsSL --retry 3 -m 120 -o "$2" "$1"; }
elif command -v wget >/dev/null 2>&1; then
    download() { wget -q -T 120 -O "$2" "$1"; }
else
    echo "error: need curl or wget" >&2
    exit 1
fi

mkdir -p "$DEST"
failed=0
echo "$CHECKSUMS" | while read -r expected name; do
    [ -n "$name" ] || continue
    target="$DEST/$name"
    if [ -f "$target" ] && [ "$(sha256 "$target")" = "$expected" ]; then
        echo "ok (cached)  $name"
        continue
    fi
    ok=0
    for base in "$PRIMARY" "$ARCHIVE"; do
        if download "$base/$name" "$target.part" && [ "$(sha256 "$target.part")" = "$expected" ]; then
            mv "$target.part" "$target"
            echo "ok           $name  <- $base"
            ok=1
            break
        fi
        rm -f "$target.part"
    done
    if [ "$ok" -ne 1 ]; then
        echo "FAILED       $name (download failed or checksum mismatch)" >&2
        exit 1
    fi
done || failed=1

if [ "$failed" -ne 0 ]; then
    exit 1
fi
echo "All 24 ITC-2007 track 2 instances are in $DEST"
