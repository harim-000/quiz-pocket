# QuizPocket 서버 설정

## IntelliJ
backend/pom.xml을 Maven 프로젝트로 열고 Project SDK 및 Maven Runner JRE를 21로 설정합니다.
QuizPocketApplication 실행 설정에 DB_URL, DB_USERNAME, DB_PASSWORD를 환경변수로 추가합니다.
.env.example은 참고용이며 자동으로 읽지 않습니다.

## Neon
Free 프로젝트 quiz-pocket을 생성합니다. Render와 가능한 가까운 리전을 선택합니다.
Connect에서 호스트, DB 이름, 사용자 정보를 확인합니다.
DB_URL 형식: jdbc:postgresql://호스트/neondb?sslmode=require
비밀번호는 DB_PASSWORD에 별도로 설정하고 Git에 저장하지 않습니다.

## 확인
서버 실행 후 http://localhost:8080/actuator/health 응답이 {"status":"UP"}인지 확인합니다.
DB 연결 실패 시 UP이 나오지 않습니다. 현재는 엔티티와 문제은행 테이블이 없는 서버 골격입니다.
테스트용 H2는 테스트에서만 사용하며 실제 실행에는 PostgreSQL을 사용합니다.

## Render
GitHub의 quiz-pocket 저장소를 연결하고 Docker Web Service를 생성합니다.
Root Directory는 backend, 플랜은 Free, Health Check Path는 /actuator/health입니다.
환경변수 DB_URL, DB_USERNAME, DB_PASSWORD를 입력합니다.
render.yaml을 이용해 같은 설정으로 Blueprint를 만들 수도 있습니다.
무료 서버는 유휴 상태에서 절전되므로 첫 요청이 느릴 수 있습니다.

## 이후
React 연결 시 실제 프론트엔드 주소에 맞춰 CORS를 설정하고,
문제 등록/수정 API를 공개하기 전에 작성자 인증을 추가합니다.
