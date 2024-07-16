@echo off
setlocal

set ytdlp="yt-dlp.exe"

if not exist "%~dp0bin"\%ytdlp% (
  echo.
  echo Installing %ytdlp%
  call "%~dp0\bin\YtToZara.bat" install-ytdlp "%~dp0bin"
)

if not exist "%~dp0bin"\%ytdlp% (
  echo ERROR: yt-dlp install failed
  goto :EOF
)

set/p url=YouTube playlist URL: 
if "%url%" neq "" (
  call "%~dp0"YtToZara.cmd "%url%"
) else (
  echo A YouTube playlist name is required.
)
