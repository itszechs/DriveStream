package zechs.drive.stream.ui.player.utils

import android.content.res.Resources
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.util.MimeTypes


class CustomTrackNameProvider(
    resources: Resources
) : DefaultTrackNameProvider(resources) {

    override fun getTrackName(format: Format): String {
        var trackName = super.getTrackName(format)
        format.sampleMimeType?.let {
            val sampleFormat = formatNameFromMime(format.sampleMimeType)
            trackName += " ($sampleFormat)"
        }
        format.label?.let {
            if (!trackName.startsWith(it)) trackName += " - $it"
        }
        return trackName
    }

    private fun formatNameFromMime(
        mimeType: String?
    ) = when (mimeType) {
        MimeTypes.AUDIO_DTS -> "DTS"
        MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
        MimeTypes.AUDIO_DTS_EXPRESS -> "DTS Express"
        MimeTypes.AUDIO_TRUEHD -> "TrueHD"
        MimeTypes.AUDIO_AC3 -> "AC-3"
        MimeTypes.AUDIO_E_AC3 -> "E-AC-3"
        MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3-JOC"
        MimeTypes.AUDIO_AC4 -> "AC-4"
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_MPEG -> "MP3"
        MimeTypes.AUDIO_MPEG_L2 -> "MP2"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_ALAC -> "ALAC"
        MimeTypes.AUDIO_WAV -> "WAV"
        MimeTypes.AUDIO_AMR -> "AMR"
        MimeTypes.AUDIO_AMR_NB -> "AMR-NB"
        MimeTypes.AUDIO_AMR_WB -> "AMR-WB"
        MimeTypes.APPLICATION_PGS -> "PGS"
        MimeTypes.APPLICATION_SUBRIP -> "SRT"
        MimeTypes.TEXT_SSA -> "SSA"
        MimeTypes.TEXT_VTT -> "VTT"
        MimeTypes.APPLICATION_TTML -> "TTML"
        MimeTypes.APPLICATION_TX3G -> "TX3G"
        MimeTypes.APPLICATION_DVBSUBS -> "DVB"
        else -> null
    }
}