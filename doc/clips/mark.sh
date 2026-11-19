#!/bin/bash
# There is no window manager in the filming display, and a drag looks for the windows one would have marked: mark them.
for window in $(xwininfo -display :98 -root -children | awk '/^ +0x/{print $1}'); do
  xprop -display :98 -id "$window" -f WM_STATE 32c -set WM_STATE "1, 0" 2>/dev/null || true
done
