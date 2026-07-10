# TV Browser — Android TV Web Tarayıcısı

Android TV için sıfırdan geliştirilmiş, modern ve kumandayla (D-pad) rahat kullanılabilen bir web tarayıcısı.

## Özellikler

- **Sanal imleç**: Kumandanın yön tuşları ekrandaki imleci hareket ettirir (basılı tutunca hızlanır), OK/orta tuş tıklama yapar, ekran kenarına dayanınca sayfa kayar.
- **Modern koyu TV arayüzü**: Odak vurgulu butonlar, yuvarlatılmış adres çubuğu, degrade arka planlı ana sayfa.
- **Hızlı erişim ana sayfası**: Büyük arama kutusu + popüler siteler ve yer imlerinden oluşan kart ızgarası.
- **Çoklu sekme**: Sekme değiştirici diyalog ile sekme açma/kapama; `target=_blank` bağlantıları yeni sekmede açılır.
- **Yer imleri ve geçmiş**: Kalıcı olarak saklanır; yer imleri ana sayfadaki hızlı erişim ızgarasına eklenir.
- **Tam ekran video** desteği (`onShowCustomView`).
- **Ayarlar**: Arama motoru (Google / Yandex / DuckDuckGo / Bing), tarayıcı kimliği (TV / Masaüstü / Mobil), geçmiş-çerez-önbellek temizleme.
- **Oturum geri yükleme**: Açık sekmeler uygulama yeniden açıldığında geri gelir.
- Türkçe ve İngilizce arayüz.

## Kumanda kullanımı

| Tuş | İşlev |
|---|---|
| Yön tuşları | İmleci hareket ettirir (sayfada) / odak gezdirir (ana sayfa ve araç çubuğunda) |
| OK (orta tuş) | İmlecin olduğu yere tıklar |
| Geri | Sayfa geçmişinde geri → çıkış onayı |
| Menü tuşu veya sayfanın en üstünde yukarı itme | Araç çubuğunu açar |
| Araç çubuğunda aşağı | Araç çubuğunu kapatır, imlece döner |

## Derleme

```bash
./gradlew assembleDebug
```

APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

Her push'ta GitHub Actions debug APK'yı derleyip **tv-browser-debug-apk** artifact'ı olarak yükler.

## Kurulum (sideload)

1. Actions sekmesinden son başarılı build'in `tv-browser-debug-apk` artifact'ını indirin.
2. TV'de *Bilinmeyen kaynaklardan kuruluma* izin verin.
3. APK'yı USB bellek veya `adb install app-debug.apk` ile kurun.

## Teknik detaylar

- Kotlin + klasik View'lar, sistem WebView motoru
- minSdk 21 (Android 5.0+ TV'ler), targetSdk 35
- Depolama: SharedPreferences + JSON (ek bağımlılık yok)
