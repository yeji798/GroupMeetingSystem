# 그룹 모임 관리 시스템 코드 발표 생존 가이드

> 목표: 코드를 한 줄씩 암기하는 것이 아니라, **요청이 어느 객체를 거쳐 어떤 CSV에 저장되는지** 설명할 수 있게 하는 문서입니다.  
> 발표 직전에는 1~6장을 먼저 읽고, 각 팀원은 7장의 담당 기능을 나눠 읽으세요.

---

## 0. 2시간 학습 순서

### 0~20분: 전원이 공통 구조 암기

다음 한 문장을 모두 말할 수 있어야 합니다.

> “이 프로젝트는 Java Swing 기반 데스크톱 프로그램이고, View가 사용자 입력을 검증한 뒤 Model 객체를 만들며, Repository가 이를 CSV로 저장합니다. 복잡한 정산 계산만 Service로 분리했고, 공통 UI와 CSV 처리는 common/util로 재사용합니다.”

반드시 볼 부분:

1. 1장 전체 구조
2. 2장 실행 흐름
3. 3장 객체 관계
4. 6장 CSV 저장 방식

### 20~70분: 기능 분담

- 1명: 회원·방 관리
- 1명: 약속·여행 일정 알고리즘
- 1명: 장소·회차
- 1명: 비용·정산

인원이 적으면 두 영역씩 맡습니다. 각자 7장의 담당 부분을 읽고 실제 파일을 한 번 엽니다.

### 70~100분: 서로 가르치기

각 담당자가 5분씩 다음 순서로 설명합니다.

1. 시작 View
2. 사용하는 Model
3. 호출하는 Repository/Service
4. 저장되는 CSV
5. 검증 조건
6. 반환값

### 100~120분: 압박 질문 연습

8장의 질문을 서로 무작위로 묻습니다. 답변은 항상 다음 형식으로 합니다.

> “그 기능은 `클래스명`의 `메서드명`에서 시작합니다. 입력을 검증한 후 `Repository 메서드`를 호출하고, 결과는 `CSV명`에 저장됩니다.”

---

## 1. 프로젝트 전체 구조

```text
GroupMeetingSystem/
├─ src/
│  ├─ com/groupmeeting/
│  │  ├─ Main.java                 프로그램 시작점
│  │  ├─ model/                    데이터 모양을 정의하는 객체
│  │  ├─ repository/               CSV 읽기·쓰기·조회·수정
│  │  ├─ service/                  정산 비즈니스 계산
│  │  ├─ util/                     공통 CSV 변환
│  │  └─ view/
│  │     ├─ auth/                  로그인·회원가입·프로필
│  │     ├─ common/                공통 UI 컴포넌트와 테마
│  │     ├─ room/                  메인·모임방
│  │     ├─ schedule/              단체 약속 일정
│  │     ├─ travel/                단체 여행 일정
│  │     ├─ place/                 장소와 투표
│  │     ├─ round/                 회차와 참여자
│  │     └─ expense/               지출과 정산
│  ├─ data/                        현재 포함된 CSV 데이터
│  └─ image/logo.png               화면 로고
├─ README.md
├─ SYSTEM_FEATURES.md              사용자 관점 기능 명세
└─ CODE_PRESENTATION_GUIDE.md      코드 관점 발표 자료
```

### 계층별 역할

| 계층 | 쉬운 설명 | 하면 안 되는 일 |
|---|---|---|
| Model | 데이터 한 건의 모양 | 파일을 직접 읽거나 화면을 띄우기 |
| View | 입력받고 결과를 보여주기 | CSV 형식을 직접 해석하기 |
| Repository | CSV를 객체로 바꾸고 CRUD 수행 | Swing 화면을 띄우기 |
| Service | 여러 데이터를 이용한 업무 계산 | 화면 컴포넌트 제어 |
| Util/Common | 여러 곳에서 쓰는 공통 기능 | 특정 기능만의 상태 저장 |

### 자주 나오는 용어

- **JFrame**: 독립적으로 열리는 큰 창. 로그인, 메인, 방 상세 화면에 사용합니다.
- **JDialog**: 부모 창 위에 뜨는 작업용 창입니다.
- **Modal**: 다이얼로그를 닫기 전까지 부모 창을 조작하지 못하게 하는 방식입니다.
- **Repository**: 데이터 저장 위치를 View에서 숨기는 객체입니다.
- **DTO/VO 역할의 Model**: 관련 값들을 하나의 객체로 묶습니다.
- **CRUD**: Create, Read, Update, Delete의 약자입니다.
- **UUID**: 지출과 회차를 이름과 무관하게 구분하는 고유 문자열입니다.

---

## 2. 프로그램 실행과 객체 흐름

### 프로그램 시작

```text
Main.main()
  → SwingUtilities.invokeLater(...)
  → LoginView 생성
  → 로그인 성공
  → MainView 생성
  → RoomDetailView
  → 각 기능별 JDialog
```

`Main.main()`은 Swing 이벤트 디스패치 스레드에서 UI를 생성합니다. Swing 컴포넌트는 한 UI 스레드에서 다루는 것이 안전하므로 `SwingUtilities.invokeLater`를 사용합니다.

### 일반적인 저장 흐름

```text
사용자 버튼 클릭
  → View의 handle...() 메서드
  → 빈 값·형식·권한 검증
  → Model 객체 생성 또는 수정
  → Repository 메서드 호출
  → CSV 전체/일부 갱신
  → 성공 메시지 및 화면 새로고침
```

예: 회원가입

```text
SignupDialog.handleSubmit()
  → 입력값 검증
  → new Member(...)
  → MemberRepository.addMember()
  → members.csv 추가 저장
  → 가입한 ID 반환
  → LoginView가 ID 입력란에 자동 표시
```

예: 정산

```text
ExpenseListDialog.handleSettle()
  → 일반 지출/회차별 지출 그룹 분리
  → SettlementCalculator.calculateCombined()
  → List<SettlementItem> 반환
  → SettlementRepository.replaceForRoom()
  → settlements.csv 저장
```

---

## 3. Model 클래스: 어떤 데이터를 담는가

