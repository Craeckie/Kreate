package it.fast4x.rimusic.utils

import app.kreate.database.models.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveTapPlaybackTest {

    private fun song( id: String ) = Song(
        id = id,
        title = "title-$id",
        artistsText = null,
        durationText = null,
        thumbnailUrl = null
    )

    @Test
    fun `filter active, empty selection returns full list at tapped index`() {
        val a = song( "a" )
        val b = song( "b" )
        val c = song( "c" )
        val fullList = listOf( a, b, c )

        val (songs, index) = resolveTapPlayback( b, emptyList(), fullList )

        assertEquals( fullList, songs )
        assertEquals( 1, index )
    }

    @Test
    fun `non-empty selection containing song returns selection at its index`() {
        val a = song( "a" )
        val b = song( "b" )
        val c = song( "c" )
        val fullList = listOf( a, b, c )
        val selection = listOf( c, a )

        val (songs, index) = resolveTapPlayback( a, selection, fullList )

        assertEquals( selection, songs )
        assertEquals( 1, index )
    }

    @Test
    fun `non-empty selection not containing song falls back to full list`() {
        val a = song( "a" )
        val b = song( "b" )
        val c = song( "c" )
        val fullList = listOf( a, b, c )
        val selection = listOf( a )

        val (songs, index) = resolveTapPlayback( c, selection, fullList )

        assertEquals( fullList, songs )
        assertEquals( 2, index )
    }

    @Test
    fun `song absent from full list returns single-element list at index 0`() {
        val a = song( "a" )
        val b = song( "b" )
        val fullList = listOf( a, b )
        val missing = song( "missing" )

        val (songs, index) = resolveTapPlayback( missing, emptyList(), fullList )

        assertEquals( listOf( missing ), songs )
        assertEquals( 0, index )
    }

    @Test
    fun `duplicate ids in full list match the first occurrence`() {
        val a1 = song( "a" )
        val b = song( "b" )
        val a2 = song( "a" )
        val fullList = listOf( a1, b, a2 )

        val (songs, index) = resolveTapPlayback( a2, emptyList(), fullList )

        assertEquals( fullList, songs )
        assertEquals( 0, index )
    }
}
