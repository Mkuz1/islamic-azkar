package com.koussa.islamicazkar.data

import android.content.Context
import android.graphics.Typeface
import android.icu.util.IslamicCalendar
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import com.koussa.islamicazkar.R

/** The Hijri (Umm al-Qura) date, shifted by the user's offset to match the local moon sighting. */
object Hijri {
    data class Date(val day: Int, val month: Int, val year: Int)

    fun of(ctx: Context, millis: Long = System.currentTimeMillis()): Date =
        of(millis, Settings.prefs(ctx).getInt(Settings.K_HIJRI_OFFSET, 0))

    fun of(millis: Long, offsetDays: Int): Date {
        val c = IslamicCalendar().apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            timeInMillis = millis + offsetDays * 86_400_000L
        }
        return Date(c.get(IslamicCalendar.DAY_OF_MONTH), c.get(IslamicCalendar.MONTH), c.get(IslamicCalendar.YEAR))
    }

    private val MONTHS_AR = listOf("مُحَرَّم", "صَفَر", "رَبِيع الأوَّل", "رَبِيع الآخِر", "جُمادَى الأُولى", "جُمادَى الآخِرة", "رَجَب", "شَعْبان", "رَمَضان", "شَوَّال", "ذُو القَعْدة", "ذُو الحِجَّة")
    private val MONTHS_EN = listOf("Muharram", "Safar", "Rabiʿ al-Awwal", "Rabiʿ al-Akhir", "Jumada al-Ula", "Jumada al-Akhira", "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhu al-Qaʿda", "Dhu al-Hijja")
    private val MONTHS_FR = listOf("Mouharram", "Safar", "Rabiʿ al-Awwal", "Rabiʿ ath-Thani", "Joumada al-Oula", "Joumada ath-Thania", "Rajab", "Chaʿbane", "Ramadan", "Chawwal", "Dhou al-Qiʿda", "Dhou al-Hijja")
    private val WEEKDAYS_AR = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    private val WEEKDAYS_EN = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val WEEKDAYS_FR = listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

    fun monthAr(m: Int) = MONTHS_AR[m]
    fun month(lang: Lang, m: Int) = when (lang) { Lang.AR -> MONTHS_AR[m]; Lang.EN -> MONTHS_EN[m]; Lang.FR -> MONTHS_FR[m] }

    /** [dow] counts from Sunday = 0. */
    fun weekdayAr(dow: Int) = WEEKDAYS_AR[dow]
    fun weekday(lang: Lang, dow: Int) = when (lang) { Lang.AR -> WEEKDAYS_AR[dow]; Lang.EN -> WEEKDAYS_EN[dow]; Lang.FR -> WEEKDAYS_FR[dow] }

    fun format(lang: Lang, d: Date): String = when (lang) {
        Lang.AR -> "${Lang.AR.digits(d.day)} ${MONTHS_AR[d.month]} ${Lang.AR.digits(d.year)} هـ"
        Lang.EN -> "${d.day} ${MONTHS_EN[d.month]} ${d.year} AH"
        Lang.FR -> "${d.day} ${MONTHS_FR[d.month]} ${d.year} H"
    }
}

/** Arabic-Indic digits for Arabic, Western digits otherwise. */
fun Lang.digits(n: Int): String =
    if (this == Lang.AR) n.toString().map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("") else n.toString()

/** Classical scripts used for weekdays, months, prayer names and quotes on widgets. */
enum class Script(val ar: String, val en: String, val fr: String, val font: Int, val bold: Boolean) {
    RUQAA("الرقعة", "Ruqʿah", "Roqʿa", R.font.aref_ruqaa_bold, false),
    NASKH("النسخ", "Naskh", "Naskh", R.font.amiri_bold, false),
    KUFI("الكوفي", "Kufic", "Coufique", R.font.reem_kufi, true),
    NASTALIQ("النستعليق", "Nastaʿlīq", "Nastaʿlîq", R.font.nastaliq, true);

    fun label(lang: Lang) = when (lang) { Lang.AR -> ar; Lang.EN -> en; Lang.FR -> fr }

    fun typeface(ctx: Context): Typeface {
        val base = ResourcesCompat.getFont(ctx, font) ?: Typeface.SERIF
        if (!bold) return base
        return if (Build.VERSION.SDK_INT >= 28) Typeface.create(base, 700, false) else Typeface.create(base, Typeface.BOLD)
    }