Model은 대부분 생성자에서 값을 받고 Getter로 꺼냅니다. 수정 가능한 값만 Setter가 있습니다.

### `Member`

회원 한 명을 표현합니다.

| 필드 | 의미 |
|---|---|
| `name` | 실명 |
| `nickname` | 화면 표시 이름 |
| `id` | 로그인 및 관계 연결에 사용하는 고유 아이디 |
| `password` | 로그인 비밀번호 |
| `email` | 이메일 |

- 생성자: 다섯 값을 모두 받아 객체를 만듭니다.
- Getter: 각 값을 반환합니다.
- `setName/setNickname/setPassword/setEmail`: 프로필 수정에 사용합니다.
- ID Setter가 없는 이유: 가입 후 로그인 ID가 관계 데이터의 기준이므로 변경하지 않기 위해서입니다.
- `toString()`: 객체의 내용을 문자열로 확인할 때 사용합니다.

### `Room`

모임방 한 개를 표현합니다.

| 필드 | 의미 |
|---|---|
| `code` | 1000~9999의 방 코드 |
| `name` | 방 이름 |
| `category` | `단체 약속` 또는 `단체 여행` |
| `ownerId` | 방장 회원 ID |
| `memberIds` | 참여 중인 회원 ID 목록 |

- `CATEGORY_PROMISE`, `CATEGORY_TRAVEL`: 문자열 오타를 막는 상수입니다.
- `getMemberCount()`: `memberIds.size()`를 반환합니다.
- `hasMember(id)`: 참여자 목록에 ID가 있는지 반환합니다.
- `setName/setCategory`: 이름과 종류를 수정할 수 있게 준비된 Setter입니다.

### `AvailabilityEntry`

단체 약속에서 회원 한 명의 가능한 시간 구간 한 건을 표현합니다.

| 필드 | 의미 |
|---|---|
| `roomCode` | 어느 방의 일정인지 |
| `memberId` | 누가 제출했는지 |
| `date` | `yyyy-MM-dd` 날짜 |
| `startTime` | `HH:mm` 시작 시간 |
| `endTime` | `HH:mm` 종료 시간 |

- 모든 필드가 `final`이라 생성 후 바뀌지 않습니다.
- 장소 필드가 없는 이유: 장소 후보는 일정과 독립적으로 수정·투표되므로 `PlaceRepository`에서 관리합니다.
- Getter는 각 문자열을 반환합니다.

### `TravelDateEntry`

단체 여행의 가능한 기간 한 건입니다.

- `roomCode`: 방 코드
- `memberId`: 제출 회원
- `startDate`: 시작일 `yyyy-MM-dd`
- `endDate`: 종료일 `yyyy-MM-dd`
- 네 Getter가 각각 필드를 반환합니다.

### `MeetingRound`

`1차`, `2차` 같은 모임 회차입니다.

- `roomCode`: 소속 방
- `id`: UUID 고유값
- `name`: 화면에 보이는 회차명
- 세 Getter가 각 값을 반환합니다.

### `Expense`

지출 한 건입니다.

| 필드 | 의미 |
|---|---|
| `id` | 지출 UUID |
| `roomCode` | 소속 방 |
| `payerId` | 실제 결제자 |
| `amount` | 금액, 소수점 없는 `long` |
| `reason` | 지출 사유 |
| `note` | 선택 메모 |
| `roundId` | 회차 지출이면 회차 UUID, 일반 지출이면 빈 문자열 |

- Getter/Setter: 지출 조회와 수정에 사용합니다.
- `setRoundId()`: null이면 빈 문자열로 바꿔 null 처리를 단순화합니다.
- `isRoundExpense()`: `roundId`가 null/빈 문자열이 아니면 `true`를 반환합니다.

### `SettlementItem`

최종 송금 관계 한 건입니다.

- `roomCode`: 소속 방
- `fromMemberId`: 돈을 보내야 하는 회원
- `toMemberId`: 돈을 받아야 하는 회원
- `amount`: 송금 금액
- `confirmed`: 완료 여부
- `setConfirmed()`: 체크박스 변경 결과를 반영합니다.
- `involves(memberId)`: 보내는 사람 또는 받는 사람이 해당 회원이면 `true`입니다.

### `RoomFinalDecision`

방 상세 화면에 표시할 최종 결정을 모읍니다.

- `roomCode`
- `finalDate`
- `finalStartTime`
- `finalEndTime`
- `finalPlace`
- `empty(roomCode)`: 아직 결정이 없을 때 빈 값으로 채운 객체를 반환하는 정적 팩토리 메서드입니다.
- 날짜·시간·장소 Setter는 `RoomFinalRepository`가 일부 값만 갱신할 때 사용합니다.

---

## 4. Repository 공통 작동 원리

대부분의 Repository는 같은 패턴을 사용합니다.

1. 생성자에서 `ensureFileExists()` 호출
2. `data` 폴더와 CSV가 없으면 헤더가 있는 파일 생성
3. `loadAll()`에서 UTF-8로 한 줄씩 읽기
4. 첫 줄인 헤더 건너뛰기
5. `CsvUtil.parseLine()`으로 열 분리
6. Model 또는 내부 Row 객체로 변환
7. 조회 메서드에서 필요한 행만 필터링
8. 수정/삭제는 전체 목록을 메모리에 읽고 대상만 바꾼 뒤 전체 파일 다시 쓰기

### 반환형을 읽는 법

- `List<T>`: 여러 건을 반환합니다. 결과가 없으면 보통 빈 리스트입니다.
- `T`: 한 건을 반환합니다. 못 찾으면 null일 수 있습니다.
- `boolean`: 성공/실패 또는 존재 여부를 반환합니다.
- `void`: 반환값 없이 저장 상태만 변경합니다.
- `Map<K,V>`: 키로 빠르게 결과를 찾거나 그룹화한 결과입니다.

---

## 5. Repository 클래스별 메서드

### `MemberRepository`

