# WineScanner — MVP умного сканера винных этикеток

KMP + Compose Multiplatform, Android и iOS из одной кодовой базы.
Camera → on-device ONNX-детекция границ этикетки → кроп → Ktor → карточка
вина из базы «Своё Вино».

## Стек

| Слой              | Технология                                                        |
|-------------------|--------------------------------------------------------------------|
| UI                | Compose Multiplatform 1.11.1 (Kotlin 2.4.0)                       |
| Камера            | CameraX 1.5.1 (Android) / AVFoundation (iOS)                      |
| Edge AI           | ONNX Runtime Mobile 1.27.0 (Android, `ai.onnxruntime`) — iOS через `onnxruntime-objc`, см. ниже |
| Сеть              | Ktor Client 3.4.0 (OkHttp / Darwin) + kotlinx.serialization        |

Версии зафиксированы в `gradle/libs.versions.toml` и актуальны на июль 2026 —
при первом синке Gradle стоит проверить, не вышли ли более свежие патчи.

## Структура проекта

```
composeApp/src/
├── commonMain/kotlin/com/winescanner/app/
│   ├── App.kt                    — сборка зависимостей + переключение экранов
│   ├── model/Models.kt           — WineResult (контракт JSON) и ScanUiState
│   ├── camera/CameraController.kt   — expect: контроллер камеры + превью
│   ├── ml/LabelExtractor.kt         — expect: Edge AI (детекция + кроп)
│   ├── network/NetworkModule.kt     — HttpClient, ApiConfig.BASE_URL
│   ├── network/WineApiService.kt    — POST кропа, парсинг ответа
│   ├── domain/ScanRepository.kt     — склеивает ML и сеть
│   ├── presentation/ScanViewModel.kt — StateFlow<ScanUiState>
│   ├── presentation/CameraScreen.kt  — экран 1
│   ├── presentation/LoadingScreen.kt — экран 2
│   ├── presentation/ResultScreen.kt  — экран 3
│   └── ui/theme/Theme.kt            — палитра «винный погреб»
├── androidMain/.../{camera,ml,network}/*.android.kt  — CameraX, ONNX Runtime, OkHttp
├── androidMain/assets/               — сюда кладётся label_detector.onnx
└── iosMain/.../{camera,ml,network}/*.ios.kt          — AVFoundation, Darwin
```

## Как запустить

**Android** — открыть корень проекта в Android Studio (или Fleet с плагином
Kotlin Multiplatform), дождаться синка Gradle, запустить конфигурацию
`composeApp` на устройстве/эмуляторе с камерой.