    companion object {
        fun current(ctx: Context): Script = entries.getOrElse(Settings.prefs(ctx).getInt(Settings.K_SCRIPT, 0)) { RUQAA }
    }
}

/** Calm widget colour palettes: the glass tint behind every widget, and the matching clock face. */
enum class Palette(val ar: String, val en: String, val fr: String, val glass: Int, val top: Int, val bottom: Int) {
    MUSHAF("أخضر المصحف", "Mushaf green", "Vert Mushaf", R.drawable.widget_glass, 0xFF295245.toInt(), 0xFF0B1F19.toInt()),
    MIDNIGHT("ليل", "Midnight", "Minuit", R.drawable.widget_glass_midnight, 0xFF26355A.toInt(), 0xFF0B1229.toInt()),
    SAND("رمل", "Desert sand", "Sable", R.drawable.widget_glass_sand, 0xFF6B5233.toInt(), 0xFF2A1E10.toInt()),
    ROSE("وردي", "Rose", "Rose", R.drawable.widget_glass_rose, 0xFF6A2F3E.toInt(), 0xFF2A0F17.toInt()),
    TEAL("فيروزي", "Turquoise", "Turquoise", R.drawable.widget_glass_teal, 0xFF1F5A5E.toInt(), 0xFF082626.toInt()),
    LAVENDER("خزامى", "Lavender", "Lavande", R.drawable.widget_glass_lavender, 0xFF4A3B6B.toInt(), 0xFF1C1530.toInt()),
    GRAPHITE("فحمي", "Graphite", "Graphite", R.drawable.widget_glass_graphite, 0xFF3A3D42.toInt(), 0xFF121315.toInt());

    fun label(lang: Lang) = when (lang) { Lang.AR -> ar; Lang.EN -> en; Lang.FR -> fr }

    companion object {
        fun current(ctx: Context): Palette = entries.getOrElse(Settings.prefs(ctx).getInt(Settings.K_PALETTE, 0)) { MUSHAF }
    }
}

/** A quote with its meaning and source in each language. */
data class Quote(val ar: String, val en: String, val fr: String, val srcAr: String, val srcEn: String, val srcFr: String) {
    fun meaning(lang: Lang) = when (lang) { Lang.AR -> ""; Lang.EN -> en; Lang.FR -> fr }
    fun source(lang: Lang) = when (lang) { Lang.AR -> srcAr; Lang.EN -> srcEn; Lang.FR -> srcFr }
}

/** Classical Arabic poetry: one verse each, its two hemistichs on separate lines. */
object Poetry {
    private fun p(ar: String, en: String, fr: String, poetAr: String, poet: String) = Quote(ar, en, fr, poetAr, poet, poet)
    private const val MUTANABBI_AR = "المتنبي"
    private const val MUTANABBI = "Al-Mutanabbi"
    private const val SHAFII_AR = "الإمام الشافعي"
    private const val SHAFII = "Imam al-Shafiʿi"