- `ensureFileExists()`: `members.csv`를 준비합니다.
- `loadAll() → List<Member>`: 모든 회원을 객체 목록으로 반환합니다.
- `saveAll(members)`: 전체 회원 목록을 덮어씁니다.
- `addMember(member) → boolean`: 중복 ID가 아니면 회원을 추가하고 성공 여부를 반환합니다.
- `updateMember(updated) → boolean`: 같은 ID의 회원을 교체하고 발견 여부를 반환합니다.
- `isIdDuplicate(id) → boolean`: 같은 ID 존재 여부입니다.
- `authenticate(id,password) → Member`: 둘 다 일치하는 회원을 반환하고 없으면 null입니다.

### `RoomRepository`

- `loadAll()/saveAll()`: 전체 방을 읽고 씁니다.
- `addRoom(room) → boolean`: 코드가 중복되지 않을 때 추가합니다.
- `isCodeDuplicate(code) → boolean`: 코드 중복 여부입니다.
- `findByCode(code) → Room`: 해당 방 또는 null을 반환합니다.
- `getRoomsForMember(memberId) → List<Room>`: 참여 중인 방만 반환합니다.
- `joinRoom(code,memberId) → Room`: 방을 찾아 회원을 추가한 뒤 갱신된 방을 반환합니다. 실패하면 null입니다.
- `leaveRoom(code,memberId)`: 해당 방의 참여자 목록에서 ID를 제거합니다.
- `generateUniqueCode() → String`: 1000~9999 사이에서 중복되지 않는 코드를 생성합니다. 현재 생성 화면은 사용자가 직접 코드를 입력하므로 보조 API입니다.

`rooms.csv`의 여러 회원 ID는 한 열 안에서 세미콜론(`;`)으로 연결합니다.

### `AvailabilityRepository`

- `loadAll()`: 약속 일정 전체를 반환합니다.
- `saveAll(entries)`: 5열 새 형식으로 전체 저장합니다.
- `appendEntries(entries) → boolean`: 최초 제출 목록을 파일 끝에 추가합니다.
- `getForRoom(roomCode)`: 특정 방 일정만 반환합니다.
- `getForRoomAndMember(roomCode,memberId)`: 특정 사용자의 일정만 반환합니다.
- `replaceForMember(...) → boolean`: 그 방·회원의 기존 행만 제거하고 새 일정으로 교체합니다.
- `getLegacyPlaceSuggestions(roomCode)`: 예전 6열 CSV의 장소 열을 마이그레이션하기 위해 반환합니다.
- `usesLegacySchema(path)`: 현재 파일이 예전 6열인지 확인합니다.

### `TravelDateRepository`

- `loadAll()`: 여행 기간 전체 읽기
- `getForRoom()`: 방별 조회
- `getForRoomAndMember()`: 방+회원별 조회
- `appendEntries() → boolean`: 최초 기간 추가
- `replaceForMember() → boolean`: 특정 회원의 기간 전체 교체

### `PlaceRepository`

Model을 따로 만들지 않고 비공개 내부 클래스 `PlaceRow(roomCode,place)`를 사용합니다.

- `getPlaces(roomCode) → List<String>`: 방의 후보 이름 목록
- `addPlace(roomCode,place) → boolean`: 공백과 대소문자 무시 중복을 검사한 뒤 추가
- `deletePlace(roomCode,place)`: 일치 후보 삭제
- `seedIfEmpty(roomCode,initialPlaces)`: 현재 후보가 하나도 없을 때만 예전 장소들을 초기 등록

### `PlaceVoteRepository`

내부 `VoteRow(roomCode,place,memberId)`를 사용합니다.

- `getMyVote(roomCode,memberId) → String`: 본인이 선택한 장소, 없으면 null
- `castVote(...)`: 같은 방·회원의 기존 표를 제거한 뒤 새 표를 추가하여 1인 1표 유지
- `cancelVote(...)`: 본인 표 삭제
- `getVoteCounts(roomCode) → Map<String,Integer>`: 장소별 득표 수
- `getVotersByPlace(roomCode) → Map<String,List<String>>`: 장소별 투표자 ID 목록
- `deleteVotesForPlace(...)`: 장소 삭제 시 관련 표도 함께 삭제

### `RoomFinalRepository`

- `getForRoom(roomCode) → RoomFinalDecision`: 저장된 결정 또는 `empty()` 객체
- `updateOrCreate(roomCode,Consumer)`: 행이 있으면 수정하고 없으면 빈 객체를 만들어 수정하는 공통 로직
- `setFinalDate()`: 날짜만 갱신
- `setFinalTime()`: 시작·종료 시간만 갱신
- `setFinalPlace()`: 장소만 갱신

`Consumer<RoomFinalDecision>`를 사용하여 “객체의 어느 값을 바꿀지”를 메서드 인자로 전달합니다.

### `MeetingRoundRepository`

- `loadAll()`: 전체 회차
- `getForRoom(roomCode)`: 방별 회차
- `addRound(roomCode,name)`: UUID를 생성해 회차 추가

### `RoundParticipantRepository`

내부 `ParticipantRow`로 CSV 행을 표현합니다.

- `getParticipantIds(roundId)`: 회차 참여자 ID 목록
- `isParticipating(roundId,memberId)`: 참여 여부
- `addParticipant(roomCode,roundId,memberId)`: 중복이 아닐 때 참여 행 추가
- 참여 취소 API는 없습니다.

### `ExpenseRepository`

- `loadAll()`: 전체 지출
- `saveAll()`: 전체 덮어쓰기
- `getForRoom(roomCode)`: 방별 지출
- `addExpense(expense)`: 지출 추가
- `updateExpense(updated)`: 같은 UUID 지출 교체
- `deleteExpense(expenseId)`: UUID가 일치하는 지출 제거

### `SettlementRepository`

- `loadAll()`: 전체 정산 관계
- `getForRoom(roomCode)`: 방별 정산
- `replaceForRoom(roomCode,newItems)`: 다른 방 결과는 유지하고 대상 방 결과 전체 교체
- `setConfirmed(roomCode,from,to,confirmed)`: 정확한 송금 관계의 완료 여부 변경

---

## 6. CSV 저장 방법

### CSV 파일별 스키마

