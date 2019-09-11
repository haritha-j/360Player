# 360Player

Based on the 360 player by Oculus. https://github.com/fbsamples/360-video-player-for-android

This player takes a 360 equirectangular video split into X x Y tiles (default 5 x 4) as input and stitches them together and plays them in a 360 player.

The input videos should be in the root of the phone storage labeled as output{i}.mp4.

i.e. output0.mp4, output1.mp4...output19.mp4 for 20 tiles.

X and Y can be set in the main activity.