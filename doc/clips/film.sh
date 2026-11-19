#!/bin/bash
# Films one clip of the real desk: ./film.sh <clip> <file to use> <jar of the application>
# Filmed at twice the size, each pixel as four: the colour of a video is kept at half its sharpness, and at twice
# the size that half is still a whole pixel of the Spectrum.
# A display of its own, a home of its own with real speed, the application, the camera, and then the hand.
set -e
clip=$1; file=$(realpath "$2"); jar=$(realpath "$3"); here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d); mkdir -p "$work/home/.oozx" "$here/out"
# Real speed, tapes loaded fast, and the desk filling the display.
# A board that needs ROMs of its own finds them in ROMS, named NAME=file:file (the board's settings key), copied in.
roms=""
for board in $ROMS; do
  key=${board%%=*}; list=""
  for rom in $(echo "${board#*=}" | tr ':' ' '); do
    mkdir -p "$work/home/.oozx/roms"; cp "$rom" "$work/home/.oozx/roms/"
    list="$list${list:+,}\"$work/home/.oozx/roms/$(basename "$rom")\""
  done
  roms="$roms${roms:+,}\"$key\":[$list]"
done
echo '{"speed":{"emulation":100,"fastLoading":true},"desk":{"x":0,"y":0,"width":1280,"height":800},"roms":{"files":{'"$roms"'}}}' > "$work/home/.oozx/config.json"
# The machine's sound goes to an output of its own, which nobody hears and the camera records.
pw-cli ls Node 2>/dev/null | grep -q 'node.name = "oozx-clip"' || pw-cli create-node adapter \
  '{ factory.name=support.null-audio-sink node.name=oozx-clip media.class=Audio/Sink object.linger=true audio.position=[FL FR] }' >/dev/null
Xephyr :98 -screen 1280x800 -ac -noreset >/dev/null 2>&1 & screen=$!
trap 'kill $camera $recorder $desk $screen 2>/dev/null; rm -rf "$work"' EXIT
until [ -e /tmp/.X11-unix/X98 ]; do sleep 0.3; done
run="env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE DISPLAY=:98"
PIPEWIRE_NODE=oozx-clip $run java -Duser.home="$work/home" -Dawt.robot.screenshotMethod=x11 -cp "$jar" "$here/Film.java" "$clip" "$file" "$here/mark.sh" "$work/rolling" \
  >"$here/out/desk.log" 2>&1 & desk=$!
sleep "${WARM:-40}"
$run ffmpeg -y -loglevel error -f x11grab -draw_mouse 1 -framerate 30 -video_size 1280x800 -i :98 \
  -vf scale=iw*2:ih*2:flags=neighbor -c:v libx264 -preset veryfast -crf 16 -pix_fmt yuv420p "$work/picture.mp4" & camera=$!
ffmpeg -y -loglevel error -f pulse -i oozx-clip.monitor "$work/sound.wav" & recorder=$!
sleep 1
touch "$work/rolling"
wait $desk || true
sleep 1
kill -INT $camera $recorder; wait $camera $recorder 2>/dev/null || true
# Picture and sound were taken side by side; they are joined here, cut to the shorter of the two.
ffmpeg -y -loglevel error -i "$work/picture.mp4" -i "$work/sound.wav" -c:v copy -c:a aac -b:a 128k -shortest "$here/out/$clip.mp4"
echo "$here/out/$clip.mp4"