| 파일 | 열 순서 | 연결 Model/Repository |
|---|---|---|
| `members.csv` | `name,nickname,id,password,email` | `Member` |
| `rooms.csv` | `code,name,category,ownerId,members` | `Room` |
| `availability.csv` | `roomCode,memberId,date,startTime,endTime` | `AvailabilityEntry` |
| `travel_dates.csv` | `roomCode,memberId,startDate,endDate` | `TravelDateEntry` |
| `places.csv` | `roomCode,place` | 내부 `PlaceRow` |
| `place_votes.csv` | `roomCode,place,memberId` | 내부 `VoteRow` |
| `room_final.csv` | `roomCode,finalDate,finalStartTime,finalEndTime,finalPlace` | `RoomFinalDecision` |
| `meeting_rounds.csv` | `roomCode,id,name` | `MeetingRound` |
| `round_participants.csv` | `roomCode,roundId,memberId` | 내부 `ParticipantRow` |
| `expenses.csv` | `id,roomCode,payerId,amount,reason,note,roundId` | `Expense` |
| `settlements.csv` | `roomCode,fromMemberId,toMemberId,amount,confirmed` | `SettlementItem` |

### 예시: 회원

```csv
name,nickname,id,password,email
홍길동,길동,student01,1234,student@example.com
```

### 예시: 방

```csv
code,name,category,ownerId,members
1234,팀 회식,단체 약속,student01,student01;student02;student03
```

### 예시: 지출

```csv
id,roomCode,payerId,amount,reason,note,roundId
UUID-1,1234,student01,30000,저녁 식사,,
UUID-2,1234,student02,12000,카페,2차 모임 UUID
```

### `CsvUtil`

- `escape(value) → String`
  - null은 빈 문자열로 바꿉니다.
  - 쉼표, 큰따옴표, 줄바꿈이 있으면 전체 값을 큰따옴표로 감쌉니다.
  - 값 내부 큰따옴표는 `""`로 두 번 기록합니다.
- `toCsvLine(String... values) → String`
  - 가변 인자로 받은 여러 값을 escape하고 쉼표로 연결합니다.
- `parseLine(line) → List<String>`
  - 큰따옴표 안 쉼표는 구분자로 보지 않습니다.
  - `""`는 실제 큰따옴표 하나로 복원합니다.

### 저장 전략의 장단점

장점:

- DB 설치가 필요 없어 제출과 시연이 간단합니다.
- 파일을 직접 열어 결과를 확인할 수 있습니다.
- 작은 팀 프로젝트 데이터에는 구현이 쉽습니다.

단점:

- 동시 쓰기 제어가 없습니다.
- 데이터가 많아지면 매번 전체 파일을 읽고 쓰는 작업이 느립니다.
- 비밀번호가 평문입니다.
- 외래키와 트랜잭션을 DB처럼 강제하지 못합니다.
- 일부 저장 중 프로그램이 종료되면 파일 일관성이 깨질 수 있습니다.

### 교수님이 잡을 수 있는 경로 문제

Repository의 `DATA_DIR`은 `"data"`입니다. 즉 실행 작업 디렉터리 기준 `data/*.csv`를 사용합니다. 저장소에는 현재 `src/data`도 존재하므로 Eclipse 실행 설정에 따라 서로 다른 폴더를 볼 위험이 있습니다.

발표 답변:

> “현재는 상대 경로라 실행 위치 의존성이 있습니다. 개선한다면 실행 데이터 디렉터리를 하나의 설정 클래스로 통합하거나 사용자 홈의 애플리케이션 데이터 폴더를 사용하고, 이미지처럼 읽기 전용 초기 데이터만 classpath resource로 두겠습니다.”

### 기존 데이터 호환성

- `availability.csv`에는 과거 `place` 열이 남아 있을 수 있습니다.
- 새 `AvailabilityEntry`는 장소를 가지지 않습니다.
- Repository가 과거 6열도 읽되 장소는 일정 객체에서 제외합니다.
- `getLegacyPlaceSuggestions()`로 예전 장소를 `places.csv`에 옮길 수 있습니다.
- `expenses.csv` 역시 과거 6열 데이터는 일반 지출로 해석하고 새 데이터는 `roundId`를 포함한 7열로 저장합니다.

---

## 7. View 클래스 전체 해설

View의 공통 필드 패턴:

- `final Repository`: 데이터 접근 객체
- `final Room/Member`: 현재 화면의 대상과 로그인 사용자
- `JTextField/JPanel/JScrollPane`: 화면 상태를 유지해야 하는 Swing 컴포넌트
- `initFrame/initDialog`: 창 크기·위치·배경 설정
- `initComponents`: 실제 컴포넌트 생성과 이벤트 연결
- `handle...`: 버튼 클릭 후 실행되는 업무 로직
- `render...`: 최신 데이터를 읽어 목록 UI를 다시 그림

### 7.1 시작점과 회원 화면

#### `Main`

- 필드 없음
- `main(String[])`: Swing UI 스레드에서 `LoginView`를 생성하고 표시합니다.

#### `LoginView`

필드:

- `MemberRepository`: 인증용
- `idField`, `pwField`: 사용자 입력

함수:

- 생성자 → `initFrame()` → `initComponents()`
- `handleLogin()`: 빈 값 검사 → `authenticate()` → 실패 시 비밀번호 초기화, 성공 시 `MainView` 열기
- `handleOpenSignup()`: `SignupDialog`를 열고 성공한 ID를 입력란에 자동 설정

#### `SignupDialog`

필드:

- 회원 입력 필드 6개
- `EMAIL_PATTERN`: 이메일 정규식
- `registeredId`: 성공 시 호출자에게 돌려줄 ID

함수:

- `handleSubmit()`: 필수값, 비밀번호 길이·일치, 이메일, ID 중복 검사 후 회원 저장
- `showWarning()`: 공통 경고창
- `getRegisteredId()`: 성공 ID 또는 실패/취소 시 null

#### `ProfileEditDialog`

필드:

- `memberRepository`, `loginMember`
- 이름·닉네임·이메일·새 비밀번호 입력 필드

함수:

- `handleSave()`: 필수값·이메일 검사, 비밀번호를 입력한 경우에만 길이와 일치 검사, 객체 수정 후 CSV 갱신
- 로그인 ID는 표시만 하고 수정하지 않습니다.

