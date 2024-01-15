@echo off
@setlocal
set tee=groovy YtToZara.groovy
set ytcmd=\yt -q
set audioFormat=--audio-format mp3
set bitRate=--audio-quality 128K
set ytAudioOptions=-f 140 --extract-audio %audioFormat% %bitRate%
%ytcmd% %ytAudioOptions% --print filename --no-simulate %* | %tee%
