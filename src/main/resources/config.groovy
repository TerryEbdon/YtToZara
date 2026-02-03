/**
@file
@brief This file configures the YtToZara app.
@author Terry Ebdon
*/

version = 'v2.1.4'

silenceRemove {
  enabled        = true
  startPeriods   = 1
  startSilence   = 0.5
  stopSilence    = 0.5
  startThreshold = '-26dB'
  startDuration  = 0
  stopDuration   = 1
  detection      = 'peak'
}

normalise {
  enabled                  = true
  integratedLoudnessTarget = -13   // LUFS (Loudness Units Full Scale)
}
