"""Step-by-step ʿUmrah guide shown in the apps and on the website.

Each step lists the dua cards it links to (item ids in azkar.json) and the hadiths it
relies on as (book, dataset number, phrase); build_hadith_duas.py checks that each
cited hadith contains its phrase before writing the reference labels.
"""

TITLE = {"ar": "دليل العمرة خطوة بخطوة", "en": "ʿUmrah step by step", "fr": "La ʿOumra pas à pas"}
INTRO = {
    "ar": "صفة العمرة كما أدّاها النبي ﷺ، من الإحرام إلى الحلق أو التقصير، مع أذكار كل خطوة.",
    "en": "How to perform ʿUmrah the way the Prophet ﷺ did, from iḥrām to shaving or shortening the hair, with the words to say at each step.",
    "fr": "Comment accomplir la ʿOumra comme le Prophète ﷺ, de l'iḥrām jusqu'au rasage ou au raccourcissement des cheveux, avec les invocations de chaque étape.",
}
NOTE = {
    "ar": "هذا ملخّص مختصر. في المسائل الخاصة (كالحيض، أو نسيان ركن، أو العذر) اسأل أهل العلم الموثوقين.",
    "en": "This is a short summary. For special cases (menstruation, a forgotten step, illness or other excuses) ask a trusted scholar.",
    "fr": "Ceci est un bref résumé. Pour les cas particuliers (menstrues, étape oubliée, maladie ou autre excuse), consultez un savant de confiance.",
}

