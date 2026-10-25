# 사용자별 문제은행

## 테이블

- app_users: 사용자 ID, 이메일(유일), 표시 이름, BCrypt 비밀번호 해시
- subjects: 과목 ID, 소유 사용자 ID, 과목명. 사용자 내 과목명 중복 금지
- questions: 문제 ID, 과목 ID, 유형(MC/SHORT/ESSAY/OX), 질문, 정답, 해설
- question_choices: 객관식 선지. 정답은 순서가 아니라 choice_id를 참조
- question_keywords: 서술형 필수 키워드
- question_aliases: 단답형 허용 정답
- flyway_schema_history: DB 변경 이력

문제는 과목을 통해 사용자에게 연결됩니다. 공통 필드와 검색/정렬을 통일하기 위해 문제 유형별 테이블을 따로 만들지 않았습니다.
소유자 ID는 요청에서 받지 않고 로그인 세션에서 가져옵니다. 다른 사용자의 리소스에는 404를 반환합니다.
과목 삭제 시 소속 문제와 선지·키워드·허용 정답도 삭제됩니다.

## 생성

backend/src/main/resources/db/migration/V1__question_bank.sql을 Flyway가 서버 시작 시 한 번 실행합니다.
적용된 migration 파일은 수정하지 말고 V2__설명.sql 형태로 새 파일을 추가하세요.
JPA는 ddl-auto=validate로 스키마를 검증하고 데이터를 임의 변경하지 않습니다.

## API

GET /api/auth/csrf
POST /api/auth/register {email,password,displayName}
POST /api/auth/login {email,password}
GET /api/auth/me
POST /api/auth/logout
GET/POST /api/subjects
GET/PUT/DELETE /api/subjects/{id}
GET/POST /api/questions
GET/PUT/DELETE /api/questions/{id}

문제 입력: subjectId,type,title,answer,explanation,choices:[{id,text}],keywords:[],aliases:[]
쓰기 요청은 X-XSRF-TOKEN 헤더가 필요합니다. 프론트가 csrf API로 가져와 자동으로 전송합니다.

## 로컬 실행

1. JDK 21로 backend 실행. DB 환경변수 3개 또는 무시된 backend/.env.properties 사용.
2. frontend에서 npm ci 후 npm run dev. 기본 API 대상은 localhost:8080.
3. 다른 서버 포트면 BACKEND_TARGET=http://127.0.0.1:18080 npm run dev.
4. 브라우저에서 회원가입 후 과목과 문제를 생성.

## 배포

- Render: backend 변경을 GitHub에 Push하고 재배포. 기존 DB 환경변수 유지. COOKIE_SECURE=true 추가.
- Vercel: frontend를 Root Directory로 배포. vercel.json이 /api를 Render로 프록시하므로 동일 출처 세션 쿠키를 사용.
- Render 주소 변경 시 frontend/vercel.json의 목적지 수정.
- 프론트에서 Render를 직접 다른 출처로 호출하지 않습니다. 개발 Vite proxy / 배포 Vercel rewrite를 사용합니다.

## 현재 범위

과목·문제는 Neon에 저장됩니다. 풀이/오답 기록은 계정별 브라우저 저장이며 기기 간 동기화하지 않습니다.
세션은 서버 메모리에 저장되므로 Render 재시작/절전 후 다시 로그인해야 할 수 있습니다.
회원가입 이메일 인증과 비밀번호 재설정은 아직 없습니다.
이전 브라우저 전용 버전의 문제를 옮기려면 내보낸 JSON을 로그인 후 가져오기로 등록하세요.
파일 가져오기는 여러 API 요청으로 처리되므로 중간 실패 시 일부 저장이 남을 수 있습니다. 실패 시 메시지와 서버 최신 목록을 표시합니다.

## 검증

BankSecurityTests는 로그인, 비밀번호 해시, CSRF, 사용자간 접근 차단, 네 가지 유형 생성·수정, 과목 변경·연쇄 삭제를 확인합니다.
테스트는 기본 H2에서 실행되며, Neon 환경변수를 주입한 통합 테스트도 롤백으로 실행할 수 있습니다.

## PDF 가져오기

문제은행 화면의 PDF 가져오기에서 텍스트 PDF를 선택하고, 추출된 문제를 수정·선택한 뒤 검토 확인 후 저장합니다.
PDF 원본은 PDF.js로 브라우저 안에서 처리하며 서버에는 저장할 문제 데이터만 보냅니다. 별도 AI API를 사용하지 않습니다.
지원 형식: 01. 질문 / 1) 또는 ① 선지 / 정답: / 해설: / 예시 답안과 풀이.
유형 머리말은 객관식·단답형·서술형·OX. 모범 답안 표기가 있는 서술형도 지원합니다.
문제의 코드·줄바꿈을 보존하고 원본 쪽수를 검토 화면에 표시합니다. 키워드는 사용자가 직접 입력합니다.
파일 한도 15MB/80쪽, 추출 텍스트 50만 자, 일괄 저장 200문제. 스캔/OCR·이미지·표/수식 해석·복잡한 다단 배치·별도 정답지 매칭은 미지원입니다.

POST /api/questions/import {subject,questions:[{type,title,answer,explanation,choices,keywords,aliases}]}
사용자 소유 과목에 추가하고 기존 문제는 지우지 않습니다. 같은 과목에서 유형·질문·정답·선지 내용이 같은 문제는 건너뜁니다.
입력에 오류가 있으면 해당 요청 전체를 롤백합니다. 응답은 {created,skipped}입니다.
다른 파일 구조의 해석이 잘못되면 검토 화면에서 수정하거나 선택을 해제하세요. 정답은 추정하지 않습니다.

프론트 파서 테스트: frontend에서 npm test. 서버 배치 테스트: BankSecurityTests.

## Updated import formats

The unified File Import supports TXT, PDF and DOCX only. JSON import has been removed. Batch saving is atomic; current limits and extraction details are in file-import.md.
