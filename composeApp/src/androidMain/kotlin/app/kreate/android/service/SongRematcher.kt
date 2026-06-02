package app.kreate.android.service

import app.kreate.database.models.Song
import app.kreate.util.toDuration
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlin.math.abs
import kotlin.time.Duration

/**
 * Searches YouTube Music for a replacement when a song's stored videoId
 * becomes unplayable (e.g. YouTube retires/remaps the canonical videoId).
 *
 * Scoring produces one of two confidence levels:
 *  - [Confidence.STRONG]: safe to swap silently.
 *  - [Confidence.WEAK]:   needs user confirmation via [RematchConfirmDialog].
 */
object SongRematcher {

    enum class Confidence { STRONG, WEAK }

    data class Match(
        val item: Innertube.SongItem,
        val confidence: Confidence
    )

    // --- Text normalisation (mirrors SongMatchingDialog.filteredText) ----------

    /**
     * Strips noise tokens that differ between YouTube upload variants
     * (official video, lyrics, vevo, hd, etc.) so that title/artist
     * matching is not confused by these suffixes.
     *
     * Shared with [it.fast4x.rimusic.ui.components.themed.SongMatchingDialog]
     * — keep the two in sync.
     */
    fun filteredText( text: String ): String =
        text.lowercase()
            .replace("(", " ")
            .replace(")", " ")
            .replace("-", " ")
            .replace("lyrics", "")
            .replace("vevo", "")
            .replace(" hd", "")
            .replace("official video", "")
            .filter { it.isLetterOrDigit() || it.isWhitespace() || it == '\'' || it == ',' }
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Token set from a normalised string (for Jaccard overlap). */
    private fun tokens( text: String ): Set<String> =
        filteredText( text ).split(" ").filter { it.isNotBlank() }.toSet()

    /** Jaccard similarity: |A∩B| / |A∪B|. Returns 0 for empty inputs. */
    private fun jaccard( a: Set<String>, b: Set<String> ): Double {
        if( a.isEmpty() || b.isEmpty() ) return 0.0
        val intersection = a.intersect( b ).size.toDouble()
        val union = a.union( b ).size.toDouble()
        return intersection / union
    }

    // --- Duration comparison --------------------------------------------------

    /** Parse "m:ss" / "h:mm:ss" to seconds, returns null on failure. */
    private fun parseSec( durationText: String? ): Long? {
        val d: Duration = durationText.toDuration()
        return if( d == Duration.ZERO && durationText?.isNotBlank() == true ) null
               else d.inWholeSeconds.takeIf { it > 0 }
    }

    // --- Search ---------------------------------------------------------------

    /**
     * Query YouTube Music for songs matching [song]'s title and artists.
     * Returns the raw result list (may be empty on network failure).
     */
    suspend fun searchCandidates( song: Song ): List<Innertube.SongItem> {
        val query = filteredText("${song.cleanTitle()} ${song.cleanArtistsText()}")
        return runCatching {
            Innertube.searchPage(
                body = SearchBody(
                    query  = query,
                    params = Innertube.SearchFilter.Song.value
                ),
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )?.getOrNull()?.items ?: emptyList()
        }.getOrDefault( emptyList() )
    }

    // --- Matching -------------------------------------------------------------

    /**
     * Pick the best replacement from [candidates] for [song].
     *
     * Filters out any candidate whose videoId matches the already-dead
     * [song].id before scoring.
     *
     * Returns null when there are no usable candidates.
     */
    fun bestMatch( song: Song, candidates: List<Innertube.SongItem> ): Match? {
        val viable = candidates.filter { it.key.isNotBlank() && it.key != song.id }
        if( viable.isEmpty() ) return null

        val songTitleTokens  = tokens( song.cleanTitle() )
        val songArtistTokens = tokens( song.cleanArtistsText() )
        val songSec          = parseSec( song.durationText )

        data class Scored( val item: Innertube.SongItem, val titleSim: Double )

        // Score every viable candidate, not just the first.
        val scored = viable.map { cand ->
            val candTitleTokens  = tokens( cand.info?.name ?: "" )
            val candArtistTokens = tokens(
                cand.authors?.joinToString(" ") { it.name ?: "" } ?: ""
            )
            val titleSim      = jaccard( songTitleTokens, candTitleTokens )
            val artistOverlap = songArtistTokens.intersect( candArtistTokens ).isNotEmpty()
            val candSec       = parseSec( cand.durationText )
            val durationClose = songSec != null && candSec != null && abs( songSec - candSec ) <= 3
            val isStrong      = durationClose && titleSim >= 0.6 && artistOverlap
            Triple( cand, titleSim, isStrong )
        }

        // Return the best-scoring STRONG candidate, or the top WEAK (relevance order).
        val bestStrong = scored.filter { it.third }.maxByOrNull { it.second }
        return if( bestStrong != null )
            Match( bestStrong.first, Confidence.STRONG )
        else
            Match( viable.first(), Confidence.WEAK )
    }
}