### 7.2 공통 UI

#### `Theme`

상태를 저장하지 않는 UI 유틸리티입니다.

- 색상/폰트/표준 너비 상수
- `styleButton()`: 기본 녹색 버튼 스타일
- `styleSecondaryButton()`: 보조 버튼 스타일
- `styleTextField()`: 입력창 스타일
- `alignAsCenteredColumn()`: 여러 컴포넌트 폭과 정렬 통일
- `wrapHorizontalScrollable()`: 목록을 가로 스크롤 영역으로 감쌈
- `resyncScrollableHeight()`: 목록 변경 후 스크롤 높이 재계산

#### `CalendarPanel`

필드에는 현재 표시 월, 선택 날짜, 월 제목, 날짜 패널이 있습니다.

- 이전/다음 달 이동
- 요일 헤더와 날짜 버튼 생성
- `getSelectedDate() → LocalDate`: 선택일 반환
- `selectDate(date)`: 수정 화면에서 기존 날짜를 미리 선택

#### `TimePickerPanel`

오전/오후 토글과 시·분 Spinner를 묶습니다.

- `setTime24(hour,minute)`: 기존 24시간 값을 UI에 설정
- `get24HourTime() → String`: 선택값을 `HH:mm`으로 변환

#### `CodeInputPanel`

방 코드 네 자리를 네 입력칸으로 나눕니다.

- 숫자 한 자리만 허용
- 입력 시 다음 칸으로 이동
- Backspace 시 이전 칸 이동
- `getCode()`, `setCode()`, `isComplete()`, `focusFirstBox()`

### 7.3 메인과 방

#### `MainView`

필드:

- `loginMember`, `RoomRepository`
- 방 목록 패널, 검색창
- 현재 실행 중 신규 방 코드 집합

핵심 함수:

- `refreshRoomList(keyword)`: 내 방만 읽고 이름 포함 검색
- `createRoomRow(room)`: 이름·인원·종류·나가기 버튼 생성
- `handleCreateRoom()`: 생성 Dialog 후 새 방 상세로 이동
- `handleJoinRoom()`: 참여 Dialog 후 목록 갱신
- `handleLeaveRoom()`: 확인 후 참여자에서 제거
- `openRoomDetail()`: 상세 창을 열고 메인 숨김
- `refreshAndShow()`: 상세에서 돌아올 때 갱신

#### `CategorySelectDialog`

- 약속/여행 카드를 표시합니다.
- 선택 카테고리를 필드에 저장합니다.
- `getSelectedCategory()`는 선택 문자열 또는 취소 시 null을 반환합니다.

#### `CreateRoomDialog`

필드:

- `RoomRepository`, `ownerId`
- 방 이름, `CodeInputPanel`
- `createdRoom`: 성공 결과

`handleCreate()`:

1. 이름과 코드 검증
2. 숫자·1000~9999·중복 검사
3. 카테고리 선택
4. 방장을 첫 참여자로 넣어 `Room` 생성
5. 저장
6. 종류에 맞는 최초 일정 Dialog 표시

`getCreatedRoom()`은 성공한 방 또는 null입니다.

#### `JoinRoomDialog`

- 4자리 코드 완성 여부 확인
- `findByCode()`로 방 조회
- 이미 참여 중인지 확인
- `joinRoom()` 호출
- 종류별 최초 일정 화면 표시
- `getJoinedRoom()`으로 결과 반환

#### `MemberListDialog`

- 방 참여자 ID를 회원 닉네임으로 변환하여 표시
- `iAmOwner`이면 자기 자신을 제외한 회원에게 강퇴 버튼 제공
- `handleKick()`: 확인 후 `RoomRepository.leaveRoom()` 호출

#### `RoomDetailView`

모든 기능을 연결하는 허브입니다.

주요 필드:

- 부모 `MainView`, 현재 `Room`, `loginMember`
- 모든 기능의 Repository 인스턴스
- `KOREAN_CITIES`: 여행용 무작위 도시 후보

핵심 함수:

- `buildSectionsCard()`: 방 종류에 맞는 기능 버튼 구성
- `buildFinalCard()`: 최종 날짜·시간·장소 표시, 방장에게 수정 제공
- `handleComputeSchedule/TravelSchedule`: 추천 결과 화면
- `handleEditMySchedule/TravelSchedule`: 내 일정 수정
- `handleCheckPlaces/handleVotePlaces`: 후보·투표
- `handleRandomPlace/handleRandomMapPlace`: 무작위 추천
- `ensurePlaceListSeeded()`: 구형 일정 장소를 새 장소 저장소로 이전
- `handleCheckExpenses()`: 지출 화면
- `handleShowRounds()`: 회차 화면
- `handleEditFinal...()`: 방장 최종 결정 수정
- 내부 `ScrollableContentPanel`: 폭은 viewport에 맞추고 세로는 콘텐츠 크기를 유지

### 7.4 단체 약속 일정

#### `ScheduleInputDialog`

- 임시 `savedEntries` 목록에 여러 가능 시간을 모읍니다.
- `handleSave()`: 날짜와 시간 검증 후 목록 추가
- `renderEntries()`: 임시 목록 다시 표시
- `handleSubmit()`: 일정은 `AvailabilityRepository`, 선택 장소는 `PlaceRepository`에 각각 저장

#### `MyScheduleEditDialog`

- 시작 시 내 기존 일정을 로딩합니다.
- `handleAddEntry()`: 날짜·시간 검증과 같은 날짜 구간 중복 검사
- `isOverlapping()`: `새 시작 < 기존 종료 && 새 종료 > 기존 시작`이면 겹침
- `handleEditEntry()`: 선택 항목을 입력기에 다시 채움
- `handleDeleteEntry()`: 임시 목록에서 삭제
- `handleFinalSave()`: `replaceForMember()`로 내 일정 전체 교체

#### `ParticipantScheduleDialog`

- 방 참여자마다 카드 생성
- 각 회원의 가능 날짜·시간을 표시
- `findMemberById()`는 ID에 해당하는 `Member` 또는 null 반환

#### `ScheduleResultDialog`

필드:

