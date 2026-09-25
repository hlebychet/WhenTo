# WhenTo

Turn a messenger message into a calendar event in a couple of taps. WhenTo
reads the meeting line straight from what you share ("Let's discuss the mockup
on Thursday at 15:00", "Договоримся в четверг в 15:00 обсудить макет"),
extracts the date, time and duration with on-device AI, and opens the system
calendar with the event already filled in. Your text never leaves the phone.

## Highlights

- Works fully offline — no network, no cloud, no account.
- On-device Gemini Nano (AICore) parses English and Russian messages.
- Usually needs only two taps: share the text, then save in the calendar.
- Understands relative dates such as "next Friday".
- Keeps a history of created events so you can recreate them instantly.

## How it works

1. In any messenger, open a message that contains a meeting ("Let's meet
   Thursday at 15:00 to discuss the mockup", "Увидимся в следующую пятницу
   в 10 утра").
2. Share it with WhenTo — or select two messages and share them together.
3. WhenTo parses the text and opens your calendar editor with the event
   pre-filled. Fix anything you like and tap "Save".

There is no confirmation screen in the way: the calendar editor itself is
where you review and adjust before saving.

## Requirements

- Android 8.0 or newer.
- A phone with Gemini Nano for the on-device AI (currently the Pixel 10
  series). The first run may need a one-time model download.

## Install

Until WhenTo reaches an app store, download the APK from this repository's
Releases page:

1. Open the [Releases](https://github.com/hlebychet/WhenTo/releases) page and
   grab the latest `WhenTo-vX.Y.Z-release.apk`.
2. When Android asks, allow installing from unknown sources.
3. Open the APK and install.

## Getting started

- In Telegram, WhatsApp, Slack or any messenger, tap a message and choose
  **Share**, then pick **WhenTo**.
- The calendar editor opens pre-filled. Review the details and press **Save**.
- The event also lands in WhenTo's history — tap any entry to recreate it.

## Known limitations (MVP)

- Date parsing is most reliable for explicit times and "next <weekday>"
  references.
- Location is intentionally not passed to the calendar in this version.
- English and Russian message detection is supported; other languages may vary.

## Для пользователей (RU)

**Что это.** Приложение превращает сообщение о встрече в событие
календаря за пару нажатий: вы делитесь текстом («Встретимся в четверг в
15:00 обсудить макет»), WhenTo распознаёт дату, время и длительность прямо
на устройстве и открывает системный календарь с уже заполненным событием.
Интернет не нужен, текст никуда не уходит с телефона.

**Как установить.** На вкладке Releases скачайте последний
`WhenTo-vX.Y.Z-release.apk` и откройте файл. При запросе разрешите установку
из неизвестных источников.

**Как пользоваться.** В любом мессенджере выберите сообщение с встречай →
**Поделиться** (Share) → **WhenTo**. Откроется редактор календаря с готовым
событием — поправьте при необходимости и нажмите **Сохранить**. История
созданных событий хранится в приложении.

**Что нужно.** Android 8.0+, телефон с Gemini Nano (сейчас это Pixel 10).
Первый запуск может потребовать однократной загрузки модели.

**Ограничения.** Надёжнее всего распознаются явные время и фразы вида
«в следующий четверг». Локация в этой версии в календарь не передаётся.

## For developers

Build instructions, architecture and testing live in
[CONTRIBUTING.md](CONTRIBUTING.md). Design and implementation history are
under [docs/](docs/).