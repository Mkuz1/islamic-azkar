// Islamic Azkar — prayer times for the website: the same calculation, cities and methods as the apps
// (default: Dubai · Dulook DXB, IACAD — 18.2°, Dhuhr and Maghrib +3 min).
window.AzkarPrayer = (() => {
  "use strict";

  const CITIES = [
    ["مكة المكرمة", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"], ["المدينة المنورة", "Madinah", 24.4672, 39.6112, "Asia/Riyadh"],
    ["القدس", "Jerusalem", 31.7780, 35.2354, "Asia/Jerusalem"], ["بيروت", "Beirut", 33.8938, 35.5018, "Asia/Beirut"],
    ["طرابلس (لبنان)", "Tripoli (Lebanon)", 34.4367, 35.8497, "Asia/Beirut"], ["صيدا", "Saida", 33.5571, 35.3729, "Asia/Beirut"],
    ["دمشق", "Damascus", 33.5138, 36.2765, "Asia/Damascus"], ["عمّان", "Amman", 31.9539, 35.9106, "Asia/Amman"],
    ["القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"], ["الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"],
    ["جدة", "Jeddah", 21.5433, 39.1728, "Asia/Riyadh"], ["دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"],
    ["أبوظبي", "Abu Dhabi", 24.4539, 54.3773, "Asia/Dubai"], ["الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar"],
    ["الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"], ["بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad"],
    ["إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"], ["الدار البيضاء", "Casablanca", 33.5731, -7.5898, "Africa/Casablanca"],
    ["تونس", "Tunis", 36.8065, 10.1815, "Africa/Tunis"], ["الجزائر", "Algiers", 36.7538, 3.0588, "Africa/Algiers"],
    ["باريس", "Paris", 48.8566, 2.3522, "Europe/Paris"], ["ليون", "Lyon", 45.7640, 4.8357, "Europe/Paris"],
    ["مرسيليا", "Marseille", 43.2965, 5.3698, "Europe/Paris"], ["بروكسل", "Brussels", 50.8503, 4.3517, "Europe/Brussels"],
    ["لندن", "London", 51.5074, -0.1278, "Europe/London"], ["برلين", "Berlin", 52.5200, 13.4050, "Europe/Berlin"],
    ["مونتريال", "Montreal", 45.5019, -73.5674, "America/Toronto"], ["نيويورك", "New York", 40.7128, -74.0060, "America/New_York"],
    ["سيدني", "Sydney", -33.8688, 151.2093, "Australia/Sydney"], ["كوالالمبور", "Kuala Lumpur", 3.1390, 101.6869, "Asia/Kuala_Lumpur"],
    ["جاكرتا", "Jakarta", -6.2088, 106.8456, "Asia/Jakarta"],
  ].map(([ar, name, lat, lng, tz]) => ({ ar, name, lat, lng, tz }));

  // mwl, ummAlQura, egypt, isna, karachi, gulf, dubai, france, turkey — as CalcMethod in the apps.
  const FAJR = [18, 18.5, 19.5, 15, 18, 19.5, 18.2, 12, 18];
  const ISHA = [17, 0, 17.5, 15, 18, 0, 18.2, 12, 17];
  const DUBAI = 6;
  const ishaMinutes = m => (m === 1 || m === 5 ? 90 : 0);
  const offsets = m => (m === DUBAI ? [0, 0, 3, 0, 3, 0] : [0, 0, 0, 0, 0, 0]);

  const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
  const dsin = d => Math.sin(rad(d)), dcos = d => Math.cos(rad(d)), dtan = d => Math.tan(rad(d));
  const fix = (a, b) => a - b * Math.floor(a / b);
  function julian(y, m, d) {
    if (m <= 2) { y -= 1; m += 12; }
    const a = Math.floor(y / 100), b = 2 - a + Math.floor(a / 4);
    return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
  }
  function sun(jd) {
    const d = jd - 2451545.0;
    const g = fix(357.529 + 0.98560028 * d, 360), q = fix(280.459 + 0.98564736 * d, 360);
    const l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360), e = 23.439 - 0.00000036 * d;
    const ra = fix(deg(Math.atan2(dcos(e) * dsin(l), dcos(l))) / 15, 24);
    return [deg(Math.asin(dsin(e) * dsin(l))), q / 15 - fix(ra, 24)];
  }
  /** Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha in local hours. */
  function hours(y, mo, d, lat, lng, tz, method, hanafi) {
    const jd = julian(y, mo, d) - lng / (15 * 24);
    const midDay = t => fix(12 - sun(jd + t)[1], 24);
    const angleTime = (angle, t, ccw) => {
      const decl = sun(jd + t)[0];
      const x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
      const span = deg(Math.acos(x)) / 15;
      return midDay(t) + (ccw ? -span : span);
    };
    const asr = (f, t) => { const decl = sun(jd + t)[0]; return angleTime(-deg(Math.atan(1 / (f + dtan(Math.abs(lat - decl))))), t, false); };
    let fajr = angleTime(FAJR[method], 5 / 24, true);
    const sunrise = angleTime(0.833, 6 / 24, true), dhuhr = midDay(12 / 24), asrT = asr(hanafi ? 2 : 1, 13 / 24);
    const sunset = angleTime(0.833, 18 / 24, false);
    let isha = ishaMinutes(method) ? NaN : angleTime(ISHA[method], 18 / 24, false);
    const night = 24 - (sunset - sunrise);
    const fajrLimit = night * FAJR[method] / 60;
    if (isNaN(fajr) || sunrise - fajr > fajrLimit) fajr = sunrise - fajrLimit;
    if (ishaMinutes(method)) isha = sunset + ishaMinutes(method) / 60;
    else { const lim = night * ISHA[method] / 60; if (isNaN(isha) || isha - sunset > lim) isha = sunset + lim; }
    const shift = tz - lng / 15, off = offsets(method);
    return [fajr, sunrise, dhuhr, asrT, sunset, isha].map((h, i) => h + shift + off[i] / 60);
  }

  /** The date (y, m, d), minutes since midnight and UTC offset (hours) now in `tz`. */
  function local(tz, date = new Date()) {
    const p = Object.fromEntries(new Intl.DateTimeFormat("en-US", { timeZone: tz, hourCycle: "h23", year: "numeric", month: "numeric", day: "numeric", hour: "numeric", minute: "numeric" })
      .formatToParts(date).map(x => [x.type, +x.value]));
    const asUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute);
    const offset = Math.round((asUtc - Math.floor(date.getTime() / 60000) * 60000) / 60000) / 60;
    return { y: p.year, m: p.month, d: p.day, minutes: p.hour * 60 + p.minute, offset };
  }

  /** Today's six times (minutes after local midnight) for a place. */
  function times(place, method, hanafi, date = new Date()) {
    const l = local(place.tz, date);
    return hours(l.y, l.m, l.d, place.lat, place.lng, l.offset, method, hanafi).map(h => Math.round(h * 60));
  }

  /** The next prayer (sunrise excluded): its index and minutes from now. */
  function next(place, method, hanafi) {
    const now = local(place.tz).minutes, t = times(place, method, hanafi);
    for (const i of [0, 2, 3, 4, 5]) if (t[i] > now) return { index: i, inMinutes: t[i] - now, times: t };
    const tomorrow = times(place, method, hanafi, new Date(Date.now() + 86400000));
    return { index: 0, inMinutes: tomorrow[0] + 1440 - now, times: t };
  }

  return { CITIES, DUBAI, times, next };
})();