- 일정·회원·최종결정 Repository
- 방, 로그인 회원, 결과 패널

핵심 알고리즘:

1. `groupByMember()`: 제출 데이터를 회원별 Map으로 묶음
2. 모든 등장 날짜 순회
3. `extractIntervalsForDate()`: 회원별 해당 날짜 구간 추출
4. 한 명이라도 그 날짜 입력이 없으면 후보 제외
5. `intersectIntervalLists()`: 두 구간 목록의 교집합 계산
6. 모든 제출 회원에 대해 반복 교집합
7. 겹치는 시간 길이 내림차순 정렬
8. 상위 3개 `RankedSlot` 반환

`handleFinalize()`은 방장만 후보의 날짜·시작·종료 시간을 최종 결정으로 저장합니다.

#### `FinalDateEditDialog`

- 방 종류를 `isTravel`로 구분합니다.
- 약속: 날짜 한 개 선택
- 여행: 시작·종료 날짜 선택 및 종료일 검증
- 결과 문자열을 `RoomFinalRepository.setFinalDate()`로 저장

#### `FinalTimeEditDialog`

- 시작·종료 `TimePickerPanel`
- 종료가 시작보다 늦은지 문자열 `HH:mm` 비교
- `setFinalTime()` 호출

### 7.5 단체 여행

#### `TravelDateInputDialog`

- 가능한 시작·종료 기간을 임시 목록으로 관리
- 종료일이 시작일보다 빠른지 검사
- `isOverlapping()`: 이미 추가한 기간과 중복 검사
- 제출 시 `appendEntries()`

#### `TravelScheduleEditDialog`

- 내 기존 기간 로딩
- 추가·수정·삭제
- 중복 검사
- `replaceForMember()`로 전체 교체

#### `TravelScheduleResultDialog`

`computeTopRanges()`:

1. 회원별 기간 목록으로 묶음
2. 첫 회원의 기간 목록에서 시작
3. `intersectRanges(a,b)`로 다음 회원과 날짜 교집합 반복
4. 시작일 순으로 정렬
5. 상위 5개 반환

`DateRange` 내부 클래스가 계산 중 시작일과 종료일을 묶습니다. 방장만 결과를 최종 날짜로 확정할 수 있습니다.

### 7.6 장소

#### `PlaceListDialog`

- `renderPlaces()`: 방 후보 목록 표시
- `handleAddPlace()`: 입력 후 공백·중복 검사가 포함된 `addPlace()`
- `handleDeletePlace()`: 확인 후 장소와 관련 투표 삭제

#### `PlaceVoteDialog`

- 본인 기존 표를 읽고 후보별 선택 상태 표시
- `handleVote()`: `castVote()`로 기존 표를 교체
- `handleCancelVote()`: 본인 표 삭제
- `handleShowResult()`: 결과 Dialog

#### `VoteResultDialog`

- `getVoteCounts()`와 `getVotersByPlace()` 결과 사용
- 득표 수 내림차순 표시
- 회원 ID를 닉네임으로 변환

#### `FinalPlaceEditDialog`

- 빈 장소명 검사
- `setFinalPlace()` 호출

### 7.7 회차

#### `AddRoundDialog`

- 회차명 입력
- 빈 값 검사 후 `addRound()`
- `isAdded()`로 부모 화면에 성공 여부 반환

#### `RoundListDialog`

- 회차 목록과 본인 참여 상태 표시
- 방장만 회차 추가 버튼 사용
- 미참여 회원만 참여 버튼 표시
- `handleJoinRound()`: 확인 후 참여 추가
- 참여 취소는 구현하지 않음

#### `RoundParticipantsDialog`

- 선택한 회차의 참여자 ID를 불러옴
- 회원 Repository로 닉네임을 찾아 표시

### 7.8 비용과 정산

#### `ExpenseEditDialog`

- 일반 지출 추가/수정 겸용; 기존 지출이 null이면 추가 모드
- 결제자·금액·사유·메모 입력
- 빈 값, 숫자, 0보다 큰 금액 검사
- 추가 시 UUID 생성, 수정 시 기존 UUID 유지

#### `RoundExpenseEditDialog`

- 회차 지출 추가/수정 겸용
- 회차와 결제자 선택
- 금액·사유·메모 검증
- `roundId`를 포함한 `Expense` 저장

#### `ExpenseListDialog`

- 방 지출 목록과 총액 표시
- 결제자 본인에게만 수정/삭제 버튼 표시
- 일반 지출 추가
- 회차가 있을 때 회차 지출 추가
- `handleSettle()`에서 지출 그룹을 만든 뒤 정산 Service 호출
- `handleCheckSettlement()`에서 기존 결과 조회

#### `SettlementCalculator`

상태가 없는 계산 전용 클래스라 생성자가 private입니다.

- `Group`: 함께 나눌 지출 목록과 참여자 ID 목록
- `calculateCombined(roomCode,groups)`: 모든 그룹 잔액을 합쳐 최종 송금 목록 반환
- `computeGroupBalance()`: 그룹별 `실제 지출 - 공평 부담액`
- `settleBalances()`: 채권자와 채무자를 연결해 `SettlementItem` 생성

정산 세부:

1. 총액 ÷ 인원수로 기본 부담액 계산
2. 나머지는 목록 앞 회원부터 1원씩 배분
3. 양수 잔액은 받을 돈, 음수는 보낼 돈
4. 받을 금액 큰 순, 보낼 금액 큰 순으로 정렬
5. 두 금액의 최솟값만 송금
6. 한쪽 잔액이 0이 되면 다음 사람으로 이동

#### `SettlementListDialog`

- 참여자별 정산 상태 요약
- 회원 선택 시 `MySettlementDialog` 열기

#### `MySettlementDialog`

- `viewedMemberId`: 보고 있는 회원
- `isEditable`: 로그인 사용자 본인 화면인지
- 보내야 할 항목과 받아야 할 항목을 구분
- 본인 화면에서만 완료 체크 가능
- 체크 시 `setConfirmed()` 호출 후 다시 렌더링

---

## 8. 교수님 압박 질문과 답변

### 구조

