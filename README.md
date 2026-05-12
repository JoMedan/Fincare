# 💰 Fincare — 개인 가계부 관리 앱 백엔드

> Spring Boot 기반 RESTful API 서버  
> JWT 인증 · Redis 토큰 관리 · 지출 분석 · Docker Compose 배포

---

## 📌 프로젝트 개요

월 수입과 고정 지출을 등록하면 하루 예산을 자동 계산해 주는 가계부 앱의 백엔드입니다.  
당일 예산이 부족할 경우 SafeBox(비상금)에서 자동 충당하고, 월별·카테고리별 지출 분석 API를 제공합니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.1 |
| Security | Spring Security, JWT (JJWT 0.11.5) |
| Database | MySQL 8.0, Spring Data JPA |
| Cache / Session | Redis 7 (Refresh Token, 블랙리스트) |
| Build | Gradle |
| Infra | Docker, Docker Compose |
| Docs | Swagger (Springdoc OpenAPI) |
| Test | JUnit 5, Mockito, MockMvc, H2 |

---

## ✨ 주요 구현 사항

### 🔐 JWT 이중 토큰 인증
- **Access Token** (1시간) + **Refresh Token** (7일) 분리 발급
- Access Token 만료 시 `/auth/refresh`로 재발급 → 로그인 유지
- 로그아웃 시 Access Token을 **Redis TTL 기반 블랙리스트**에 등록 → 서버 재시작 후에도 유효
- Refresh Token도 Redis에 저장 → 로그아웃 즉시 무효화

### 💸 예산 자동 관리
- 월 순수입(`총수입 - 고정지출`) ÷ 당월 일수 = 하루 예산 자동 계산
- 지출 발생 시 하루 예산 차감, 예산 초과 시 SafeBox에서 자동 충당
- 날짜 변경 감지 후 전일 잔여 예산 SafeBox 이월

### 📊 지출 분석 API
- **월별 트렌드**: 최근 N개월 수입·지출·순액 추이 (차트 데이터)
- **전월 대비**: 이번 달 vs 지난달 증감액·증감률(%)
- **카테고리 분석**: 특정 월 카테고리별 금액·비율(%)

### 🏗 설계 품질
- **Custom Exception 계층**: `FinanceException` 기반 도메인 예외 5종, HTTP 상태 코드 자동 매핑
- **TransactionType Enum**: `@JsonValue`/`@JsonCreator`로 한글 JSON 호환, JPA Converter로 기존 DB 무중단 마이그레이션
- **GlobalExceptionHandler**: 전역 예외 처리로 일관된 에러 응답 형식

---

## 📁 프로젝트 구조

```
backend/src/main/java/Fincare/FincareAppProject/
├── Config/         # JwtUtil, JwtFilter, SecurityConfig
├── Controller/     # AuthController, TransactionController, UserController, AnalysisController, ChatbotController
├── DTO/            # 요청/응답 데이터 객체
├── Entity/         # User, Transaction
├── Enums/          # TransactionType, TransactionTypeConverter
├── Exception/      # FinanceException 계층 + GlobalExceptionHandler
├── Repository/     # JPA Repository
└── Service/        # UserService, TransactionService, AnalysisService, TokenService, OpenAiService
```

---

## 🚀 실행 방법

### 방법 1. Docker Compose (권장)

**사전 준비**: [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치

```bash
cd backend

# 1. 환경변수 파일 생성
cp .env.example .env
# .env 파일에서 OPENAI_API_KEY 등 실제 값으로 수정

# 2. 전체 실행 (MySQL + Redis + App)
docker-compose up --build

# 3. 종료
docker-compose down
```

### 방법 2. 로컬 직접 실행

**사전 준비**: MySQL 8.0, Redis 7 로컬 설치 및 실행

```bash
cd backend
# application.properties 기본값 기준
# MySQL: localhost:3306/fincaredb  /  Redis: localhost:6379

./gradlew bootRun
```

---

## 📡 API 명세

서버 실행 후 Swagger UI에서 전체 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

### 주요 엔드포인트

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/auth/register` | 회원가입 | ✗ |
| POST | `/auth/login` | 로그인 (Access + Refresh Token 반환) | ✗ |
| POST | `/auth/refresh` | Access Token 재발급 | ✗ |
| POST | `/auth/logout` | 로그아웃 | ✓ |
| GET | `/transactions` | 거래 내역 조회 (날짜/기간 필터) | ✓ |
| POST | `/transactions` | 거래 내역 등록 | ✓ |
| PATCH | `/transactions/{id}` | 거래 내역 수정 | ✓ |
| DELETE | `/transactions/{id}` | 거래 내역 삭제 | ✓ |
| GET | `/analysis/monthly-trend` | 월별 수입·지출 트렌드 | ✓ |
| GET | `/analysis/compare` | 전월 대비 증감률 | ✓ |
| GET | `/analysis/category` | 카테고리별 지출 분석 | ✓ |
| POST | `/chatbot/ask` | AI 챗봇 (OpenAI 연동) | ✓ |

---

## 🧪 테스트

```bash
cd backend
./gradlew test
```

| 테스트 클래스 | 개수 | 설명 |
|--------------|------|------|
| `UserServiceTest` | 11 | 회원가입, 로그인, 비밀번호 변경, 회원 탈퇴 |
| `TransactionServiceTest` | 12 | 거래 생성/삭제, SafeBox 충당 로직 |
| `AnalysisServiceTest` | 7 | 트렌드, 증감률, 카테고리 분석 |
| `AuthControllerTest` | 11 | MockMvc 기반 API 통합 테스트 |
| **합계** | **41** | |

---

## 🔧 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/fincaredb` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `csedbadmin` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `localhost` |
| `JWT_SECRET` | JWT 서명 키 | (기본값 — **운영 시 반드시 변경**) |
| `JWT_EXPIRATION_MS` | Access Token 유효 시간 (ms) | `3600000` (1시간) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh Token 유효 시간 (ms) | `604800000` (7일) |
| `APP_CORS_ALLOWED_ORIGINS` | CORS 허용 출처 | `http://localhost:3000` |
| `OPENAI_API_KEY` | OpenAI API 키 | — |
