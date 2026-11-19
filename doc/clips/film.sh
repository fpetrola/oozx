#!/bin/bash
# Films one clip of the real desk: ./film.sh <clip> <file to use> <jar of the application>
# Filmed at twice the size, each pixel as four: the colour of a video is kept at half its sharpness, and at twice
# the size that half is still a whole pixel of the Spectrum.
# A display of its own, a home of its own with real speed, the application, the camera, and then the hand.
set -e
clip=$1; file=$(realpath "$2"); jar=$(realpath "$3"); here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d); mkdir -p "$work/home/.oozx" "$here/out"
echo '{"speed":{"emulation":100,"fastLoading":true}}' > "$work/home/.oozx/config.json"
Xephyr :98 -screen 1280x800 -ac -noreset >/dev/null 2>&1 & screen=$!
trap 'kill $camera $desk $screen 2>/dev/null; rm -rf "$work"' EXIT
until [ -e /tmp/.X11-unix/X98 ]; do sleep 0.3; done
run="env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE DISPLAY=:98"
$run java -Duser.home="$work/home" -Dawt.robot.screenshotMethod=x11 -cp "$jar" "$here/Film.java" "$clip" "$file" "$here/mark.sh" "$work/rolling" \
  >"$here/out/desk.log" 2>&1 & desk=$!
sleep "${WARM:-40}"
$run ffmpeg -y -loglevel error -f x11grab -draw_mouse 1 -framerate 30 -video_size 1280x800 -i :98 \
  -vf scale=iw*2:ih*2:flags=neighbor -c:v libx264 -preset veryfast -crf 16 -pix_fmt yuv420p "$here/out/$clip.mp4" & camera=$!
sleep 1
touch "$work/rolling"
wait $desk || true
sleep 1
kill -INT $camera; wait $camera 2>/dev/null || true
echo "$here/out/$clip.mp4"