**Q. 왜 모든 코드를 View에 넣지 않았나요?**  
A. 화면, 데이터 표현, 저장, 계산 책임을 분리했습니다. UI가 바뀌어도 Model과 Repository를 재사용할 수 있고, CSV 대신 DB로 변경할 때 View 수정 범위를 줄일 수 있습니다.

**Q. Repository와 Service 차이가 뭔가요?**  
A. Repository는 저장과 조회를 담당하고, Service는 여러 데이터로 업무 규칙을 계산합니다. 정산 알고리즘은 파일 저장과 무관한 순수 계산이므로 Service입니다.

**Q. 왜 장소에는 Model 클래스가 없나요?**  
A. 현재 장소 데이터가 방 코드와 문자열 두 값뿐이라 Repository 내부 Row로 충분하다고 판단했습니다. 주소·좌표·등록자 같은 속성이 늘어나면 `Place` Model로 승격하는 것이 좋습니다.

**Q. `final` 필드는 왜 사용했나요?**  
A. 일정 한 건은 생성 뒤 내용이 바뀌지 않는 값 객체로 보고 불변성을 높였습니다. 수정은 객체 필드를 바꾸는 대신 목록에서 새 객체로 교체합니다.

### Swing

**Q. JFrame과 JDialog 차이는?**  
A. JFrame은 독립 창이고 JDialog는 특정 작업을 위한 보조 창입니다. Modal JDialog는 작업 완료 전 부모 조작을 막아 입력 흐름을 단순하게 합니다.

**Q. `invokeLater`는 왜 사용합니까?**  
A. Swing은 thread-safe하지 않으므로 컴포넌트 생성과 변경을 Event Dispatch Thread에서 수행하기 위해 사용합니다.

**Q. 이벤트는 어떻게 연결합니까?**  
A. 버튼에 `addActionListener(e -> handle...())`를 등록합니다. 클릭 이벤트가 발생하면 람다가 View의 처리 메서드를 호출합니다.

**Q. `render...()`가 왜 필요합니까?**  
A. 목록 데이터가 추가·수정·삭제된 후 기존 컴포넌트를 제거하고 최신 데이터 기준으로 다시 생성합니다. 마지막에 `revalidate()`와 `repaint()`로 레이아웃과 화면을 갱신합니다.

### 데이터

**Q. CSV의 쉼표는 어떻게 처리합니까?**  
A. `CsvUtil.escape()`가 쉼표나 큰따옴표가 있는 값을 큰따옴표로 감싸고, 내부 큰따옴표는 두 번 씁니다. 읽을 때 `parseLine()`이 큰따옴표 내부 쉼표를 무시합니다.

**Q. 수정/삭제를 어떻게 합니까?**  
A. CSV는 행 중간 수정이 어려워 전체를 객체 목록으로 읽고, ID가 일치하는 객체를 교체하거나 제외한 뒤 전체 파일을 다시 씁니다.

**Q. 데이터 관계는 어떻게 연결합니까?**  
A. DB 외래키 대신 문자열 ID를 사용합니다. `roomCode`로 방 관련 데이터를, `memberId`로 회원을, `roundId`로 회차를 연결합니다.

**Q. 왜 UUID를 사용합니까?**  
A. 지출 사유나 회차 이름은 중복·수정될 수 있으므로 변경되지 않는 고유 식별자가 필요합니다.

**Q. 비밀번호 보안은 괜찮나요?**  
A. 현재 교육용 로컬 프로젝트라 평문 CSV라는 한계가 있습니다. 실제 서비스라면 BCrypt/Argon2로 salt를 포함한 해시를 저장하고 DB 접근 제어를 적용해야 합니다.

**Q. 동시 접근하면 어떻게 됩니까?**  
A. 현재 파일 잠금과 트랜잭션이 없어 두 프로세스가 동시에 쓰면 갱신 손실 가능성이 있습니다. 실제 다중 사용자 환경에서는 DB와 트랜잭션이 필요합니다.

### 일정

**Q. 약속 시간 교집합은 어떻게 구합니까?**  
A. 시작은 두 시작 시간 중 늦은 값, 끝은 두 종료 시간 중 이른 값으로 잡습니다. `start < end`일 때만 실제 교집합입니다. 이를 회원마다 반복합니다.

**Q. 일정을 제출하지 않은 회원은요?**  
A. 현재 알고리즘은 한 번도 제출하지 않은 회원을 계산에서 제외합니다. 정보가 없는 사람을 불가능으로 단정하지 않기 위한 선택이지만, “전원 제출 후 계산” 규칙이 필요하면 방 전체 회원 수와 제출자 수를 검사해야 합니다.

**Q. 같은 회원의 일정 중복은 어떻게 막나요?**  
A. 수정 화면은 같은 날짜에 대해 `newStart < existingEnd && newEnd > existingStart` 조건으로 겹침을 검사합니다.

**Q. 왜 날짜와 시간을 String으로 저장했나요?**  
A. CSV 직렬화가 단순하고 ISO 형식은 문자열 정렬과 날짜 순서가 같습니다. 계산할 때 `LocalDate/LocalTime`으로 파싱합니다. 더 엄격하게 하려면 Model부터 날짜 타입을 쓰고 Repository에서만 문자열로 변환할 수 있습니다.

**Q. 약속 추천 순위 기준은?**  
A. 일정을 제출한 모든 회원의 공통 시간 구간을 구하고, 구간 길이가 긴 순서로 정렬해 상위 3개를 반환합니다.

**Q. 여행 교집합은?**  
A. 시작일은 두 시작일 중 늦은 날짜, 종료일은 두 종료일 중 빠른 날짜입니다. 시작이 종료보다 늦지 않으면 유효한 공통 기간입니다.

### 장소·권한

**Q. 1인 1표를 어떻게 보장합니까?**  
A. `castVote()`가 동일한 방과 회원의 기존 행을 먼저 제거하고 새 행 하나를 추가합니다.

**Q. 장소를 삭제하면 고아 투표가 남지 않나요?**  
A. View에서 장소 삭제와 함께 `deleteVotesForPlace()`를 호출합니다. 다만 DB 외래키처럼 Repository 차원에서 강제하지는 않아 개선 시 통합 Service나 DB cascade가 필요합니다.

