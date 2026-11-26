"""Builds the islamicazkar.net landing page in English (/), Arabic (/ar/) and French (/fr/).

    python3 tools/build_landing.py [<raw screenshots dir>]

Writes website/index.html, website/ar/index.html and website/fr/index.html from the template and
texts below; the styles and script are website/landing.css and website/landing.js. With a folder of
raw app screenshots (the "appstore-raw" branch: iPhone-6.9/<lang>-<screen>.png), it also refreshes
the phone images in website/img/landing/. Icons: Phosphor (tools/landing/icons, MIT).
The numbers on the page are counted from website/data/azkar.json at build time.
"""
import html, json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, "website")
SCREENS = ["reader", "tasbih", "record", "widgets"]
SITE = "https://islamicazkar.net"


def icon(name, flip=False):
    s = open(os.path.join(ROOT, "tools", "landing", "icons", name + ".svg")).read().strip()
    cls = "ic flip" if flip else "ic"
    return re.sub(r"<svg ", f'<svg aria-hidden="true" focusable="false" class="{cls}" ', s, count=1)


def images(raw):
    from PIL import Image
    out = os.path.join(WEB, "img", "landing")
    os.makedirs(out, exist_ok=True)
    for lang in ("en", "ar", "fr"):
        for s in SCREENS:
            im = Image.open(os.path.join(raw, "iPhone-6.9", f"{lang}-{s}.png")).convert("RGB")
            for w, tag in ((560, ""), (400, "-s")):
                im.resize((w, round(im.height * w / im.width)), Image.LANCZOS).save(
                    os.path.join(out, f"{lang}-{s}{tag}.jpg"), "JPEG", quality=80, optimize=True, progressive=True)
    print("images refreshed")


def cat_names():
    """The category names of the web app (website/app.js), in the order ar, en, fr."""
    src = open(os.path.join(WEB, "app.js"), encoding="utf-8").read()
    maps = [dict(re.findall(r'(\w+): "([^"]*)"', m)) for m in re.findall(r"cat: \{([^}]*)\}", src)]
    return dict(zip(("ar", "en", "fr"), maps))


def reading():
    d = json.load(open(os.path.join(WEB, "data", "azkar.json"), encoding="utf-8"))
    ch = {c["id"]: c for c in d["chapters"]}
    return {"morning": len(d["morning"]), "evening": len(d["evening"]), "sleep": len(ch[28]["items"]), "prayer": len(ch[25]["items"]),
            "cats": [(c["key"], len(c["chapters"])) for c in d["categories"]], "guide": bool(d.get("guides"))}


def plural(form, n):
    """English and French: one form with {n}. Arabic: (one, two, 3 to 10, 11 and more)."""
    if isinstance(form, str):
        return form.format(n=n)
    one, two, few, many = form
    return one if n == 1 else two if n == 2 else (few if 3 <= n % 100 <= 10 else many).format(n=n)


def counts():
    d = json.load(open(os.path.join(WEB, "data", "azkar.json"), encoding="utf-8"))
    return {"items": sum(len(c["items"]) for c in d["chapters"]), "chapters": len(d["chapters"])}


AR_DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")

