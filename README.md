# AI-SafeDrony
AI-SafeDrony

# 실행 방법
Gradle Sync

프로젝트 열기 후 Sync Project with Gradle Files 실행

필수 의존성 자동 설치

▶️ 앱 실행 방법
Android Studio에서 MainActivity 선택 후 실행

앱 구동 후, 메인화면에서 기능 테스트 가능:

실시간 카메라 화면 표시

AI 모델을 활용한 헬멧 착용 여부 감지

헬멧 미착용 시 자동 캡처 및 경고 음성 출력

캡처된 이미지 목록 확인 및 교육 콘텐츠 생성 기능 실행

생성된 교육 슬라이드를 화면에 표시

📦 주요 구현 기능
실시간 카메라 연동: CameraX를 활용하여 작업자 촬영

AI 모델 연동: TFLite 기반 헬멧 감지 모델 사용

TTS(Text-to-Speech): 위험 감지 시 경고 메시지 음성 출력

이미지 자동 저장 및 조회: 위험 상황 자동 캡처 → 목록에서 확인

맞춤형 교육 콘텐츠 표시: 선택한 이미지 기반 슬라이드 생성 후 표시
