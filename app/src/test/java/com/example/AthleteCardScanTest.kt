package com.example

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.data.model.JudoGroup
import com.example.util.CardDataParser
import com.example.util.ExtractedMemberInfo
import com.example.util.SportCardScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AthleteCardScanTest {

    @Test
    fun testOcrTextExtraction_allFieldsPresentAndCorrect() {
        val ocrDocumentText = """
            JUDO CLUB SEDDOUK
            CARTE D'ADHÉRENT 2026-2027
            NOM : BOURENANE
            PRÉNOM : Babi
            Date de naissance : 06/01/2013
            Sexe : M
            Tél : 0782487120
            Licence N° : 4521
        """.trimIndent()

        val extracted = CardDataParser.parseOcrText(
            fullText = ocrDocumentText,
            lines = ocrDocumentText.split("\n")
        )

        // 1. Correct fields extracted
        assertEquals("BOURENANE", extracted.lastName)
        assertEquals("Babi", extracted.firstName)
        assertEquals("2013-01-06", extracted.birthDate)
        assertEquals("M", extracted.gender)
        assertEquals("0782487120", extracted.phone)
        assertEquals("4521", extracted.cardNumber)
        assertEquals("JCS-4521", extracted.qrCode)

        // 2. Club name is never confused with athlete name
        assertNotEquals("JUDO", extracted.lastName)
        assertNotEquals("SEDDOUK", extracted.lastName)
        assertNotEquals("CLUB", extracted.firstName)

        // 3. Recommended Judo category matches birth date
        assertEquals(JudoGroup.fromBirthDate("2013-01-06"), extracted.group)
        assertTrue(extracted.ocrRecognized)
    }

    @Test
    fun testOcrTextExtraction_missingFieldsRemainEmpty() {
        val minimalText = """
            FÉDÉRATION FRANÇAISE DE JUDO
            NOM : DUPONT
            PRÉNOM : Jean
        """.trimIndent()

        val extracted = CardDataParser.parseOcrText(
            fullText = minimalText,
            lines = minimalText.split("\n")
        )

        assertEquals("DUPONT", extracted.lastName)
        assertEquals("Jean", extracted.firstName)

        // Unreadable / absent fields must remain empty - never fabricated
        assertEquals("", extracted.birthDate)
        assertEquals("", extracted.gender)
        assertEquals("", extracted.phone)
        assertEquals("", extracted.cardNumber)
        assertEquals("", extracted.photoUrl)
        assertFalse(extracted.portraitDetected)
    }

    @Test
    fun testNameInversionPrevention_prenomAndNomOnSeparateLines() {
        val invertedLinesText = """
            PRÉNOM
            Mohamed
            NOM
            BENALI
            SEXE : MASCULIN
            NÉ LE : 14/05/2012
        """.trimIndent()

        val extracted = CardDataParser.parseOcrText(
            fullText = invertedLinesText,
            lines = invertedLinesText.split("\n")
        )

        // Must never confuse Nom and Prénom
        assertEquals("BENALI", extracted.lastName)
        assertEquals("Mohamed", extracted.firstName)
        assertEquals("2012-05-14", extracted.birthDate)
        assertEquals("M", extracted.gender)
    }

    @Test
    fun testPortraitCropping_keepsHeadAndShouldersAndNeverReturnsEntireDocument() {
        // Create a simulated 800x600 card document bitmap
        val documentBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        // Face bounding box located in a portrait photo section of the card: x: 100, y: 120, w: 90, h: 110
        val faceBox = Rect(100, 120, 190, 230)

        val croppedPortrait = SportCardScanner.cropPortrait(documentBitmap, faceBox)

        assertNotNull("Cropped portrait should not be null when a valid face is detected", croppedPortrait)
        croppedPortrait?.let { portrait ->
            // Bounding dimensions should be around head and shoulders (e.g. ~157 x ~247)
            assertTrue("Portrait width should be greater than raw face width", portrait.width >= faceBox.width())
            assertTrue("Portrait height should be greater than raw face height", portrait.height >= faceBox.height())

            // Must NEVER return the full card/document image
            assertFalse(
                "Cropped portrait must NEVER be the entire document width",
                portrait.width >= documentBitmap.width * 0.95f
            )
            assertFalse(
                "Cropped portrait must NEVER be the entire document height",
                portrait.height >= documentBitmap.height * 0.95f
            )
        }

        // Test with a bounding box that would cover the entire image: must be rejected
        val wholeImageBox = Rect(0, 0, 800, 600)
        val rejectedPortrait = SportCardScanner.cropPortrait(documentBitmap, wholeImageBox)
        assertNull("Full document box must be rejected to avoid using entire document as profile picture", rejectedPortrait)
    }

    @Test
    fun testMergeInfo_preservesAllRecognizedFields() {
        val ocrInfo = ExtractedMemberInfo(
            firstName = "Babi",
            lastName = "BOURENANE",
            birthDate = "2013-01-06",
            gender = "M",
            phone = "0782487120",
            cardNumber = "4521"
        )

        val merged = CardDataParser.mergeInfo(
            qrInfo = null,
            ocrInfo = ocrInfo,
            croppedPortraitPath = "/data/user/0/cache/judoka_portraits/portrait_123.jpg",
            portraitDetected = true
        )

        assertEquals("BOURENANE", merged.lastName)
        assertEquals("Babi", merged.firstName)
        assertEquals("2013-01-06", merged.birthDate)
        assertEquals("M", merged.gender)
        assertEquals("0782487120", merged.phone)
        assertEquals("4521", merged.cardNumber)
        assertEquals("JCS-4521", merged.qrCode)
        assertEquals("/data/user/0/cache/judoka_portraits/portrait_123.jpg", merged.photoUrl)
        assertTrue(merged.portraitDetected)
    }
}