T = {
"en": dict(
    read_nav="Read azkar", read_h="Read the azkar here", read_p="No download needed. Open any reading in your browser, with the counter, transliteration, meaning and your record.", read_all="Open the full reader", read_cats="All categories", r_morning="Morning azkar", r_evening="Evening azkar", r_sleep="Before sleep", r_prayer="After prayer", r_n="{n} azkar", r_ch="{n} chapters", r_guide="Umrah step by step",
    dir="ltr", path="/", title="Islamic Azkar: morning and evening azkar with their sources",
    desc="The complete Fortress of the Muslim in Arabic with transliteration and meaning in English and French. A counter, your azkar record, prayer times and widgets. Free, no ads, no account.",
    skip="Skip to content", features="Features", how="How it works", faq="Questions", download="Download free", menu="Menu", close="Close menu", home="Islamic Azkar home",
    h1="Your morning and evening azkar,<br>authentic and always with you",
    sub="The complete Fortress of the Muslim in Arabic, with transliteration and meaning in English and French. Every dhikr shows its source, and a counter keeps your place.",
    read="Read the morning azkar now",
    proof=["Free, with no ads", "No account", "iPhone, iPad, Mac, Apple TV, Android and Windows"],
    alt_reader="Islamic Azkar showing Ayat al Kursi in Arabic with its English meaning", alt_tasbih="The tasbih counter at 21 of 33",
    alt_record="The azkar record calendar with the morning azkar completed", alt_widgets="Prayer time and Hijri date widgets in Arabic calligraphy",
    facts_label="What is inside",
    facts=[("{items}", "azkar and duas"), ("{chapters}", "chapters for every moment"), ("99", "Names of Allah with meanings"), ("9", "prayer time methods"), ("3", "languages: Arabic, English, French")],
    tagline="Open the app after Fajr, read each dhikr and tap to count. When you finish, it closes with a prayer and your streak grows by one day.",
    f1=("AUTHENTIC SOURCES", "Know where every word comes from", "Each dhikr names its source, from the Quran and the sahih collections. The Arabic is copied word for word from the source text."),
    sources=["Quran", "Bukhari", "Muslim", "Abu Dawud", "Tirmidhi", "Nasai"],
    f2=("COUNTER AND TASBIH", "Tap anywhere to count and never lose your place",
        [("Counts each dhikr", " to its number: 3, 7, 33 or 100, then moves to the next."), ("Tasbih for 33, 99, 100", " or no limit, with a gentle tick on each bead."), ("Picks up where you stopped", " if you close the app halfway.")]),
    f3=("YOUR RECORD", "See every day you kept your azkar", "A calendar marks each completed reading, with your streak and monthly totals. Turn on sync to see the same record on your phone, tablet, computer and the website, using a private code instead of an account."),
    f4=("PRAYER TIMES AND WIDGETS", "Prayer times and azkar on your Home Screen",
        [("Prayer times for your city", " with Umm al Qura, Dubai, Egyptian, ISNA, France and four more methods, plus Hanafi Asr."), ("Widgets in Arabic calligraphy", ": Ruqah, Naskh, Kufic or Nastaliq."), ("Reminders", " for the morning and evening azkar at the time you choose.")]),
    how_h="Three steps each morning", how_p="The app suggests the right reading for the time of day.", step="STEP",
    steps=[("Open the suggested reading", "After Fajr the morning azkar are marked Now; after Asr, the evening ones."), ("Read and tap to count", "Each dhikr shows how many times to say it and counts with you."), ("Finish with a prayer", "A short dua closes the reading and your record adds the day.")],
    dl_h="Get Islamic Azkar free", dl_p="Every version has the same azkar and keeps your record in sync. Downloads always fetch the newest release.",
    free=["No ads", "No account, no tracking", "Sadaqa jariyah"],
    p_apple=("iPhone, iPad, Mac and Apple TV", "Coming to the App Store after Apple's review"),
    p_android=("Android and Google TV", "Direct download · Android 8 or later"),
    p_windows=("Windows 10 and 11", "Installer, no administrator rights needed"),
    p_mac=("Mac, direct download", "Signed and notarized by Apple · macOS 15 or later"),
    p_web=("In your browser", "Nothing to install"),
    faqs=[("Where do the azkar come from?", "From the Fortress of the Muslim, the Quran and hadith graded sahih or hasan. Each dhikr shows its reference."),
          ("Is it really free?", "Yes. There are no ads, no purchases and no subscription. The app is a sadaqa jariyah."),
          ("Do I need an account?", "No. Everything stays on your device. Sync is optional and uses a private code, never your name or email."),
          ("Can I use it in English or French?", "Yes. The app speaks Arabic, English and French, with transliteration and meaning under each dhikr."),
          ("How are prayer times calculated?", "On your device, for your location or a chosen city, with nine methods including Umm al Qura and Dubai, and Hanafi Asr."),
          ("How do I delete my data?", 'Only synced records are kept on our server, and you can erase them at <a href="/delete/">islamicazkar.net/delete</a> at any time.')],
    final="Start tomorrow morning<br>with your azkar ready",
    foot="Islamic Azkar · a sadaqa jariyah on behalf of Mohamad Al-Koussa, Dima Ghannoum and their families",
    ded_tr="SADAQA JARIYAH", ded_names='<p class="names">On behalf of <span class="nw">Mohamad Al-Koussa</span>, <span class="nw">Dima Ghannoum</span> and their families</p>', ded_dua="O Allah, place it in the scale of their good deeds.", ded_hadith="“When a person dies, their deeds end except for three: an ongoing charity…” Muslim",
    ded_label="Sadaqa jariyah", ded="This app is a sadaqa jariyah on behalf of Mohamad Al-Koussa, Dima Ghannoum and their families. Please remember them in your duas.", support="Support", privacy="Privacy", delete="Delete your data"),
"ar": dict(
    read_nav="اقرأ الأذكار", read_h="اقرأ الأذكار هنا", read_p="لا حاجة إلى التنزيل. افتح أي ورد في متصفحك، مع العدّاد والنطق والمعنى وسجلّك.", read_all="افتح القارئ كاملًا", read_cats="كل الأقسام", r_morning="أذكار الصباح", r_evening="أذكار المساء", r_sleep="أذكار النوم", r_prayer="أذكار بعد الصلاة", r_n=("ذكر واحد", "ذكران", "{n} أذكار", "{n} ذكرًا"), r_ch=("باب واحد", "بابان", "{n} أبواب", "{n} بابًا"), r_guide="دليل العمرة خطوة بخطوة",
    dir="rtl", path="/ar/", title="أذكار إسلامية: أذكار الصباح والمساء بمصادرها",
    desc="حصن المسلم كاملًا بالعربية مع النطق والمعنى بالإنجليزية والفرنسية. عدّاد وسجلّ للأذكار ومواقيت الصلاة وأدوات للشاشة. مجاني بلا إعلانات ولا حساب.",
    skip="تخطَّ إلى المحتوى", features="المزايا", how="طريقة الاستخدام", faq="أسئلة", download="تنزيل مجاني", menu="القائمة", close="إغلاق القائمة", home="أذكار إسلامية، الصفحة الرئيسية",
    h1="أذكار الصباح والمساء،<br>صحيحةً ومعك أينما كنت",
    sub="حصن المسلم كاملًا بالعربية، مع النطق والمعنى بالإنجليزية والفرنسية. لكل ذكر مصدره، وعدّادٌ يحفظ موضعك.",
    read="اقرأ أذكار الصباح الآن",
    proof=["مجاني بلا إعلانات", "بلا حساب", "آيفون وآيباد وماك وApple TV وأندرويد وويندوز"],
    alt_reader="أذكار إسلامية تعرض آية الكرسي مع معناها", alt_tasbih="عدّاد المسبحة عند ٢١ من ٣٣",
    alt_record="تقويم سجلّ الأذكار وقد أُتمّت أذكار الصباح", alt_widgets="أدوات مواقيت الصلاة والتاريخ الهجري بالخط العربي",
    facts_label="ما في التطبيق",
    facts=[("{items}", "ذكرًا ودعاءً"), ("{chapters}", "بابًا لكل لحظة"), ("99", "اسمًا من أسماء الله الحسنى بمعانيها"), ("9", "طرق لحساب مواقيت الصلاة"), ("3", "لغات: العربية والإنجليزية والفرنسية")],
    tagline="افتح التطبيق بعد الفجر، واقرأ كل ذكر واضغط لتعدّ. وحين تُتمّ وِردك يُختم بدعاء، ويزيد عدد أيامك المتتالية يومًا.",
    f1=("مصادر صحيحة", "اعرف من أين جاءت كل كلمة", "لكل ذكر مصدره من القرآن الكريم وكتب الحديث الصحيحة، والنص العربي منقول حرفيًّا من مصدره."),
    sources=["القرآن الكريم", "البخاري", "مسلم", "أبو داود", "الترمذي", "النسائي"],
    f2=("العدّاد والمسبحة", "اضغط في أي مكان لتعدّ، ولا تفقد موضعك أبدًا",
        [("يعدّ كل ذكر", " إلى عدده: ٣ أو ٧ أو ٣٣ أو ١٠٠، ثم ينتقل إلى ما بعده."), ("مسبحة لـ ٣٣ و٩٩ و١٠٠", " أو بلا حدّ، مع اهتزاز لطيف عند كل حبة."), ("يكمل من حيث توقفت", " إن أغلقت التطبيق في منتصف الورد.")]),
    f3=("سجلّك", "انظر كل يوم حافظت فيه على أذكارك", "تقويم يعلّم كل وِرد أتممته، مع الأيام المتتالية ومجموع الشهر. فعّل المزامنة لترى السجلّ نفسه على هاتفك وجهازك اللوحي وحاسوبك والموقع، برمز خاص بدل الحساب."),
    f4=("مواقيت الصلاة والأدوات", "مواقيت الصلاة والأذكار على شاشتك الرئيسية",
        [("مواقيت الصلاة لمدينتك", " بطريقة أم القرى أو دبي أو الهيئة المصرية أو أمريكا الشمالية أو فرنسا وأربع طرق أخرى، مع العصر على المذهب الحنفي."), ("أدوات بالخط العربي", ": الرقعة أو النسخ أو الكوفي أو النستعليق."), ("تذكيرات", " بأذكار الصباح والمساء في الوقت الذي تختاره.")]),
    how_h="ثلاث خطوات كل صباح", how_p="يقترح التطبيق الورد المناسب لوقت اليوم.", step="الخطوة",
    steps=[("افتح الورد المقترح", "بعد الفجر تظهر أذكار الصباح بعلامة «الآن»، وبعد العصر أذكار المساء."), ("اقرأ واضغط لتعدّ", "يُظهر كل ذكر عدد مرات قوله، ويعدّ معك."), ("اختم بدعاء", "دعاء قصير يختم الورد، ويُضاف اليوم إلى سجلّك.")],
    dl_h="حمّل أذكار إسلامية مجانًا", dl_p="في كل النسخ الأذكار نفسها، ويُزامَن سجلّك بينها. التنزيلات تأتي دائمًا بأحدث إصدار.",
    free=["بلا إعلانات", "بلا حساب ولا تتبّع", "صدقة جارية"],
    p_apple=("آيفون وآيباد وماك وApple TV", "قريبًا على App Store بعد مراجعة Apple"),
    p_android=("أندرويد وGoogle TV", "تنزيل مباشر · أندرويد ٨ أو أحدث"),
    p_windows=("ويندوز ١٠ و١١", "مثبّت لا يحتاج صلاحيات المسؤول"),
    p_mac=("ماك، تنزيل مباشر", "موقّع وموثّق من Apple · macOS 15 أو أحدث"),
    p_web=("في متصفحك", "لا حاجة إلى التثبيت"),
    faqs=[("من أين الأذكار؟", "من حصن المسلم والقرآن الكريم والأحاديث الصحيحة والحسنة، ويظهر مرجع كل ذكر تحته."),
          ("هل هو مجاني حقًّا؟", "نعم، بلا إعلانات ولا مشتريات ولا اشتراك. التطبيق صدقة جارية."),
          ("هل أحتاج إلى حساب؟", "لا. كل شيء يبقى على جهازك. المزامنة اختيارية وتعمل برمز خاص، لا باسمك ولا ببريدك."),
          ("هل يمكنني استخدامه بالإنجليزية أو الفرنسية؟", "نعم. التطبيق بالعربية والإنجليزية والفرنسية، مع النطق والمعنى تحت كل ذكر."),
          ("كيف تُحسب مواقيت الصلاة؟", "على جهازك، لموقعك أو لمدينة تختارها، بتسع طرق منها أم القرى ودبي، مع العصر على المذهب الحنفي."),
          ("كيف أحذف بياناتي؟", 'لا يُحفظ على خادمنا إلا سجلّ من فعّل المزامنة، ويمكنك حذفه متى شئت من <a href="/delete/">islamicazkar.net/delete</a>.')],
    final="ابدأ صباح الغد<br>وأذكارك جاهزة",
    foot="أذكار إسلامية · صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم",
    ded_tr="", ded_names='<p class="names ar" lang="ar">عن محمد الكوسا وديمة غنوم وعائلاتهم</p>', ded_dua="", ded_hadith="",
    ded_label="صدقة جارية", ded="هذا التطبيق صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم، فلا تنسوهم من صالح دعائكم.", support="الدعم", privacy="الخصوصية", delete="احذف بياناتك"),
"fr": dict(
    read_nav="Lire les adhkar", read_h="Lisez les adhkar ici", read_p="Rien à télécharger. Ouvrez n'importe quelle lecture dans votre navigateur, avec le compteur, la translittération, la traduction et votre historique.", read_all="Ouvrir le lecteur complet", read_cats="Toutes les catégories", r_morning="Adhkar du matin", r_evening="Adhkar du soir", r_sleep="Avant de dormir", r_prayer="Après la prière", r_n="{n} adhkar", r_ch="{n} chapitres", r_guide="La Oumra pas à pas",
    dir="ltr", path="/fr/", title="Islamic Azkar : les adhkar du matin et du soir avec leurs sources",
    desc="Toute la Citadelle du musulman en arabe, avec translittération et traduction en français et en anglais. Un compteur, votre historique, les horaires de prière et des widgets. Gratuit, sans publicité, sans compte.",
    skip="Aller au contenu", features="Fonctions", how="Mode d'emploi", faq="Questions", download="Télécharger", menu="Menu", close="Fermer le menu", home="Accueil Islamic Azkar",
    h1="Vos adhkar du matin et du soir,<br>authentiques et toujours avec vous",
    sub="Toute la Citadelle du musulman en arabe, avec translittération et traduction en français et en anglais. Chaque dhikr indique sa source, et un compteur garde votre place.",
    read="Lire les adhkar du matin",
    proof=["Gratuit, sans publicité", "Sans compte", "iPhone, iPad, Mac, Apple TV, Android et Windows"],
    alt_reader="Islamic Azkar affichant Ayat al Kursi avec sa traduction", alt_tasbih="Le chapelet à 21 sur 33",
    alt_record="Le calendrier de l'historique avec les adhkar du matin terminés", alt_widgets="Widgets des horaires de prière et de la date hégirienne en calligraphie arabe",
    facts_label="Ce que contient l'application",
    facts=[("{items}", "adhkar et invocations"), ("{chapters}", "chapitres pour chaque moment"), ("99", "Noms d'Allah avec leur sens"), ("9", "méthodes de calcul des prières"), ("3", "langues : arabe, anglais, français")],
    tagline="Ouvrez l'application après Fajr, lisez chaque dhikr et touchez pour compter. À la fin, une invocation conclut la lecture et votre série gagne un jour.",
    f1=("SOURCES AUTHENTIQUES", "Sachez d'où vient chaque mot", "Chaque dhikr indique sa source, du Coran et des recueils authentiques. Le texte arabe est repris mot pour mot de sa source."),
    sources=["Coran", "Bukhari", "Muslim", "Abu Dawud", "Tirmidhi", "Nasai"],
    f2=("COMPTEUR ET CHAPELET", "Touchez n'importe où pour compter, sans jamais perdre votre place",
        [("Compte chaque dhikr", " jusqu'à son nombre : 3, 7, 33 ou 100, puis passe au suivant."), ("Chapelet de 33, 99, 100", " ou sans limite, avec une légère vibration à chaque grain."), ("Reprend où vous vous êtes arrêté", " si vous fermez l'application en cours de lecture.")]),
    f3=("VOTRE HISTORIQUE", "Voyez chaque jour où vous avez lu vos adhkar", "Un calendrier marque chaque lecture terminée, avec vos jours d'affilée et le total du mois. Activez la synchronisation pour retrouver le même historique sur votre téléphone, votre tablette, votre ordinateur et le site, avec un code privé au lieu d'un compte."),
    f4=("HORAIRES ET WIDGETS", "Les horaires de prière et les adhkar sur votre écran d'accueil",
        [("Les horaires de votre ville", " selon Umm al Qura, Dubaï, l'Égypte, l'ISNA, la France et quatre autres méthodes, avec l'Asr hanafite."), ("Des widgets en calligraphie arabe", " : roq'a, naskh, coufique ou nasta'lîq."), ("Des rappels", " pour les adhkar du matin et du soir, à l'heure de votre choix.")]),
    how_h="Trois étapes chaque matin", how_p="L'application propose la lecture qui correspond au moment de la journée.", step="ÉTAPE",
    steps=[("Ouvrez la lecture proposée", "Après Fajr, les adhkar du matin sont marqués Maintenant ; après Asr, ceux du soir."), ("Lisez et touchez pour compter", "Chaque dhikr indique combien de fois le dire et compte avec vous."), ("Terminez par une invocation", "Une courte douâ conclut la lecture et votre historique ajoute le jour.")],
    dl_h="Téléchargez Islamic Azkar gratuitement", dl_p="Chaque version contient les mêmes adhkar et synchronise votre historique. Les téléchargements donnent toujours la dernière version.",
    free=["Sans publicité", "Sans compte ni suivi", "Sadaqa jariya"],
    p_apple=("iPhone, iPad, Mac et Apple TV", "Bientôt sur l'App Store, après la validation d'Apple"),
    p_android=("Android et Google TV", "Téléchargement direct · Android 8 ou plus récent"),
    p_windows=("Windows 10 et 11", "Installateur sans droits d'administrateur"),
    p_mac=("Mac, téléchargement direct", "Signé et notarié par Apple · macOS 15 ou plus récent"),
    p_web=("Dans votre navigateur", "Rien à installer"),
    faqs=[("D'où viennent les adhkar ?", "De la Citadelle du musulman, du Coran et des hadiths jugés sahih ou hasan. Chaque dhikr indique sa référence."),
          ("Vraiment gratuit ?", "Oui. Ni publicité, ni achat, ni abonnement. L'application est une sadaqa jariya."),
          ("Besoin d'un compte ?", "Non. Tout reste sur votre appareil. La synchronisation est facultative et utilise un code privé, jamais votre nom ni votre adresse électronique."),
          ("Disponible en français ?", "Oui. L'application est en arabe, en français et en anglais, avec translittération et traduction sous chaque dhikr."),
          ("Comment sont calculés les horaires de prière ?", "Sur votre appareil, pour votre position ou une ville choisie, avec neuf méthodes dont Umm al Qura et Dubaï, et l'Asr hanafite."),
          ("Comment supprimer mes données ?", 'Seul l\'historique synchronisé est conservé sur notre serveur, et vous pouvez l\'effacer à tout moment sur <a href="/delete/">islamicazkar.net/delete</a>.')],
    final="Commencez demain matin<br>avec vos adhkar prêts",
    foot="Islamic Azkar · une sadaqa jariya au nom de Mohamad Al-Koussa, Dima Ghannoum et leurs familles",
    ded_tr="SADAQA JARIYA", ded_names='<p class="names">Au nom de <span class="nw">Mohamad Al-Koussa</span>, <span class="nw">Dima Ghannoum</span> et leurs familles</p>', ded_dua="Ô Allah, place-la dans la balance de leurs bonnes actions.", ded_hadith="« Quand l\'homme meurt, ses œuvres cessent sauf trois : une aumône continue… » Muslim",
    ded_label="Sadaqa jariya", ded="Cette application est une sadaqa jariya au nom de Mohamad Al-Koussa, Dima Ghannoum et leurs familles. Pensez à eux dans vos douas.", support="Assistance", privacy="Confidentialité", delete="Supprimer vos données"),
}


