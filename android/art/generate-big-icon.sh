#!/bin/sh
set -e -u

echo "Generating ~/alpine-icons/ic_launcher.png..."
mkdir -p ~/alpine-icons/

vector2svg ../app/src/main/res/drawable/ic_launcher.xml ~/alpine-icons/ic_launcher.svg

sed -i "" 's/viewBox="0 0 108 108"/viewBox="18 18 72 72"/' ~/alpine-icons/ic_launcher.svg

SIZE=512
rsvg-convert \
	-w $SIZE \
	-h $SIZE \
	-o ~/alpine-icons/ic_launcher_$SIZE.png \
	~/alpine-icons/ic_launcher.svg

rsvg-convert \
	-b black \
	-w $SIZE \
	-h $SIZE \
	-o ~/alpine-icons/ic_launcher_square_$SIZE.png \
	~/alpine-icons/ic_launcher.svg
