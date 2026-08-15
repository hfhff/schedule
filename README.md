# 오늘의 일정

개인용 Android 일정·알림 앱입니다.

## 기능

- 날짜별 여러 일정 추가·수정·삭제 및 완료 체크
- 월간 목표를 달력 상단에 표시
- 오전 전체 일정 / 오후 미완료 일정 알림
- 알림 시각 변경
- 서버·로그인·백업 없는 기기 로컬 저장

## 빌드

```powershell
.\gradlew.bat testDebugUnitTest lint assembleDebug
```

완성된 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.
