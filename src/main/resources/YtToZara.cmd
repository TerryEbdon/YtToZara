@echo off
@setlocal
set tee=%~dp0\bin\YtToZara.bat
set ytcmd=yt-dlp -q
set audioFormat=--audio-format mp3
set bitRate=--audio-quality 128K
set fileNameOptions=--print filename --output "%%(title)s.%%(ext)s"
set ytAudioOptions=-f 140 --extract-audio %audioFormat% %bitRate%
set json=--write-info-json
set commonArgs=%ytAudioOptions% %fileNameOptions% --no-simulate
set metadataOnly=%commonArgs% %json% --skip-download --progress %*
set tracksOnly=%commonArgs% %*
@REM %ytcmd% %ytAudioOptions% %fileNameOptions% %json% --no-simulate %* | %tee%
@REM %ytcmd% %ytAudioOptions% %fileNameOptions% %json% --no-simulate --skip-download %*
echo Downloading metadata:
%ytcmd% %metadataOnly%
echo Download tracks, via tee:
@REM @echo %ytcmd% %tracksOnly% | %tee%
%ytcmd% %tracksOnly% | %tee%
