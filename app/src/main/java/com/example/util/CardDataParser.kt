package com.example.util

import com.example.data.model.Belt
import com.example.data.model.JudoGroup
import org.json.JSONObject
import java.util.Locale

data class ExtractedMemberInfo(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "", // Format AAAA-MM-JJ
    val gender: String = "",    // "M", "F", or ""
    val phone: String = "",
    val notes: String = "",
    val qrCode: String = "",
    val cardNumber: String = "",
    val season: String = "",
    val photoUrl: String = "",
    val group: JudoGroup? = null,
    val belt: Belt = Belt.BLANCHE,
    val rawScannedData: String = "",
    val portraitDetected: Boolean = false,
    val ocrRecognized: Boolean = false
)

object CardDataParser {

    private val BLACKLIST_NAME_WORDS = setOf(
        "JUDO", "CLUB", "SEDDOUK", "FEDERATION", "FRANCAISE", "FRANÇAISE", "ALGERIENNE", "ALGÉRIENNE",
        "REPUBLIQUE", "RÉPUBLIQUE", "FFJDA", "CARTE", "ADHERENT", "ADHÉRENT", "MEMBRE", "COTISATION",
        "SAISON", "PASSEPORT", "SPORTIF", "LICENCE", "IDENTITE", "IDENTITÉ", "NATIONALE",
        "MINISTERE", "MINISTÈRE", "JEUNESSE", "SPORTS", "ASSOCIATION", "DOJO", "TATAMI",
        "PRESENCE", "PRÉSENCE", "ANNEE", "ANNÉE", "VALABLE", "DATE", "NOM", "PRENOM", "PRÉNOM",
        "SEXE", "TEL", "TÉL", "TELEPHONE", "TÉLÉPHONE", "NAISSANCE", "SIGNATURE", "DIRECTION",
        "LIGUE", "COMITE", "COMITÉ", "REGION", "RÉGION", "VILLE", "WILAYA", "COMMUNE", "CODE",
        "PHOTO", "PROFIL", "ATTESTATION", "CERTIFICAT", "MEDICAL", "MÉDICAL", "SURNAME", "NAME",
        "FIRSTNAME", "LASTNAME", "GENDER", "BIRTH", "CARD", "EXPIRES", "DELIVRE", "DÉLIVRÉ"
    )

    fun parseCardData(raw: String): ExtractedMemberInfo {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ExtractedMemberInfo()

        // 1. Try parsing JSON format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = JSONObject(trimmed)
                val firstName = json.optString("firstName", json.optString("prenom", "")).trim()
                val lastName = json.optString("lastName", json.optString("nom", "")).trim()
                val birthDateRaw = json.optString("birthDate", json.optString("dateNaissance", "")).trim()
                val birthDate = normalizeDateToIso(birthDateRaw)
                val gender = normalizeGender(json.optString("gender", json.optString("sexe", "")))
                val phone = cleanPhoneNumber(json.optString("phone", json.optString("telephone", "")))
                val cardNumber = json.optString("cardNumber", json.optString("licence", json.optString("numero", ""))).trim()
                val qrCode = json.optString("qrCode", if (cardNumber.isNotBlank()) "JCS-$cardNumber" else "")
                val beltStr = json.optString("belt", json.optString("ceinture", "BLANCHE"))
                val groupStr = json.optString("group", json.optString("categorie", ""))
                val season = json.optString("season", json.optString("saison", ""))
                val photoUrl = json.optString("photoUrl", json.optString("photo", ""))
                val notes = json.optString("notes", "")

                val group = if (groupStr.isNotBlank()) JudoGroup.fromString(groupStr)
                else if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate)
                else null