    val all = listOf(
        p("الخَيْلُ وَاللَّيْلُ وَالبَيْداءُ تَعرِفُني\nوَالسَّيفُ وَالرُّمحُ وَالقِرطاسُ وَالقَلَمُ",
            "The horses, the night and the desert know me, and so do the sword, the spear, the paper and the pen.",
            "Les chevaux, la nuit et le désert me connaissent, ainsi que l’épée, la lance, le papier et la plume.", MUTANABBI_AR, MUTANABBI),
        p("ما كُلُّ ما يَتَمَنّى المَرءُ يُدرِكُهُ\nتَجري الرِّياحُ بِما لا تَشتَهي السُّفُنُ",
            "Not everything a man wishes for does he attain; the winds blow as the ships do not desire.",
            "L’homme n’obtient pas tout ce qu’il souhaite ; les vents soufflent contre le désir des navires.", MUTANABBI_AR, MUTANABBI),
        p("عَلى قَدرِ أَهلِ العَزمِ تَأتي العَزائِمُ\nوَتَأتي عَلى قَدرِ الكِرامِ المَكارِمُ",
            "Resolve comes in the measure of the resolute, and noble deeds in the measure of the noble.",
            "La détermination vient à la mesure des résolus, et les nobles actions à la mesure des nobles.", MUTANABBI_AR, MUTANABBI),
        p("إِذا غامَرتَ في شَرَفٍ مَرومِ\nفَلا تَقنَع بِما دونَ النُّجومِ",
            "When you venture after a coveted honour, do not settle for anything below the stars.",
            "Quand tu poursuis un honneur convoité, ne te contente de rien en deçà des étoiles.", MUTANABBI_AR, MUTANABBI),
        p("أَعَزُّ مَكانٍ في الدُّنى سَرجُ سابِحٍ\nوَخَيرُ جَليسٍ في الزَّمانِ كِتابُ",
            "The dearest seat in the world is the saddle of a swift horse, and the best companion at any time is a book.",
            "La plus noble place au monde est la selle d’un coursier, et le meilleur compagnon de tout temps est un livre.", MUTANABBI_AR, MUTANABBI),
        p("وَإِذا أَتَتكَ مَذَمَّتي مِن ناقِصٍ\nفَهِيَ الشَّهادَةُ لي بِأَنّي كامِلُ",
            "When blame of me reaches you from a lesser man, it is testimony that I am complete.",
            "Quand le blâme à mon égard te vient d’un homme imparfait, c’est le témoignage de ma perfection.", MUTANABBI_AR, MUTANABBI),
        p("دَعِ الأَيّامَ تَفعَلُ ما تَشاءُ\nوَطِب نَفساً إِذا حَكَمَ القَضاءُ",
            "Let the days do as they will, and be at peace when destiny decrees.",
            "Laisse les jours faire ce qu’ils veulent, et apaise ton âme quand le destin décide.", SHAFII_AR, SHAFII),
        p("نَعيبُ زَمانَنا وَالعَيبُ فينا\nوَما لِزَمانِنا عَيبٌ سِوانا",
            "We blame our times while the fault is in us; our times have no fault but ourselves.",
            "Nous blâmons notre époque alors que la faute est en nous ; notre époque n’a d’autre défaut que nous.", SHAFII_AR, SHAFII),
        p("شَكَوتُ إِلى وَكيعٍ سوءَ حِفظي\nفَأَرشَدَني إِلى تَركِ المَعاصي",
            "I complained to Wakiʿ of my poor memory; he guided me to leave off sins.",
            "Je me suis plaint à Wakîʿ de ma mauvaise mémoire ; il m’a conseillé de délaisser les péchés.", SHAFII_AR, SHAFII),
        p("وَأَخبَرَني بِأَنَّ العِلمَ نورٌ\nوَنورُ اللَّهِ لا يُهدى لِعاصي",
            "He told me that knowledge is a light, and the light of Allah is not given to the disobedient.",
            "Il m’a appris que la science est une lumière, et que la lumière d’Allah n’est pas offerte au pécheur.", SHAFII_AR, SHAFII),
        p("وَلَرُبَّ نازِلَةٍ يَضيقُ لَها الفَتى\nذَرعاً وَعِندَ اللَّهِ مِنها المَخرَجُ",
            "Many a hardship weighs heavily on a man, yet with Allah lies the way out of it.",
            "Que d’épreuves accablent l’homme, alors qu’auprès d’Allah se trouve l’issue.", SHAFII_AR, SHAFII),
        p("ضاقَت فَلَمّا اِستَحكَمَت حَلَقاتُها\nفُرِجَت وَكُنتُ أَظُنُّها لا تُفرَجُ",
            "It tightened, and when its rings were firmly closed, it was lifted, though I thought it never would be.",
            "Elle s’est resserrée, et quand ses anneaux furent bien clos, elle s’est dénouée, alors que je la croyais sans issue.", SHAFII_AR, SHAFII),
        p("تَغَرَّب عَنِ الأَوطانِ في طَلَبِ العُلا\nوَسافِر فَفي الأَسفارِ خَمسُ فَوائِدِ",
            "Leave your homeland in pursuit of greatness, and travel, for in travel there are five benefits.",
            "Quitte ta patrie en quête d’élévation, et voyage, car le voyage apporte cinq bienfaits.", SHAFII_AR, SHAFII),
        p("لِسانَكَ لا تَذكُر بِهِ عَورَةَ اِمرِئٍ\nفَكُلُّكَ عَوراتٌ وَلِلنّاسِ أَعيُنُ",
            "Do not let your tongue mention another’s faults, for you are all faults, and people have eyes.",
            "Que ta langue ne mentionne pas les défauts d’autrui, car tu n’es que défauts et les gens ont des yeux.", SHAFII_AR, SHAFII),
        p("وَإِنَّما الأُمَمُ الأَخلاقُ ما بَقِيَت\nفَإِن هُمُ ذَهَبَت أَخلاقُهُم ذَهَبوا",
            "Nations endure only as long as their morals endure; if their morals go, they go too.",
            "Les nations ne durent que tant que durent leurs vertus ; si leurs vertus disparaissent, elles disparaissent.", "أحمد شوقي", "Ahmad Shawqi"),
        p("قُم لِلمُعَلِّمِ وَفِّهِ التَّبجيلا\nكادَ المُعَلِّمُ أَن يَكونَ رَسولا",
            "Stand for the teacher and give him full honour; the teacher is almost a messenger.",
            "Lève-toi pour l’enseignant et rends-lui tout honneur ; l’enseignant est presque un messager.", "أحمد شوقي", "Ahmad Shawqi"),
        p("إِذا الشَّعبُ يَوماً أَرادَ الحَياةَ\nفَلا بُدَّ أَن يَستَجيبَ القَدَر",
            "If one day a people wills to live, then destiny must answer.",
            "Si un jour un peuple veut vivre, le destin doit lui répondre.", "أبو القاسم الشابي", "Abu al-Qasim al-Shabbi"),
        p("وَمَهما تَكُن عِندَ اِمرِئٍ مِن خَليقَةٍ\nوَإِن خالَها تَخفى عَلى النّاسِ تُعلَمِ",
            "Whatever trait a man may have, though he thinks it hidden from people, it will be known.",
            "Quel que soit le caractère d’un homme, même s’il le croit caché aux gens, il finira par être connu.", "زهير بن أبي سلمى", "Zuhayr ibn Abi Sulma"),
        p("سَتُبدي لَكَ الأَيّامُ ما كُنتَ جاهِلاً\nوَيَأتيكَ بِالأَخبارِ مَن لَم تُزَوِّدِ",
            "The days will reveal to you what you did not know, and news will come to you from one you never sent.",
            "Les jours te révéleront ce que tu ignorais, et des nouvelles te viendront de celui que tu n’as pas envoyé.", "طرفة بن العبد", "Tarafa ibn al-ʿAbd"),
        p("وَأَحسَنُ مِنكَ لَم تَرَ قَطُّ عَيني\nوَأَجمَلُ مِنكَ لَم تَلِدِ النِّساءُ",
            "My eye has never seen anyone finer than you, and no woman has borne anyone more beautiful.",
            "Mon œil n’a jamais vu meilleur que toi, et aucune femme n’a enfanté plus beau que toi.", "حسان بن ثابت", "Hassan ibn Thabit"),
        p("خُلِقتَ مُبَرَّأً مِن كُلِّ عَيبٍ\nكَأَنَّكَ قَد خُلِقتَ كَما تَشاءُ",
            "You were created free of every flaw, as though you were created as you wished.",
            "Tu as été créé exempt de tout défaut, comme si tu avais été créé selon ton désir.", "حسان بن ثابت", "Hassan ibn Thabit"),
        p("مُحَمَّدٌ سَيِّدُ الكَونَينِ وَالثَّقَلَينِ\nوَالفَريقَينِ مِن عُربٍ وَمِن عَجَمِ",
            "Muhammad, master of both worlds, of men and jinn, and of both peoples, Arabs and non-Arabs.",
            "Muhammad, maître des deux mondes, des hommes et des djinns, et des deux peuples, Arabes et non-Arabes.", "البوصيري", "Al-Busiri"),
        p("أَلا لَيتَ الشَّبابَ يَعودُ يَوماً\nفَأُخبِرَهُ بِما فَعَلَ المَشيبُ",
            "If only youth would return one day, so I could tell it what old age has done.",
            "Ah, si la jeunesse revenait un jour, je lui dirais ce qu’a fait la vieillesse.", "أبو العتاهية", "Abu al-ʿAtahiya"),
        p("وَإِنّي وَإِن كُنتُ الأَخيرَ زَمانُهُ\nلَآتٍ بِما لَم تَستَطِعهُ الأَوائِلُ",
            "Though I come last in time, I bring what the ancients could not.",
            "Bien que je sois venu le dernier, j’apporte ce que les anciens n’ont pu accomplir.", "أبو العلاء المعري", "Abu al-ʿAla al-Maʿarri"),
    )
}