def page(lang):
    t = T[lang]
    c = counts()
    num = (lambda s: s.translate(AR_DIGITS)) if lang == "ar" else (lambda s: s)
    e = html.escape
    img = lambda s, alt, cls="": (f'<img src="/img/landing/{lang}-{s}.jpg" srcset="/img/landing/{lang}-{s}-s.jpg 400w, /img/landing/{lang}-{s}.jpg 560w" '
                                  f'sizes="(max-width: 720px) 240px, 288px" width="560" height="1217" alt="{e(alt)}" loading="{"eager" if cls else "lazy"}" decoding="async">')
    cur = ' aria-current="true"'
    langs = "".join(f'<a href="{T[l]["path"]}" hreflang="{l}" lang="{l}"{cur if l == lang else ""}>{n}</a>'
                    for l, n in (("en", "EN"), ("ar", "ع"), ("fr", "FR")))
    app = f"/app/?lang={lang}"
    rd, names = reading(), cat_names()[lang]
    tiles = "".join(
        f'<a class="rtile" href="{app}#/read/{href}">{icon(ic)}<span class="title-2">{e(t[key])}</span>'
        f'<span class="body-6 muted">{num(plural(t["r_n"], rd[k]))}</span><span class="go">{icon("arrow-right", flip=True)}</span></a>'
        for k, key, href, ic in (("morning", "r_morning", "morning", "sun"), ("evening", "r_evening", "evening", "moon"),
                                 ("sleep", "r_sleep", "c28", "bed"), ("prayer", "r_prayer", "c25", "mosque")))
    crow = lambda href, name, meta: (f'<a class="crow" href="{href}"><span class="title-3">{e(name)}</span>'
                                     f'<span class="body-6 muted">{meta}</span>{icon("caret-right", flip=True)}</a>')
    cats = "".join(crow(f"{app}#/cat/{k}", names.get(k, k), num(plural(t["r_ch"], n)) if n > 1 else "") for k, n in rd["cats"])
    if rd["guide"]:
        cats += crow(f"{app}#/umrah", t["r_guide"], "")
    li = lambda items: "".join(f'<li>{icon("check")}<span><b>{num(e(b))}</b>{num(e(r))}</span></li>' for b, r in items)
    facts = "".join(f'<div><b>{num(n.format(**c))}</b><span class="body-6 muted">{e(l)}</span></div>' for n, l in t["facts"])
    steps = "".join(f'<div class="step"><span class="n caption-3">{e(t["step"])} {num(str(i + 1))}</span>'
                    + ('<p class="ar" lang="ar">سُبْحَانَ اللَّهِ وَبِحَمْدِهِ</p>' if i == 1 else "")
                    + f'<h3 class="title-2">{e(h)}</h3><p class="body-6 muted">{e(p)}</p></div>' for i, (h, p) in enumerate(t["steps"]))
    faqs = "".join(f'<div><dt class="title-3">{e(q)}</dt><dd class="body-6">{a}</dd></div>' for q, a in t["faqs"])
    go = f'<span class="go">{icon("arrow-right", flip=True)}</span>'
    plat = lambda ic, key, href: (f'<a class="platform" href="{href}">{icon(ic)}<span><span class="title-3">{e(t[key][0])}</span><br><span class="body-6 muted">{e(t[key][1])}</span></span>{go}</a>')
    alternates = "\n".join(f'<link rel="alternate" hreflang="{l}" href="{SITE}{T[l]["path"]}">' for l in T) + f'\n<link rel="alternate" hreflang="x-default" href="{SITE}/">'
    jsonld = json.dumps({"@context": "https://schema.org", "@type": "FAQPage", "inLanguage": lang, "mainEntity": [
        {"@type": "Question", "name": q, "acceptedAnswer": {"@type": "Answer", "text": re.sub(r"<[^>]+>", "", a)}} for q, a in t["faqs"]]}, ensure_ascii=False)
    return f'''<!doctype html>
<html lang="{lang}" dir="{t["dir"]}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>{e(t["title"])}</title>
<meta name="description" content="{e(t["desc"])}">
<meta name="theme-color" content="#08231a">
<link rel="canonical" href="{SITE}{t["path"]}">
{alternates}
<link rel="icon" href="/img/icon.png">
<link rel="apple-touch-icon" href="/img/icon.png">
<meta property="og:title" content="{e(t["title"])}">
<meta property="og:description" content="{e(t["desc"])}">
<meta property="og:image" content="{SITE}/img/og.jpg">
<meta property="og:url" content="{SITE}{t["path"]}">
<meta property="og:type" content="website">
<meta name="twitter:card" content="summary_large_image">
<link rel="preload" href="/fonts/manrope-latin.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="/landing.css">
<script type="application/ld+json">{jsonld}</script>
</head>
<body>
<a class="skip button-sm" href="#main">{e(t["skip"])}</a>

<nav class="nav" aria-label="{e(t["menu"])}">
  <a class="brand" href="{t["path"]}" aria-label="{e(t["home"])}"><img src="/img/icon.png" alt="" width="32" height="32"><span class="ar" lang="ar">أذكار</span></a>
  <div class="nav-links button-sm"><a href="#read">{e(t["read_nav"])}</a><a href="#features">{e(t["features"])}</a><a href="#how">{e(t["how"])}</a><a href="#faq">{e(t["faq"])}</a></div>
  <div class="langs button-sm" aria-label="Language">{langs}</div>
  <a class="btn btn-primary button-sm" href="#download">{icon("download-simple")}{e(t["download"])}</a>
  <button class="menu-btn" id="menu-btn" type="button" aria-label="{e(t["menu"])}" aria-expanded="false" aria-controls="menu"><i></i><i></i><i></i></button>
</nav>
<div class="menu" id="menu" aria-hidden="true">
  <button class="menu-close" id="menu-close" type="button" aria-label="{e(t["close"])}">{icon("x")}</button>
  <a href="#read">{e(t["read_nav"])}</a><a href="#features">{e(t["features"])}</a><a href="#how">{e(t["how"])}</a><a href="#download">{e(t["download"])}</a>
  <div class="langs button-md" aria-label="Language">{langs}</div>
</div>

<main id="main" class="wrap">
  <header class="hero" id="top">
    <div class="hero-copy">
      <p class="ar basmala" lang="ar">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</p>
      <h1 class="heading-1"><span class="grad">{t["h1"]}</span></h1>
      <p class="sub">{e(t["sub"])}</p>
      <div class="actions">
        <a class="btn btn-primary button-md" href="#download">{icon("download-simple")}{e(t["download"])}</a>
        <a class="btn btn-text button-md" href="{app}#/read/morning">{e(t["read"])}<span class="nudge">{icon("arrow-right", flip=True)}</span></a>
      </div>
      <div class="proof caption-1">{"".join(f"<span>{icon('check')}{e(p)}</span>" for p in t["proof"])}</div>
    </div>
    <div class="stage">
      <div class="phone">{img("reader", t["alt_reader"], "eager")}</div>
      <div class="float">{img("tasbih", t["alt_tasbih"], "eager")}</div>
    </div>
  </header>

  <div class="facts reveal" aria-label="{e(t["facts_label"])}">{facts}</div>

  <section class="read" id="read" aria-labelledby="read-title">
    <div class="read-head reveal">
      <div><h2 class="heading-2" id="read-title">{e(t["read_h"])}</h2><p class="body-3 muted">{e(t["read_p"])}</p></div>
      <a class="btn btn-primary button-md" href="{app}">{icon("book-open")}{e(t["read_all"])}</a>
    </div>
    <div class="rtiles reveal">{tiles}</div>
    <h3 class="title-2 cats-title reveal">{e(t["read_cats"])}</h3>
    <div class="cats reveal">{cats}</div>
  </section>

  <section class="tagline" aria-label="{e(t["features"])}"><p id="tagline">{e(t["tagline"])}</p></section>

  <section class="features" id="features" aria-label="{e(t["features"])}">
    <article class="feature reveal">
      <div class="copy">
        <span class="label caption-3">{icon("book-open-text")}{e(t["f1"][0])}</span>
        <h2 class="heading-4">{e(t["f1"][1])}</h2>
        <p class="body-3 muted">{e(t["f1"][2])}</p>
        <div class="sources caption-1">{"".join(f"<span>{e(s)}</span>" for s in t["sources"])}</div>
      </div>
      <div class="media"><div class="phone">{img("reader", t["alt_reader"])}</div></div>
    </article>
    <article class="feature reveal">
      <div class="copy">
        <span class="label caption-3">{icon("hand-tap")}{e(t["f2"][0])}</span>
        <h2 class="heading-4">{e(t["f2"][1])}</h2>
        <ul class="body-3">{li(t["f2"][2])}</ul>
      </div>
      <div class="media"><div class="phone">{img("tasbih", t["alt_tasbih"])}</div></div>
    </article>
    <article class="feature reveal">
      <div class="copy">
        <span class="label caption-3">{icon("calendar-check")}{e(t["f3"][0])}</span>
        <h2 class="heading-4">{e(t["f3"][1])}</h2>
        <p class="body-3 muted">{e(t["f3"][2])}</p>
      </div>
      <div class="media"><div class="phone">{img("record", t["alt_record"])}</div></div>
    </article>
    <article class="feature reveal">
      <div class="copy">
        <span class="label caption-3">{icon("clock")}{e(t["f4"][0])}</span>
        <h2 class="heading-4">{e(t["f4"][1])}</h2>
        <ul class="body-3">{li(t["f4"][2])}</ul>
      </div>
      <div class="media"><div class="phone">{img("widgets", t["alt_widgets"])}</div></div>
    </article>
  </section>

  <section class="howto" id="how" aria-labelledby="how-title">
    <div class="steps-head reveal"><h2 class="heading-2" id="how-title">{e(t["how_h"])}</h2><p class="body-3 muted">{e(t["how_p"])}</p></div>
    <div class="steps reveal">{steps}</div>
  </section>

  <section class="download" id="download" aria-labelledby="dl-title">
    <div class="lead reveal">
      <h2 class="heading-2" id="dl-title">{e(t["dl_h"])}</h2>
      <p class="body-3 muted">{e(t["dl_p"])}</p>
      <div class="free caption-1"><span>{icon("check")}{e(t["free"][0])}</span><span>{icon("lock-simple")}{e(t["free"][1])}</span><span>{icon("sparkle")}{e(t["free"][2])}</span></div>
    </div>
    <div class="platforms reveal">
      <div class="platform">{icon("apple-logo")}<span><span class="title-3">{e(t["p_apple"][0])}</span><br><span class="body-6 soon">{e(t["p_apple"][1])}</span></span><span></span></div>
      {plat("android-logo", "p_android", "/download/android")}
      {plat("windows-logo", "p_windows", "/download/windows")}
      {plat("apple-logo", "p_mac", "/download/mac")}
      {plat("globe-simple", "p_web", app)}
    </div>
  </section>

  <section class="faq" id="faq" aria-labelledby="faq-title">
    <h2 class="heading-2 reveal" id="faq-title">{e(t["faq"])}</h2>
    <dl class="reveal">{faqs}</dl>
  </section>

  <section class="dedication reveal" aria-label="{e(t["ded_label"])}">
    <div class="ded-card">
      <p class="ar bas" lang="ar">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</p>
      <div class="rule"><i></i></div>
      <p class="ar big" lang="ar">صدقة جارية</p>
      {"" if lang == "ar" else f'<p class="tr">{e(t["ded_tr"])}</p>'}
      {t["ded_names"]}
      <div class="rule"><i></i></div>
      <p class="ar dua" lang="ar">اللَّهُمَّ اجْعَلْهُ فِي مِيزَانِ حَسَنَاتِهِمْ</p>
      {"" if lang == "ar" else f'<p class="mean">{e(t["ded_dua"])}</p>'}
      <p class="ar hadith" lang="ar">إِذَا مَاتَ الإِنْسَانُ انْقَطَعَ عَمَلُهُ إِلَّا مِنْ ثَلَاثٍ: صَدَقَةٍ جَارِيَةٍ… رواه مسلم</p>
      {"" if lang == "ar" else f'<p class="hmean">{e(t["ded_hadith"])}</p>'}
    </div>
  </section>

  <section class="final reveal" aria-labelledby="final-title">
    <h2 class="heading-2" id="final-title"><span class="grad">{t["final"]}</span></h2>
    <a class="btn btn-primary button-md" href="#download">{icon("download-simple")}{e(t["download"])}</a>
  </section>

  <footer class="body-6">
    <span>{e(t["foot"])}</span>
    <nav aria-label="{e(t["support"])}"><a href="/support/">{e(t["support"])}</a><a href="/privacy/">{e(t["privacy"])}</a><a href="/delete/">{e(t["delete"])}</a></nav>
  </footer>
</main>
<script src="/landing.js"></script>
</body>
</html>
'''


def main():
    if len(sys.argv) > 1:
        images(sys.argv[1])
    for lang in T:
        out = os.path.join(WEB, T[lang]["path"].strip("/"), "index.html")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, "w", encoding="utf-8").write(page(lang))
        print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main()