**iOS** — этот архив содержит только модуль `composeApp` (Kotlin-часть).
Тонкую Xcode-обёртку `iosApp` он не включает, потому что `.xcodeproj`
надёжнее сгенерировать штатным способом:
1. Создайте новый KMP-проект через [kmp.jetbrains.com](https://kmp.jetbrains.com)
   или мастер New Project → Kotlin Multiplatform в Android Studio.
2. Замените сгенерированный `composeApp/` содержимым из этого архива.
3. В точке входа `iosApp` (`ContentView.swift` / `App.swift`) вызовите
   `Main_iosKt.MainViewController()` — эта функция уже реализована в
   `MainViewController.kt`.

## Что обязательно донастроить перед демо

1. **ONNX-модель.** Положите файл детектора этикетки:
   - Android: `composeApp/src/androidMain/assets/label_detector.onnx`
   - iOS: добавьте `.onnx` в Xcode-таргет `iosApp` как bundled resource
     (Copy Bundle Resources), путь получайте через `NSBundle.mainBundle`.
2. **Адрес бэкенда.** `NetworkModule.kt` → `ApiConfig.BASE_URL` — сейчас
   там заглушка.
3. **iOS Info.plist.** Обязательно добавьте ключ, иначе приложение упадёт
   при первой попытке доступа к камере:
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>Приложению нужен доступ к камере для сканирования винных этикеток</string>
   ```
4. **iOS + ONNX Runtime.** Кроп и вся верстка на iOS уже работают «из
   коробки» (используется center-crop фолбэк вместо ML-детекции границ).
   Чтобы включить настоящую ONNX-детекцию на iOS — подключите под
   `onnxruntime-objc` через CocoaPods. Точные, сверенные с официальной
   документацией ONNX Runtime сигнатуры вызовов и пошаговая инструкция —
   прямо в комментарии над `detectLabelBounds()` в
   `LabelExtractor.ios.kt`. Это единственное место во всём проекте, которое
   не удалось прогнать через реальный Xcode-тулчейн в среде, где готовился
   этот MVP (здесь нет macOS/Xcode для компиляции Kotlin/Native с
   CocoaPods-зависимостью) — поэтому вместо непроверенного кода там честный
   TODO с точным планом действий.

## Ключевые архитектурные решения

- **Исходное фото никогда не покидает устройство.** `ScanRepository`
  прогоняет полноразмерный снимок через `LabelExtractor.extractLabelCrop()`
  *до* любого сетевого вызова; в `WineApiService` попадает только уже
  обрезанный JPEG.
- **Кроп уходит как `multipart/form-data`**, а не Base64-в-JSON — это
  компактнее (Base64 добавляет ~33% к размеру). Если бэкенд хакатона
  ожидает именно Base64-JSON, поменять нужно только `WineApiService.kt`.
- **ONNX-контракт — предположение**, которое стоит проверить первым.
  Ожидается один выходной тензор `[1, 5]` = `[x1, y1, x2, y2, confidence]`
  в нормализованных координатах 0..1 (см. `detectLabelBounds()` в
  `LabelExtractor.android.kt`). Если экспортированная модель устроена
  иначе (YOLO с анкорами, сегментационная маска и т.п.) — меняется только
  этот один блок.
- **Устойчивость вместо падений.** И на Android, и на iOS ML-детекция
  обёрнута в `runCatching` с фолбэком на center-crop (центральные 70%
  кадра). Так «Сделать фото» никогда не подвешивает приложение и не роняет
  демо из-за модели/сети — это прямое следствие требования «работать без
  подвисаний».
- **Ошибка сети/ML не создаёт четвёртый экран.** По ТЗ их ровно три; сбой
  во время Loading просто возвращает на экран камеры со Snackbar-сообщением
  (`ScanUiState.Camera(errorMessage = ...)`), а не отдельным UI-состоянием.
- **DI — ручной, без Koin/Hilt.** Для MVP это осознанное упрощение:
  `LabelExtractor` создаётся один раз в платформенной точке входа
  (`MainActivity` / `MainViewController`, единственное место, где нужен
  Android `Context`) и прокидывается в общий код параметром. Остальные
  зависимости (HTTP-клиент, репозиторий, ViewModel) собираются в `App.kt`.
- **Без androidx.lifecycle.ViewModel.** Вместо него — простой держатель
  состояния на `StateFlow` + `CoroutineScope` из `rememberCoroutineScope()`.
  Multiplatform-ViewModel (`androidx.lifecycle:lifecycle-viewmodel` 2.9.x)
  на момент подготовки MVP уже стабилен и это разумный следующий шаг, если
  понадобится переживать смену конфигурации/процесса.

## Дизайн

Тёмная палитра «винный погреб» (почти чёрный фон, глубокий бордовый,
приглушённое золото) вместо дефолтной Material-темы. Золотое кольцо —
сквозной элемент: и на кнопке-затворе на экране камеры, и на медальонах
рейтингов на экране результата (отсылка к медалям и фольге на этикетках).

## Что не входит в этот MVP (осознанно)

- Автоматических/юнит-тестов — `ScanRepository`/`ScanViewModel` не зависят
  от платформы и легко тестируются через `kotlinx-coroutines-test` с
  фейковыми `LabelExtractor`/`WineApiService`, но для хакатон-MVP это
  вынесено за скобки.
- Обработки батареи/памяти при долгой работе камеры, HDR/зума/вспышки.
- Кэширования результатов и истории сканирований.