/** Wisdom and proverbs. */
object Wisdom {
    private fun w(ar: String, en: String, fr: String, srcAr: String = "مثل عربي", srcEn: String = "Arabic proverb", srcFr: String = "Proverbe arabe") =
        Quote(ar, en, fr, srcAr, srcEn, srcFr)

    val all = listOf(
        w("مَن جَدَّ وَجَد، وَمَن زَرَعَ حَصَد", "Who strives finds, and who sows reaps.", "Qui persévère trouve, et qui sème récolte."),
        w("العِلمُ في الصِّغَرِ كالنَّقشِ على الحَجَر", "Learning in youth is like engraving on stone.", "Apprendre jeune, c’est graver dans la pierre."),
        w("الصَّبرُ مِفتاحُ الفَرَج", "Patience is the key to relief.", "La patience est la clé du soulagement."),
        w("خَيرُ الكَلامِ ما قَلَّ ودَلّ", "The best speech is brief and meaningful.", "La meilleure parole est brève et éloquente."),
        w("لِكُلِّ مَقامٍ مَقال", "Every situation has its fitting words.", "À chaque situation sa parole."),
        w("الوَقتُ كالسَّيفِ إن لم تَقطَعهُ قَطَعَك", "Time is like a sword: if you do not cut it, it cuts you.", "Le temps est comme une épée : si tu ne le tranches pas, il te tranche."),
        w("إذا كانَ الكَلامُ مِن فِضّة، فالسُّكوتُ مِن ذَهَب", "If speech is silver, silence is golden.", "Si la parole est d’argent, le silence est d’or."),
        w("في التَّأنّي السَّلامة، وفي العَجَلةِ النَّدامة", "In patience there is safety, and in haste regret.", "Dans la patience, la sécurité ; dans la hâte, le regret."),
        w("الجارَ قَبلَ الدّار، والرَّفيقَ قَبلَ الطَّريق", "Choose the neighbour before the house, and the companion before the road.", "Choisis le voisin avant la maison, et le compagnon avant la route."),
        w("القَناعةُ كَنزٌ لا يَفنى", "Contentment is a treasure that never runs out.", "Le contentement est un trésor inépuisable."),
        w("صَديقُكَ مَن صَدَقَك، لا مَن صَدَّقَك", "Your friend is the one who tells you the truth, not the one who agrees with you.", "Ton ami est celui qui te dit la vérité, non celui qui te donne raison."),
        w("رُبَّ أخٍ لكَ لم تَلِدهُ أُمُّك", "Many a brother of yours was not born to your mother.", "Que de frères ne sont pas nés de ta mère."),
        w("قِيمةُ كُلِّ امرِئٍ ما يُحسِنُه", "The worth of every person is what they do well.", "La valeur de chacun est dans ce qu’il fait bien.", "علي بن أبي طالب", "ʿAli ibn Abi Talib", "ʿAli ibn Abi Talib"),
        w("خَيرُ الأُمورِ أَوسَطُها", "The best of matters is the middle way.", "Le meilleur des choses est le juste milieu."),
        w("ما ضاعَ حَقٌّ وَراءَهُ مُطالِب", "No right is lost while someone claims it.", "Aucun droit ne se perd tant que quelqu’un le réclame."),
        w("اُطلُبِ العِلمَ مِنَ المَهدِ إلى اللَّحد", "Seek knowledge from the cradle to the grave.", "Cherche le savoir du berceau jusqu’à la tombe."),
        w("مَن حَفَرَ حُفرةً لأخيهِ وَقَعَ فيها", "Who digs a pit for his brother falls into it.", "Qui creuse une fosse pour son frère y tombe."),
        w("مَن سارَ على الدَّربِ وَصَل", "Who walks the path arrives.", "Qui suit le chemin arrive."),
        w("لا تُؤَجِّل عَمَلَ اليَومِ إلى الغَد", "Do not put off today’s work until tomorrow.", "Ne remets pas à demain le travail d’aujourd’hui."),
        w("يَدٌ واحِدةٌ لا تُصَفِّق", "One hand cannot clap.", "Une seule main ne peut applaudir."),
        w("التَّواضُعُ مِن شِيَمِ الكِرام", "Humility is a trait of the noble.", "L’humilité est une qualité des nobles."),
        w("مَن عاشَرَ قَوماً أربَعينَ يَوماً صارَ مِنهُم", "Whoever lives among a people for forty days becomes one of them.", "Qui vit quarante jours parmi un peuple devient l’un des siens."),
    )
}
