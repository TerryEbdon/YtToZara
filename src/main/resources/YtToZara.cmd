@echo off
@setlocal
set tee=%~dp0\bin\YtToZara.bat
set ytcmd=\yt -q
set audioFormat=--audio-format mp3
set bitRate=--audio-quality 128K
set fileNameOptions=--print filename --output "%%(title)s.%%(ext)s"
set ytAudioOptions=-f 140 --extract-audio %audioFormat% %bitRate%
%ytcmd% %ytAudioOptions% %fileNameOptions% --no-simulate %* | %tee%
