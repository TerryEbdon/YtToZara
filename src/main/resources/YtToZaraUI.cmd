@echo off
setlocal
set/p url=YouTube playlist URL: 
if "%url%" neq "" (
  call "%~dp0"YtToZara.cmd "%url%"
) else (
  echo A YouTube playlist name is required.
)
