@echo off
@setlocal
set ytcmd=\yt -q -x
set ytAudioOptions=-f 140 --extract-audio --audio-format mp3 --audio-quality 128K
%ytcmd% %ytAudioOptions% --print filename --no-simulate %* | tee playlist.txt 
