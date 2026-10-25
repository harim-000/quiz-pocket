# QuizPocket 화면

React + Vite 기반 모바일 우선 문제은행.

frontend에서 npm ci 후 npm run dev. 개발 API는 기본 http://127.0.0.1:8080.
서버가 다른 포트면 BACKEND_TARGET=http://127.0.0.1:18080 npm run dev.
npm run build로 배포 결과물 dist 생성.

Vercel: GitHub quiz-pocket 레포, Root Directory frontend, Framework Vite,
Build Command npm run build, Output Directory dist.
vercel.json이 /api 요청을 Render 서버로 전달합니다.

회원가입/로그인, 계정별 과목 및 문제 CRUD, 객관식·단답형·서술형·OX,
과목별·랜덤 학습, 선지 섞기, 해설, 키워드/자기 채점, JSON 가져오기/내보내기.
문제는 Neon DB, 풀이 기록은 로그인한 계정별 브라우저 저장.
샘플 문제는 빈 문제은행에서 버튼을 눌러 추가할 수 있습니다.

DB 구조 및 보안/배포 설정은 ../docs/database.md 참고.

PDF 가져오기: 텍스트 PDF를 브라우저에서 분석하고 질문·선지·정답을 검토 후 계정에 추가합니다. 최대 15MB/80쪽, 한 번에 200문제. 스캔 PDF는 미지원. 파서 테스트는 npm test.

## Current file import

Use File Import for TXT, PDF and DOCX. JSON import is removed. See ../docs/file-import.md for current formats and limits.
