#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf dist
mkdir -p dist/src dist/icons dist/images
cp index.html styles.css royal.css sw.js manifest.webmanifest dist/
cp src/engine.js src/ai.js src/main.js src/online-client.js src/bluetooth.js src/intl-extra.js dist/src/
cp icons/*.svg dist/icons/
cp images/*.svg dist/images/
touch dist/.nojekyll
test -s dist/index.html
test -s dist/royal.css
test -s dist/src/engine.js
test -s dist/icons/royal-crest.svg
test ! -e dist/server/index.js
test ! -e dist/.env
printf 'Staged public Baghchal web app in dist/\n'
