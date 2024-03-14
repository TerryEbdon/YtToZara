@echo off
setlocal enabledelayedexpansion

::by default, this batch file needs to be located in the same folder as ffmpeg.exe (typically the bin folder)
::otherwise, specify its full path, so that the batch file knows where the ffmpeg.exe binary is located:
set "ffmpeg=%~dp0ffmpeg.exe"

::set the audio bitrate for output files (192 kbps)... if you want a different quality, you can change this value
set "ab=192k"

::set the options to write ID3v1 and ID3v2.3 metadata, and to include album art in JPEG format
set "metadata_opts=-id3v2_version 3 -write_id3v1 1 -vcodec mjpeg -q:v 3 -huffman optimal"

if [%1] == [] echo Nothing to do. Did you forget something? & pause & exit /b

:start
echo %1

::by default, the destination file will have the same drive + path + name as the source (~dpn), but a .mp3 extension
::if the source file already has a .mp3 extension, however, then " (normalized)" will also be added to the name
::you can change these, but note that the .mp3 extension tells ffmpeg to encode the file as .mp3, so don't omit it
set "output_file=%~dpn1.mp3"
@REM if /i "%~x1"==".mp3" set "output_file=%~dpn1 (normalized).mp3"

::if you'd like to instead place the output files in a "Normalized" subfolder, uncomment this line:
set "output_file=%~dp1Normalized\%~n1.mp3"

::if the destination file already exists, it will be skipped... a message is printed, but the script doesn't pause
if exist "!output_file!" echo "!output_file!" already exists. & goto continue

if not exist "!output_file!\.." mkdir "!output_file!\.."
if not exist "!output_file!\.." echo Unable to find or create the output location. & pause & goto continue

::to use the (significantly faster) volume filter, instead of loudnorm, uncomment out the following "goto volume" line
::goto volume

::volume produces the same audio you'd get by using Audacity's amplify filter for max amplification without clipping
::loudnorm implements the EBU R128 loudness normalization algorithm
::note that the volume filter should work on most versions, but loudnorm may require a fairly recent version of ffmpeg

:loudnorm
::first pass - run ffmpeg and get stats, then loop through its output (stdout+stderr) line by line to parse the info
set "loudnorm_opts=loudnorm=I=-16:dual_mono=true:TP=-1.5:LRA=11:print_format=summary"
for /f "delims=" %%i in ('call "!ffmpeg!" -i %1 -filter:a !loudnorm_opts! -f null nul 2^>^&1') do (
  set "l=%%i"
  if "!l:~0,17!" == "Input Integrated:" set "i_i=!l:~20,6!"
  if "!l:~0,16!" == "Input True Peak:" set "i_tp=!l:~20,6!"
  if "!l:~0,10!" == "Input LRA:" set "i_lra=!l:~20,6!"
  if "!l:~0,16!" == "Input Threshold:" set "i_thr=!l:~20,6!"
  if "!l:~0,14!" == "Target Offset:" set "t_o=!l:~20,6!"
)

::second pass - pass values obtained from the first pass back in again as arguments for loudnorm using a linear filter
set "audio_filter=measured_I=!i_i!:measured_TP=!i_tp!:measured_LRA=!i_lra!:measured_thresh=!i_thr!:offset=!t_o!"
set "audio_filter=loudnorm=I=-16:TP=-1.5:LRA=11:!audio_filter: =!:linear=true"
goto second_pass

:volume
::first pass - run ffmpeg with the volumedetect filter to analyze the audio and get the max volume (in decibels)
for /f "delims=" %%i in ('call "!ffmpeg!" -i %1 -filter:a volumedetect -f null nul 2^>^&1') do (
  set "l=%%i"
  if "!l:~35,10!" == "max_volume" set "max_volume=!l:~47!"
)

::negate max_volume to get the maximum amplification that can be applied to the audio without causing clipping
set "max_volume=!max_volume: dB=!"
if not "!max_volume:~0,1!" == "-" set "amplify=-!max_volume!"
if "!max_volume:~0,1!" == "-" set "amplify=!max_volume:-=!"

::second pass - pass the negative of the value of max_volume back in as the amplification value for the volume filter
set "audio_filter="volume=!amplify!dB""
goto second_pass

:second_pass
echo ==^> "!output_file!"

::the ffmpeg output is suppressed... if you'd like to see it, remove ">nul 2>&1" from the command line
"!ffmpeg!" -n -i %1 !metadata_opts! -filter:a !audio_filter! -ab !ab! "!output_file!" >nul 2>&1

:continue
::shift the batch file's arguments and then see if there's anything left for us to do
shift
if not [%1] == [] goto start