**Q. 방장 권한은 어디서 검사합니까?**  
A. `loginMember.getId().equals(room.getOwnerId())`로 비교하고 방장 전용 버튼을 표시합니다.

**Q. 버튼을 숨기는 것만으로 보안이 충분합니까?**  
A. 로컬 UI 프로젝트에서는 동작하지만 실제 서비스에서는 부족합니다. 서버/API 계층에서도 권한을 다시 검사해야 합니다.

### 정산

**Q. 일반 지출과 회차 지출 차이는?**  
A. 일반 지출은 방 전체 인원, 회차 지출은 해당 회차 참여자만 부담합니다. `Expense.roundId`가 비었는지로 구분합니다.

**Q. 나누어떨어지지 않는 1원은?**  
A. 정수 `long`만 사용하므로 나머지를 참여자 목록 앞에서부터 1원씩 추가해 총액 보존을 보장합니다.

**Q. 정산 결과가 정확하다는 근거는?**  
A. 각 그룹에서 `실제 결제 - 부담액`을 계산하므로 전체 잔액 합은 0입니다. 채무자에서 채권자로 최소 잔액만 이동시키므로 마지막에는 모두 0이 됩니다.

**Q. 정산을 다시 계산하면 완료 체크는 유지되나요?**  
A. `replaceForRoom()`이 방의 기존 결과를 전체 교체하므로 완료 상태는 초기화됩니다. 결과 관계가 바뀔 수 있기 때문에 현재는 재계산을 새 정산으로 취급합니다.

**Q. 왜 `long`을 사용합니까?**  
A. 원 단위 금액은 소수점이 필요 없고 `int`보다 큰 범위를 안전하게 처리하기 위해서입니다. 실수형은 반올림 오류가 있어 금액에 부적합합니다.

---

## 9. 현재 코드의 한계와 개선 방향

압박 질문에서 약점을 숨기기보다 정확히 인정하고 개선안을 말하는 편이 좋습니다.

| 현재 한계 | 이유/위험 | 개선 방법 |
|---|---|---|
| CSV 평문 비밀번호 | 보안 취약 | BCrypt/Argon2 해시 |
| 상대 `data` 경로 | 실행 위치에 따라 파일이 달라짐 | 설정 클래스와 절대 앱 데이터 경로 |
| CSV 전체 재작성 | 대용량·동시성 취약 | DB와 트랜잭션 |
| View가 Repository를 직접 생성 | 결합도가 높고 테스트 어려움 | 생성자 주입, Controller/Service 계층 |
| 권한을 View에서 주로 검사 | UI 우회 시 보호 부족 | 업무 Service에서도 권한 검사 |
| 날짜/시간을 Model에서 String 저장 | 잘못된 형식 가능 | `LocalDate/LocalTime` 사용 |
| Repository 예외를 콘솔 출력 | UI가 실패 원인을 세밀하게 모름 | 예외 전달 또는 Result 타입 |
| 자동 테스트 없음 | 회귀 오류 확인 어려움 | Repository 임시파일 테스트, 알고리즘 단위 테스트 |
| 방 나가기 후 관련 데이터 유지 가능 | 고아 일정·투표·비용 | 탈퇴 정책 정의와 일괄 정리 Service |
| 방 삭제 없음 | 방장이 더 이상 필요 없는 방 제거 불가 | 확인·연관 데이터 삭제 기능 |
| 회차 참여 취소 없음 | 오입력 수정 불가 | 마감 전 취소 정책 추가 |

### 설계상 잘한 점

- Model, Repository, View, Service 책임 분리
- 장소를 일정 Model에서 분리
- UUID로 수정 가능한 이름과 고유 식별자 분리
- 공통 Theme/Calendar/TimePicker 재사용
- CSV escaping 공통화
- 방별 데이터 필터링과 회원별 교체 메서드
- 일반 지출과 회차별 지출을 하나의 정산 계산에 합산
- 레거시 CSV 호환 고려

---

## 10. 발표용 1분 설명

> “저희 시스템은 Java Swing으로 구현한 그룹 모임 관리 프로그램입니다. 사용자는 회원가입 후 방을 생성하거나 4자리 코드로 참여합니다. 방은 단체 약속과 단체 여행으로 나뉘고, 약속은 날짜와 시간 구간의 교집합, 여행은 날짜 범위의 교집합을 계산해 후보를 추천합니다. 장소는 일정과 분리해 후보 등록과 1인 1표 투표를 제공하며, 방장은 최종 날짜·시간·장소를 확정합니다. 비용은 전체 인원이 부담하는 일반 지출과 특정 회차 참여자만 부담하는 회차 지출로 구분합니다. 정산 Service가 실제 결제액과 공평 부담액의 차이를 계산해 최소 송금 관계를 만듭니다. 구조는 Model, View, Repository, Service로 분리했고 모든 데이터는 UTF-8 CSV로 저장합니다.”

---

## 11. 마지막 암기 체크리스트

팀원 전원이 다음 질문에 한 문장 이상 답할 수 있으면 됩니다.

- [ ] `Main`에서 첫 화면이 어떻게 열리는가?
- [ ] Model/View/Repository/Service의 차이는?
- [ ] 회원가입 검증 조건은?
- [ ] 방 코드 조건과 방 생성 후 흐름은?
- [ ] `roomCode`, `memberId`, `roundId`는 각각 무엇을 연결하는가?
- [ ] 약속 시간 교집합 공식은?
- [ ] 여행 날짜 교집합 공식은?
- [ ] 장소가 `AvailabilityEntry`에서 분리된 이유는?
- [ ] 투표의 1인 1표는 어떻게 보장하는가?
- [ ] 일반 지출과 회차 지출의 부담 인원 차이는?
- [ ] 정산 잔액 공식은?
- [ ] 1원이 남으면 어떻게 처리하는가?
- [ ] CSV 수정·삭제는 어떻게 구현했는가?
- [ ] CSV의 쉼표와 큰따옴표는 어떻게 처리하는가?
- [ ] 현재 코드의 가장 큰 한계와 개선 방법은?
