#!/bin/bash
# Each recording whole, oozx (generated core, warm, round 1 of each JVM discarded) against Fuse,
# one thing at a time on the P-cores, with the clock and the thermal throttle counter alongside.
# Usage: rzx-vs-fuse.sh <dir with .rzx> <output .tsv>
RZX_DIR=$1; OUT=$(realpath $2)
PLUGINS=$HOME/detodo/spectrum/versions/oozx-plugins
FUSE=$HOME/detodo/spectrum/fuse/fuse-libretro
TH=/sys/devices/system/cpu/cpu0/thermal_throttle/package_throttle_total_time_ms
freq() { cat /sys/devices/system/cpu/cpu[0-3]/cpufreq/scaling_cur_freq | sort -n | tail -1; }
watch_start() { F=$(mktemp); ( while :; do freq >> $F; sleep 0.5; done ) & W=$!; }
watch_end() { kill $W; wait $W 2>/dev/null; GHZ=$(awk '{s+=$1} END {printf "%.2f", s/NR/1e6}' $F); rm $F; }
echo -e "game\temulator\tjvm\tround\tframes\tinstructions\tseconds\tghz\tthrottle_ms\tcap_khz\tnote" > $OUT
cd $PLUGINS
for rzx in $RZX_DIR/*.rzx; do
  game=$(basename $rzx .rzx)
  for jvm in 1 2; do
    watch_start
    lines=$(taskset -c 0-3 mvn -o -B -pl libs/rzx -am -Dsurefire.failIfNoSpecifiedTests=false test -Dtest=WholeRecordingTime -Doozx.measure=true -Doozx.rounds=4 -Doozx.rzx=$rzx 2>&1 | grep "^TOTAL")
    watch_end
    echo "$lines" | sed -E 's/TOTAL round ([0-9]+): ([0-9]+) frames, ([0-9]+) instrucciones, ([0-9.]+) s.*/\1 \2 \3 \4/' | while read r f i s; do
      echo -e "$game\toozx\t$jvm\t$r\t$f\t$i\t$s\t$GHZ\t$(cat $TH)\t$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq)\t" >> $OUT; done
  done
  for run in 1 2 3; do
    watch_start
    line=$(taskset -c 0-3 $FUSE/oozx/playrzx $FUSE/fuse_libretro.so $rzx 0 1000 2>&1)
    watch_end
    err=$(echo "$line" | grep -m1 "fuse: error" | sed 's/\t/ /g')
    echo "$line" | tail -1 | sed -E 's/.*frames=([0-9]+) seconds=([0-9.]+)/\1 \2/' | { read f s; echo -e "$game\tfuse\t-\t$run\t$f\t\t$s\t$GHZ\t$(cat $TH)\t$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq)\t$err" >> $OUT; }
  done
done