                return ExtractedMemberInfo(
                    firstName = formatFirstName(firstName),
                    lastName = formatLastName(lastName),
                    birthDate = birthDate,
                    gender = gender,
                    phone = phone,
                    notes = notes,
                    qrCode = qrCode,
                    cardNumber = cardNumber,
                    season = season,
                    photoUrl = photoUrl,
                    group = group,
                    belt = Belt.fromString(beltStr),
                    rawScannedData = trimmed,
                    ocrRecognized = (firstName.isNotBlank() || lastName.isNotBlank() || birthDate.isNotBlank())
                )
            } catch (_: Exception) {}
        }

        // 2. Try parsing Delimited Key-Value strings (e.g., KEY:VALUE;KEY:VALUE or KEY=VALUE)
        if (trimmed.contains(";") || trimmed.contains("|")) {
            val delimiter = if (trimmed.contains(";")) ";" else "|"
            val parts = trimmed.split(delimiter)
            val map = mutableMapOf<String, String>()
            for (part in parts) {
                val kv = part.split(":", "=")
                if (kv.size == 2) {
                    map[kv[0].trim().uppercase(Locale.ROOT)] = kv[1].trim()
                }
            }
            if (map.isNotEmpty()) {
                val firstName = map["FIRSTNAME"] ?: map["PRENOM"] ?: map["PRÉNOM"] ?: ""
                val lastName = map["LASTNAME"] ?: map["NOM"] ?: ""
                val birthDate = normalizeDateToIso(map["BIRTHDATE"] ?: map["DOB"] ?: map["NAISSANCE"] ?: "")
                val gender = normalizeGender(map["GENDER"] ?: map["SEXE"] ?: "")
                val phone = cleanPhoneNumber(map["PHONE"] ?: map["TEL"] ?: map["TÉL"] ?: "")
                val cardNumber = map["CARD"] ?: map["LICENCE"] ?: map["ID"] ?: ""
                val qr = map["QR"] ?: if (cardNumber.isNotBlank()) "JCS-$cardNumber" else ""
                val beltStr = map["BELT"] ?: map["CEINTURE"] ?: "BLANCHE"
                val season = map["SEASON"] ?: map["SAISON"] ?: ""

                val group = if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate) else null

                return ExtractedMemberInfo(
                    firstName = formatFirstName(firstName),
                    lastName = formatLastName(lastName),
                    birthDate = birthDate,
                    gender = gender,
                    phone = phone,
                    cardNumber = cardNumber,
                    qrCode = qr,
                    season = season,
                    group = group,
                    belt = Belt.fromString(beltStr),
                    rawScannedData = trimmed,
                    ocrRecognized = (firstName.isNotBlank() || lastName.isNotBlank() || birthDate.isNotBlank())
                )
            }
        }

        // 3. Simple QR Code (e.g. JCS-1234, 1234)
        if (trimmed.startsWith("JCS-", ignoreCase = true) || (trimmed.length in 3..12 && trimmed.all { it.isDigit() || it == '-' })) {
            val cleanNumber = trimmed.removePrefix("JCS-").removePrefix("jcs-").removePrefix("MEM-")
            return ExtractedMemberInfo(
                qrCode = if (trimmed.startsWith("JCS-", ignoreCase = true)) trimmed.uppercase(Locale.ROOT) else "JCS-$cleanNumber",
                cardNumber = cleanNumber,
                rawScannedData = trimmed
            )
        }

        // 4. Free-form text / OCR Lines fallback
        val lines = trimmed.split("\n")
        return parseOcrText(trimmed, lines)
    }

    /**
     * Parses free-form text extracted by OCR from a physical document or sport card.
     */
    fun parseOcrText(fullText: String, lines: List<String>): ExtractedMemberInfo {
        var lastName = ""
        var firstName = ""
        var birthDate = ""
        var gender = ""
        var phone = ""
        var cardNumber = ""
        var season = ""

        val rawLines = if (lines.isNotEmpty()) lines else fullText.split("\n")
        val cleanLines = rawLines.map { it.trim() }.filter { it.isNotBlank() }

        for (i in cleanLines.indices) {
            val line = cleanLines[i]

            // --- SAISON ---
            if (season.isBlank()) {
                val seasonMatch = Regex("""(?i)\b(?:saison|season)?\s*[:=\-]?\s*(20[2-3][0-9][\/\-]20[2-3][0-9])\b""").find(line)
                if (seasonMatch != null) {
                    season = seasonMatch.groupValues[1].trim()
                }
            }

            // --- NOM DE FAMILLE (LAST NAME) ---
            // Must strictly avoid matching "PRÉNOM" as "NOM"
            if (lastName.isBlank() && !line.contains(Regex("""(?iu)\bpr[eéEÉ]nom"""))) {
                val nomRegex = Regex("""(?iu)\b(?:nom(?:\s+de\s+famille)?|surname|last\s*name)\b\s*[:=\-]?\s*(.*)$""")
                val match = nomRegex.find(line)
                if (match != null) {
                    val rawVal = match.groupValues[1].trim()
                    val candidate = sanitizeName(rawVal)
                    if (candidate.isNotBlank() && isValidName(candidate)) {
                        lastName = formatLastName(candidate)
                    } else if (candidate.isBlank() && i + 1 < cleanLines.size) {
                        val nextLineCand = sanitizeName(cleanLines[i + 1])
                        if (isValidName(nextLineCand) && !cleanLines[i + 1].contains(Regex("""(?iu)\b(?:nom|pr[eéEÉ]nom|sexe|date|licence)\b"""))) {
                            lastName = formatLastName(nextLineCand)
                        }
                    }
                }
            }

            // --- PRÉNOM (FIRST NAME) ---
            if (firstName.isBlank()) {
                val prenomRegex = Regex("""(?iu)\b(?:pr[eéEÉ]nom(?:\(s\))?|first\s*name|given\s*name)\b\s*[:=\-]?\s*(.*)$""")
                val match = prenomRegex.find(line)
                if (match != null) {
                    val rawVal = match.groupValues[1].trim()
                    val candidate = sanitizeName(rawVal)
                    if (candidate.isNotBlank() && isValidName(candidate)) {
                        firstName = formatFirstName(candidate)
                    } else if (candidate.isBlank() && i + 1 < cleanLines.size) {
                        val nextLineCand = sanitizeName(cleanLines[i + 1])
                        if (isValidName(nextLineCand) && !cleanLines[i + 1].contains(Regex("""(?iu)\b(?:nom|pr[eéEÉ]nom|sexe|date|licence)\b"""))) {
                            firstName = formatFirstName(nextLineCand)
                        }
                    }
                }
            }

            // --- DATE DE NAISSANCE ---
            if (birthDate.isBlank()) {
                val dobRegex = Regex("""(?iu)\b(?:n[eéEÉ]\(?e?\)?\s*(?:le)?|date\s*(?:de)?\s*naissance|naissance|d\.?o\.?b\.?|birth\s*date)\s*[:=\-]?\s*([0-9]{1,2}[\/\-\.][0-9]{1,2}[\/\-\.][0-9]{2,4}|[0-9]{4}[\/\-\.][0-9]{1,2}[\/\-\.][0-9]{1,2})""")
                val match = dobRegex.find(line)
                if (match != null) {
                    val parsed = normalizeDateToIso(match.groupValues[1])
                    if (parsed.isNotBlank()) birthDate = parsed
                } else if (Regex("""(?iu)\b(?:n[eéEÉ]\(?e?\)?\s*(?:le)?|date\s*(?:de)?\s*naissance|naissance|d\.?o\.?b\.?|birth\s*date)\s*[:=\-]?\s*$""").containsMatchIn(line) && i + 1 < cleanLines.size) {
                    val nextLine = cleanLines[i + 1]
                    val dateMatch = Regex("""\b([0-9]{1,2}[\/\-\.][0-9]{1,2}[\/\-\.][0-9]{2,4}|[0-9]{4}[\/\-\.][0-9]{1,2}[\/\-\.][0-9]{1,2})\b""").find(nextLine)
                    if (dateMatch != null) {
                        val parsed = normalizeDateToIso(dateMatch.groupValues[1])
                        if (parsed.isNotBlank()) birthDate = parsed
                    }
                }
            }

            // --- SEXE (GENDER) ---
            if (gender.isBlank()) {
                val sexeRegex = Regex("""(?iu)\b(?:sexe|gender|genre)\s*[:=\-]?\s*(masculin|f[eéEÉ]minin|homme|femme|[mfMF])\b""")
                val match = sexeRegex.find(line)
                if (match != null) {
                    gender = normalizeGender(match.groupValues[1])
                }
            }

            // --- TÉLÉPHONE DU TUTEUR / ATHLÈTE ---
            if (phone.isBlank()) {
                val telRegex = Regex("""(?iu)\b(?:t[eéEÉ]l(?:[eéEÉ]phone)?(?:\s*tuteur|\s*parent)?|contact|mobile|gsm|phone)\s*[:=\-]?\s*([+0-9\s\.\-]{8,18})""")
                val match = telRegex.find(line)
                if (match != null) {
                    val cleaned = cleanPhoneNumber(match.groupValues[1])
                    if (cleaned.length >= 8) {
                        phone = cleaned
                    }
                }
            }

            // --- IDENTIFIANT / N° LICENCE / N° CARTE ---
            if (cardNumber.isBlank()) {
                val idRegex = Regex("""(?iu)\b(?:licence|carte|identifiant|matricule|adh[eéEÉ]rent|n°|n[uúUÚ]m[eéEÉ]ro)\s*(?:n[°o\.]?)?\s*[:=\-]?\s*([A-Za-z0-9\-]{3,16})""")
                val match = idRegex.find(line)
                if (match != null) {
                    val candidate = match.groupValues[1].trim()
                    val isSeasonYear = candidate.matches(Regex("""20[2-3][0-9][\/\-]20[2-3][0-9]"""))
                    if (!isSeasonYear && candidate.any { it.isDigit() } && !candidate.equals("SEDDOUK", ignoreCase = true) && !candidate.equals("JUDO", ignoreCase = true)) {
                        cardNumber = candidate.removePrefix("JCS-").removePrefix("jcs-")
                    } else if (isSeasonYear && season.isBlank()) {
                        season = candidate
                    }
                }
            }
        }

        // Secondary Pass: Standalone date if birthDate not found yet
        if (birthDate.isBlank()) {
            val standaloneDateRegex = Regex("""\b(0[1-9]|[12][0-9]|3[01])[\/\-\.](0[1-9]|1[0-2])[\/\-\.](19[5-9][0-9]|20[0-2][0-9])\b""")
            val match = standaloneDateRegex.find(fullText)
            if (match != null) {
                birthDate = normalizeDateToIso(match.value)
            }
        }

        // Secondary Pass: Standalone Phone if not found yet
        if (phone.isBlank()) {
            val standalonePhoneRegex = Regex("""\b(0[567][0-9]{8}|0[1-9](?:[\s\.\-]?[0-9]{2}){4}|\+?(?:33|213)[\s\.\-]?[0-9\s\.\-]{8,12})\b""")
            val match = standalonePhoneRegex.find(fullText)
            if (match != null) {
                val cleaned = cleanPhoneNumber(match.value)
                if (cleaned.length >= 8) phone = cleaned
            }
        }

        // Deriving suggested Judo group
        val group = if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate) else null
        val qrCode = if (cardNumber.isNotBlank()) "JCS-$cardNumber" else ""

        val recognizedAny = firstName.isNotBlank() || lastName.isNotBlank() || birthDate.isNotBlank() || phone.isNotBlank() || cardNumber.isNotBlank()

        return ExtractedMemberInfo(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            gender = gender,
            phone = phone,
            cardNumber = cardNumber,
            qrCode = qrCode,
            season = season,
            group = group,
            rawScannedData = fullText,
            ocrRecognized = recognizedAny
        )
    }

    /**
     * Merge information extracted from QR / Barcode and Document OCR with cropped portrait.
     */
    fun mergeInfo(
        qrInfo: ExtractedMemberInfo?,
        ocrInfo: ExtractedMemberInfo?,
        croppedPortraitPath: String,
        portraitDetected: Boolean
    ): ExtractedMemberInfo {
        val base = qrInfo ?: ocrInfo ?: ExtractedMemberInfo()
        val ocr = ocrInfo ?: ExtractedMemberInfo()

        val firstName = if (base.firstName.isNotBlank()) base.firstName else ocr.firstName
        val lastName = if (base.lastName.isNotBlank()) base.lastName else ocr.lastName
        val birthDate = if (base.birthDate.isNotBlank()) base.birthDate else ocr.birthDate
        val gender = if (base.gender.isNotBlank()) base.gender else ocr.gender
        val phone = if (base.phone.isNotBlank()) base.phone else ocr.phone
        val cardNumber = if (base.cardNumber.isNotBlank()) base.cardNumber else ocr.cardNumber
        val qrCode = if (base.qrCode.isNotBlank()) base.qrCode
            else if (ocr.qrCode.isNotBlank()) ocr.qrCode
            else if (cardNumber.isNotBlank()) "JCS-$cardNumber"
            else ""
        val season = if (base.season.isNotBlank()) base.season else ocr.season

        val group = if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate)
            else base.group ?: ocr.group

        val raw = listOfNotNull(
            qrInfo?.rawScannedData.takeIf { !it.isNullOrBlank() }?.let { "QR: $it" },
            ocrInfo?.rawScannedData.takeIf { !it.isNullOrBlank() }?.let { "OCR: $it" }
        ).joinToString("\n\n")

        return ExtractedMemberInfo(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            gender = gender,
            phone = phone,
            cardNumber = cardNumber,
            qrCode = qrCode,
            season = season,
            photoUrl = croppedPortraitPath,
            group = group,
            belt = base.belt,
            rawScannedData = raw,
            portraitDetected = portraitDetected,
            ocrRecognized = (firstName.isNotBlank() || lastName.isNotBlank() || birthDate.isNotBlank() || phone.isNotBlank())
        )
    }

    private fun sanitizeName(raw: String): String {
        return raw
            .replace(Regex("""^[:=\-\s,]+"""), "")
            .replace(Regex("""[:=\-\s,]+$"""), "")
            .replace(Regex("""[^A-Za-zÀ-ÖØ-öø-ÿ\s\-']"""), "")
            .trim()
    }

    fun isValidName(text: String): Boolean {
        val clean = text.replace(Regex("""[^A-Za-zÀ-ÖØ-öø-ÿ\s\-]"""), "").trim()
        if (clean.length < 2) return false
        val words = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        return words.none { it.uppercase(Locale.ROOT) in BLACKLIST_NAME_WORDS }
    }

    fun formatLastName(raw: String): String {
        return sanitizeName(raw).uppercase(Locale.ROOT)
    }

    fun formatFirstName(raw: String): String {
        val clean = sanitizeName(raw)
        return clean.split(Regex("""[\s\-]+""")).filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
            }
    }

    fun normalizeGender(raw: String): String {
        val clean = raw.trim().uppercase(Locale.ROOT)
        return when {
            clean.startsWith("M") || clean.startsWith("H") -> "M"
            clean.startsWith("F") -> "F"
            else -> ""
        }
    }

    fun cleanPhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() || it == '+' }
    }

    fun normalizeDateToIso(raw: String): String {
        val clean = raw.trim().replace(".", "/").replace("-", "/")
        val parts = clean.split("/")
        if (parts.size == 3) {
            val p0 = parts[0].padStart(2, '0')
            val p1 = parts[1].padStart(2, '0')
            val p2 = parts[2]
            return try {
                if (p0.length == 4) {
                    // YYYY/MM/DD
                    "$p0-$p1-${p2.padStart(2, '0')}"
                } else if (p2.length == 4) {
                    // DD/MM/YYYY
                    "$p2-$p1-$p0"
                } else if (p2.length == 2) {
                    // 2-digit year DD/MM/YY
                    val yearNum = p2.toIntOrNull() ?: return ""
                    val fullYear = if (yearNum > 30) 1900 + yearNum else 2000 + yearNum
                    "$fullYear-$p1-$p0"
                } else ""
            } catch (_: Exception) {
                ""
            }
        }
        return ""
    }
}
