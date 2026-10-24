# Логотипы сервисов

Логотипы в этой папке — товарные знаки их владельцев. Они не подпадают под лицензию MIT этого
репозитория и используются только для одного: подписать на плитке лаунчера приложение этого же
сервиса, когда оно установлено на телевизоре. Ни одна из компаний не одобряла этот проект и не
связана с ним. Если вы правообладатель и хотите, чтобы логотип убрали, откройте issue — уберём.

Все логотипы показаны белыми на нейтральной плитке. Форма не меняется: исходный файл лежит рядом, а
белую версию (`*_white.svg`) делает `design/brand_white.py`, который перекрашивает каждую фигуру в белый
и ничего не перерисовывает. Плитки (`launcher/app/src/main/res/drawable/banner_*.xml`) собирает
`design/svg2vd.py`.

## Первый набор

| Сервис | Файл | Источник |
|---|---|---|
| Rutube | `rutube/Logo_RUTUBE/Logo/SVG/Logo_RUTUBE_white_mono.svg` | [rutube.ru/brand](https://rutube.ru/brand/), архив `rutube_logo.zip` |
| ivi | `ivi/Logotype/Monochrome/White/White.svg` | [design.ivi.ru](https://design.ivi.ru/), архив `Logotype.zip` |
| RuStore | `rustore/RuStore_White_Logo.svg` | [rustore.ru/media-kit](https://www.rustore.ru/media-kit): собран из официального `RuStore_Color_Logo.svg` по брендбуку (раздел 2.4: белый вариант, значок в иконке прозрачный); формы не менялись |
| Okko | `okko/okko_logo_white.svg` | символ `okko` из спрайта сайта okko.tv (`/assets/6d22776c.svg`), сайт сам показывает его белым |
| Кинопоиск | `paper/kinopoisk.svg` | логотип из шапки kinopoisk.ru (164×36) |
| VK Видео | `paper/vk_video.svg` | официальная Figma VK |
| YouTube | `YouTube_Logo/yt_logo_white_digital.svg` | [brand.youtube/youtube-logo](https://brand.youtube/youtube-logo/), белая цифровая версия; контуры перенесены из `.ai` без изменений (`design/pdf2svg_paths.py`) |
| Google Play | `paper/google_play.svg` | вставлен владельцем макета |
| Premier | `paper/premier.svg` | вставлен владельцем макета |

## Набор stillOS

Файлы с Wikimedia Commons и Википедии помечены там как общественное достояние (простой текстовый
логотип, `PD-textlogo`) с пометкой «товарный знак»: на файл нет авторского права, товарный знак остаётся
за владельцем.

| Сервис | Файл | Источник |
|---|---|---|
| Netflix | `netflix/Netflix_2015_logo.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Netflix_2015_logo.svg) |
| Prime Video | `prime_video/Prime_Video_logo_(2024).svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Prime_Video_logo_(2024).svg) |
| HBO Max | `hbo_max/white_logo_for_pressroom.svg` | белый логотип пресс-центра WBD, [press.wbd.com](https://press.wbd.com/us/brands/hbo-max/images) (`static.cdn.turner.com/2025-07/white_logo_for_pressroom.svg`) |
| Disney+ | `disney_plus/Disney+_2024.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Disney%2B_2024.svg) |
| Apple TV | `apple_tv/Apple_TV_(logo).svg` | [Википедия (en)](https://en.wikipedia.org/wiki/File:Apple_TV_(logo).svg) |
| YouTube Music | `youtube_music/youtube_music_white.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:YouTube_Music_2024.svg), `YouTube_Music_2024.svg`; белую версию подготовил владелец проекта (значок с вырезанными кольцом и треугольником) |
| Spotify | `spotify/Spotify_logo_with_text.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Spotify_logo_with_text.svg) |
| Twitch | `twitch/Twitch_Unextruded_Wordmark_Logo_White.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Twitch_Unextruded_Wordmark_Logo_White.svg) |
| Crunchyroll | `crunchyroll/Crunchyroll_2024.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Crunchyroll_2024.svg) |
| Plex | `plex/Plex_logo_2022_mono-on-black.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Plex_logo_2022_mono-on-black.svg), официальный монохромный вариант Plex |
| Kodi | `kodi/kodi-logo-with-text.svg` | сайт Kodi, [github.com/xbmc/kodi-tv](https://github.com/xbmc/kodi-tv/tree/main/static/images) |
| Jellyfin | `jellyfin/jellyfin-horizontal--flat-on-dark.svg` | [github.com/jellyfin/jellyfin-ux](https://github.com/jellyfin/jellyfin-ux), © Jellyfin Project, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); файл не изменён, белая версия — производная под той же лицензией |
| Wink | `wink/wink.svg` | сайт Wink (`block.wink.ru/images/wink.svg`) |
| КИОН | `kion/KionChromeBig_v3.svg` | сайт КИОН (`static.kion.ru/kds/mainlogo/main/KionChromeBig_v3.svg`) |
| START | `start/logo_white_V2.svg` | сайт START (`start.ru/static/images/header/logo_white_V2.svg`), уже белый |
| Амедиатека | `amediateka/logo.svg` | сайт Амедиатеки (`cybermonday.amediateka.ru/static/img/logo.svg`) |
| Первый канал | `first_channel/Logo_Pervogo_kanala.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:%D0%9B%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF_%C2%AB%D0%9F%D0%B5%D1%80%D0%B2%D0%BE%D0%B3%D0%BE_%D0%BA%D0%B0%D0%BD%D0%B0%D0%BB%D0%B0%C2%BB.svg), `Логотип «Первого канала».svg` |
| Смотрим | `smotrim/Smotrim_logo.svg` | [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D0%BC_%D0%BB%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF.svg), `Смотрим логотип.svg` |
| Zona | `zona/zona_logo_text.svg` | логотип из шапки сайта zona.plus |

Netflix — товарный знак Netflix, Inc. Prime Video, Amazon и связанные знаки — товарные знаки
Amazon.com, Inc. или её аффилированных лиц. HBO Max — товарный знак Warner Bros. Discovery. Disney+ —
товарный знак Disney Enterprises, Inc. Apple и Apple TV — товарные знаки Apple Inc., зарегистрированные
в США и других странах. YouTube, YouTube Music и Google Play — товарные знаки Google LLC. Spotify —
товарный знак Spotify AB. Twitch — товарный знак Twitch Interactive, Inc. Crunchyroll — товарный знак
Crunchyroll, LLC. Plex, the Plex Play logo and Plex Media Server are trademarks of Plex. Kodi is a
registered trademark of the XBMC Foundation. Jellyfin — товарный знак Jellyfin Project. Остальные
названия и логотипы принадлежат их владельцам.

Иконки интерфейса — бесплатный набор [Untitled UI](https://www.untitledui.com/free-icons) (линейные,
коммерческое использование без указания авторства): `design/icons/untitled/`, ресурсы делает
`design/untitled_icons.py`.
