$desktopPath = [Environment]::GetFolderPath("Desktop")
$baseDir = "$desktopPath\ReleaseNotes_v1.0.66"
New-Item -ItemType Directory -Force -Path $baseDir | Out-Null

# Define Content
$notes = @{
    "id.txt" = @"
Apa yang Baru di Versi 1.0.66:

✨ Pengalaman Lebih Mulus: Menambahkan indikator loading interaktif pada semua alat AI (Sketch to Image, Draw to Image, Upscale, dll) untuk mencegah kesalahan sentuh saat memproses.
⌨️ Perbaikan UI: Masalah tampilan saat mengetik prompt kini sudah diperbaiki agar lebih stabil.
🚀 Performa: Aplikasi memuat lebih cepat dengan peningkatan sistem caching workflow.
🛡️ Keamanan: Peningkatkan filter keamanan dan stabilitas aplikasi secara umum.
"@

    "ar.txt" = @"
ما الجديد في الإصدار 1.0.66:

✨ تجربة أكثر سلاسة: تمت إضافة مؤشرات تحميل تفاعلية لجميع أدوات الذكاء الاصطناعي لمنع اللمس غير المقصود أثناء المعالجة.
⌨️ تحسين واجهة المستخدم: تم إصلاح مشاكل العرض عند كتابة الأوامر لتكون أكثر استقرارًا.
🚀 الأداء: يتم تحميل التطبيق بشكل أسرع مع تحسين نظام التخزين المؤقت.
🛡️ الأمان: تحسين فلاتر الأمان واستقرار التطبيق بشكل عام.
"@

    "ja-JP.txt" = @"
バージョン 1.0.66 の新機能：

✨ よりスムーズな体験：すべてのAIツール（スケッチ、描画、アップスケールなど）にインタラクティブな読み込み画面を追加しました。処理中の誤操作を防ぎます。
⌨️ UIの改善：プロンプト入力時のレイアウト表示の問題を修正し、より安定させました。
🚀 パフォーマンス：キャッシュシステムの改善により、アプリの起動と読み込みが高速化しました。
🛡️ セキュリティ：安全性フィルターの強化と、全体的なバグ修正を行いました。
"@

    "ko-KR.txt" = @"
버전 1.0.66의 새로운 기능:

✨ 더 부드러운 경험: 처리 중 오작동을 방지하기 위해 모든 AI 도구(스케치, 그리기, 업스케일 등)에 로딩 화면을 추가했습니다.
⌨️ UI 개선: 프롬프트 입력 시 레이아웃이 불안정하던 문제를 수정했습니다.
🚀 성능: 워크플로우 캐싱 시스템 개선으로 앱 로딩 속도가 빨라졌습니다.
🛡️ 보안: 안전 필터가 강화되었으며 전반적인 안정성이 향상되었습니다.
"@

    "ru-RU.txt" = @"
Что нового в версии 1.0.66:

✨ Более плавная работа: Добавлены интерактивные экраны загрузки во все ИИ-инструменты (эскиз, рисование, улучшение качества и др.) для предотвращения случайных нажатий.
⌨️ Улучшение UI: Исправлены проблемы с макетом при вводе подсказок (промптов).
🚀 Производительность: Ускоренная загрузка приложения благодаря улучшенной системе кэширования.
🛡️ Безопасность: Улучшены фильтры безопасности и повышена общая стабильность приложения.
"@
}

# Write Files
foreach ($key in $notes.Keys) {
    $filePath = Join-Path $baseDir $key
    Set-Content -Path $filePath -Value $notes[$key] -Encoding UTF8
    Write-Host "Created: $key"
}

Write-Host "`nDone! Release notes saved to: $baseDir"
