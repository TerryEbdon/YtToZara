/**
@file
@author Terry Ebdon
@brief This file configures the YtToZara app.
@author Terry Ebdon
@since v2.1.0
*/

version = 'v2.1.0'

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