STEPS = [
    {
        "icon": "prepare",
        "title": {"ar": "١. الاستعداد قبل الميقات", "en": "1. Getting ready before the mīqāt", "fr": "1. Se préparer avant le mīqāt"},
        "text": {
            "ar": "اغتسل وتنظّف. يتطيّب الرجل في بدنه لا في ثياب الإحرام، ويلبس إزارًا ورداءً أبيضين نظيفين بلا مخيط ولا غطاء رأس. تلبس المرأة ثيابها الساترة المعتادة، ولا تلبس النقاب ولا القفازين.",
            "en": "Take a full bath (ghusl) and clean yourself. Men put perfume on the body, not on the iḥrām garments, and wear two clean white sheets (izār and ridāʾ), with no stitched clothing and nothing covering the head. Women wear their usual modest clothing, without a face veil (niqāb) or gloves.",
            "fr": "Faites le grand lavage (ghousl) et purifiez-vous. L'homme se parfume le corps, non les vêtements d'iḥrām, et porte deux pièces blanches et propres (izār et ridāʾ), sans vêtement cousu ni couvre-chef. La femme porte ses vêtements pudiques habituels, sans voile du visage (niqāb) ni gants.",
        },
        "zikr": [],
        "refs": [("tirmidhi", 830, "تجرد لاهلاله واغتسل"), ("bukhari", 1539, "طيب رسول الله"), ("bukhari", 1542, "ما يلبس المحرم"), ("bukhari", 1838, "ولا تنتقب")],
    },
    {
        "icon": "ihram",
        "title": {"ar": "٢. الإحرام من الميقات", "en": "2. Entering iḥrām at the mīqāt", "fr": "2. Entrer en iḥrām au mīqāt"},
        "text": {
            "ar": "عند الميقات (ذو الحليفة لأهل المدينة، والجحفة لأهل الشام، وقرن المنازل لأهل نجد، ويلملم لأهل اليمن، ومن كان دونها فمن حيث أنشأ) انوِ الدخول في العمرة وقل: «لبّيك عمرة». ومن خاف مرضًا أو مانعًا اشترط. وبعد الإحرام يُمنع: قصّ الشعر والأظفار، والطيب، وتغطية الرجل رأسه ولبسه المخيط، والصيد، وعقد النكاح، والجماع.",
            "en": "At the mīqāt (Dhū al-Ḥulayfah for the people of Madinah, al-Juḥfah for the people of Shām, Qarn al-Manāzil for Najd, Yalamlam for Yemen; anyone living closer starts from where they are) make the intention for ʿUmrah and say: “Labbayka ʿumrah” (Here I am for ʿUmrah). If you fear illness or another obstacle, add the condition below. Once in iḥrām it is forbidden to cut hair or nails, use perfume, (for men) cover the head or wear stitched clothes, hunt, conclude a marriage, or have marital relations.",
            "fr": "Au mīqāt (Dhoul-Ḥoulayfa pour les gens de Médine, al-Jouḥfa pour le Shām, Qarn al-Manāzil pour le Najd, Yalamlam pour le Yémen ; celui qui habite plus près commence de chez lui), formulez l'intention de la ʿOumra et dites : « Labbayka ʿoumra » (Me voici pour la ʿOumra). Si vous craignez une maladie ou un empêchement, ajoutez la condition ci-dessous. En iḥrām, il est interdit de couper cheveux ou ongles, de se parfumer, pour l'homme de se couvrir la tête ou de porter des vêtements cousus, de chasser, de conclure un mariage et d'avoir des rapports conjugaux.",
        },
        "zikr": [289],
        "refs": [("bukhari", 1524, "ذا الحليفه"), ("bukhari", 5089, "محلي حيث حبستني")],
    },
    {
        "icon": "talbiyah",
        "title": {"ar": "٣. التلبية", "en": "3. The talbiyah", "fr": "3. La talbiya"},
        "text": {
            "ar": "أكثِر من التلبية من الإحرام حتى تبدأ الطواف. يرفع بها الرجل صوته، وتخفضها المرأة.",
            "en": "Repeat the talbiyah often from the moment of iḥrām until you begin the ṭawāf. Men raise their voices with it; women say it quietly.",
            "fr": "Répétez souvent la talbiya depuis l'iḥrām jusqu'au début du ṭawāf. L'homme élève la voix ; la femme la prononce à voix basse.",
        },
        "zikr": [233, 287, 288],
        "refs": [("muslim", 2951, "فاهل بالتوحيد"), ("abudawud", 1814, "يرفعوا اصواتهم")],
    },
    {
        "icon": "mosque",
        "title": {"ar": "٤. دخول المسجد الحرام", "en": "4. Entering al-Masjid al-Ḥarām", "fr": "4. Entrer à al-Masjid al-Ḥarām"},
        "text": {
            "ar": "ادخل برجلك اليمنى وقل دعاء دخول المسجد، ثم توجّه إلى الحجر الأسود لتبدأ الطواف وأنت على وضوء.",
            "en": "Enter with your right foot and say the supplication for entering the mosque, then head to the Black Stone to begin the ṭawāf. Be in a state of wuḍūʾ.",
            "fr": "Entrez du pied droit en disant l'invocation d'entrée à la mosquée, puis dirigez-vous vers la Pierre noire pour commencer le ṭawāf, en état d'ablutions.",
        },
        "zikr": [20],
        "refs": [("bukhari", 1614, "توضا ثم طاف")],
    },
    {
        "icon": "tawaf",
        "title": {"ar": "٥. الطواف سبعة أشواط", "en": "5. Ṭawāf: seven circuits", "fr": "5. Le ṭawāf : sept tours"},
        "text": {
            "ar": "ابدأ من الحجر الأسود واجعل الكعبة عن يسارك. استلم الحجر وقبّله إن تيسّر، وإلا فأشِر إليه وكبّر في كل شوط. يضطبع الرجل (يكشف كتفه الأيمن) ويرمل (يسرع مع تقارب الخطى) في الأشواط الثلاثة الأولى ويمشي في الأربعة الباقية. استلم الركن اليماني إن تيسّر دون تقبيل، وقل بينه وبين الحجر: ﴿رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً...﴾. وفي سائر الطواف اذكر الله وادعُ بما شئت، فليس لكل شوط دعاء مخصوص.",
            "en": "Start at the Black Stone with the Kaʿbah on your left. Touch and kiss the Stone if you can; otherwise point to it and say “Allāhu akbar” at each circuit. Men uncover the right shoulder (iḍṭibāʿ) and walk briskly with short steps (raml) in the first three circuits, then walk normally for the last four. Touch the Yemeni corner if you can, without kissing it, and between it and the Black Stone say “Rabbanā ātinā fid-dunyā ḥasanah…”. For the rest of the ṭawāf remember Allah and make any supplication you wish; there is no fixed dua for each circuit.",
            "fr": "Commencez à la Pierre noire, la Kaʿba à votre gauche. Touchez et embrassez la Pierre si possible ; sinon désignez-la et dites « Allāhou akbar » à chaque tour. L'homme découvre l'épaule droite (iḍṭibāʿ) et presse le pas à petites enjambées (raml) durant les trois premiers tours, puis marche normalement les quatre derniers. Touchez le coin yéménite si possible, sans l'embrasser, et entre lui et la Pierre noire dites « Rabbanā ātinā fid-dounyā ḥasana… ». Pour le reste du ṭawāf, évoquez Allah et invoquez librement ; il n'y a pas d'invocation propre à chaque tour.",
        },
        "zikr": [234, 235],
        "refs": [("muslim", 2951, "رمل ثلاثا ومشي اربعا"), ("abudawud", 1889, "اضطبع فاستلم وكبر"), ("bukhari", 1609, "الركنين اليمانيين")],
    },
    {
        "icon": "maqam",
        "title": {"ar": "٦. ركعتا الطواف وزمزم", "en": "6. Two rakʿahs and Zamzam", "fr": "6. Deux rakʿas et Zamzam"},
        "text": {
            "ar": "بعد الطواف يغطّي الرجل كتفه، ثم صلِّ ركعتين خلف مقام إبراهيم إن تيسّر وإلا ففي أي مكان من المسجد، تقرأ فيهما ﴿قُلْ يَا أَيُّهَا الْكَافِرُونَ﴾ و﴿قُلْ هُوَ اللَّهُ أَحَدٌ﴾. ثم اشرب من ماء زمزم، وارجع إلى الحجر فاستلمه إن تيسّر.",
            "en": "After the ṭawāf men cover the shoulder again. Pray two rakʿahs behind the Station of Ibrāhīm if you can, or anywhere in the mosque, reciting Qul yā ayyuhā al-kāfirūn and Qul huwa Allāhu aḥad. Then drink Zamzam water, and go back to touch the Black Stone if you can.",
            "fr": "Après le ṭawāf, l'homme recouvre son épaule. Priez deux rakʿas derrière la Station d'Ibrāhīm si possible, sinon n'importe où dans la mosquée, en récitant Qoul yā ayyouhā al-kāfiroun et Qoul houwa Allāhou aḥad. Buvez ensuite de l'eau de Zamzam, puis revenez toucher la Pierre noire si possible.",
        },
        "zikr": [290],
        "refs": [("muslim", 2951, "ثم رجع الي الركن فاستلمه"), ("ibnmajah", 3062, "ماء زمزم لما شرب له")],
    },
    {
        "icon": "sai",
        "title": {"ar": "٧. السعي بين الصفا والمروة", "en": "7. Saʿy between Ṣafā and Marwah", "fr": "7. Le saʿy entre Ṣafā et Marwa"},
        "text": {
            "ar": "اصعد الصفا واقرأ: ﴿إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ﴾ «أبدأ بما بدأ الله به»، ثم استقبل الكعبة فوحّد الله وكبّره وادعُ، ثلاث مرات. انزل إلى المروة، ويسعى الرجل (يسرع) بين العلمين الأخضرين، وافعل على المروة كما فعلت على الصفا. الذهاب شوط والرجوع شوط، فتكمل سبعة أشواط تنتهي على المروة.",
            "en": "Climb Ṣafā and recite “Indeed, Ṣafā and Marwah are among the symbols of Allah” and say “I begin with what Allah began with”. Face the Kaʿbah, declare Allah's oneness and greatness and make dua, three times. Walk to Marwah; men hurry between the two green markers. On Marwah do as you did on Ṣafā. Going is one lap and returning is another, so the seven laps end on Marwah.",
            "fr": "Montez sur Ṣafā et récitez « Ṣafā et Marwa sont vraiment parmi les rites d'Allah » puis dites « Je commence par ce par quoi Allah a commencé ». Face à la Kaʿba, proclamez l'unicité et la grandeur d'Allah et invoquez, trois fois. Marchez vers Marwa ; l'homme presse le pas entre les deux repères verts. Sur Marwa, faites comme sur Ṣafā. L'aller compte pour un trajet et le retour pour un autre : les sept trajets se terminent sur Marwa.",
        },
        "zikr": [236],
        "refs": [("muslim", 2951, "ابدا بما بدا الله به"), ("muslim", 2951, "انصبت قدماه في بطن الوادي سعي"), ("muslim", 2951, "اخر طوافه علي المروه")],
    },
    {
        "icon": "halq",
        "title": {"ar": "٨. الحلق أو التقصير", "en": "8. Shaving or shortening the hair", "fr": "8. Se raser ou raccourcir les cheveux"},
        "text": {
            "ar": "يحلق الرجل رأسه أو يقصّر من جميعه، والحلق أفضل. وتقصّ المرأة من أطراف شعرها قدر أنملة، ولا تحلق.",
            "en": "Men shave the head or shorten the hair from all over it; shaving is better. Women cut a small amount from the ends of their hair (about a fingertip's length) and do not shave.",
            "fr": "L'homme se rase la tête ou raccourcit ses cheveux sur toute la tête ; le rasage est meilleur. La femme coupe un peu de l'extrémité de ses cheveux (environ une phalange) et ne se rase pas.",
        },
        "zikr": [291],
        "refs": [("bukhari", 1727, "ارحم المحلقين"), ("abudawud", 1985, "انما علي النساء التقصير")],
    },
    {
        "icon": "done",
        "title": {"ar": "٩. تمّت عمرتك", "en": "9. Your ʿUmrah is complete", "fr": "9. Votre ʿOumra est accomplie"},
        "text": {
            "ar": "بالحلق أو التقصير تمّت العمرة وحلّ لك كل ما مُنع بالإحرام. تقبّل الله منك. قال النبي ﷺ: «العمرة إلى العمرة كفّارة لما بينهما»، وقال: «عمرة في رمضان تعدل حجة».",
            "en": "With the shaving or shortening your ʿUmrah is complete, and everything forbidden by iḥrām is allowed again. May Allah accept it from you. The Prophet ﷺ said: “One ʿUmrah to the next is an expiation for what is between them,” and: “An ʿUmrah in Ramadan equals a Hajj.”",
            "fr": "Avec le rasage ou le raccourcissement, votre ʿOumra est accomplie et tout ce que l'iḥrām interdisait redevient permis. Qu'Allah l'accepte de vous. Le Prophète ﷺ a dit : « Une ʿOumra à l'autre expie ce qui est entre elles », et : « Une ʿOumra pendant le Ramadan équivaut à un Hajj. »",
        },
        "zikr": [],
        "refs": [("bukhari", 1773, "العمره الي العمره كفاره"), ("muslim", 3289, "العمره الي العمره"), ("bukhari", 1782, "عمره في رمضان حجه"), ("tirmidhi", 939, "عمره في رمضان تعدل حجه")],
    },
]
