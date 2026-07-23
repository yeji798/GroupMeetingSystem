# 그룹 모임 관리 시스템

Java Swing으로 만든 그룹 일정·장소 투표·회비 정산 관리 프로그램입니다.

## 실행 환경

- Java 21
- Eclipse IDE 또는 터미널

## 프로젝트 구조

```text
GroupMeetingSystem/
├─ src/
│  ├─ com/groupmeeting/
│  │  ├─ Main.java          # 애플리케이션 진입점
│  │  ├─ model/             # 도메인 데이터 객체
│  │  ├─ repository/        # CSV 데이터 읽기·쓰기
│  │  ├─ service/           # 정산 등 비즈니스 로직
│  │  ├─ util/              # 범용 CSV 보조 기능
│  │  └─ view/              # Swing 화면과 다이얼로그
│  │     ├─ common/         # 테마와 공통 입력 컴포넌트
│  │     ├─ auth/           # 로그인, 회원가입, 프로필
│  │     ├─ room/           # 메인 화면과 모임방 관리
│  │     ├─ schedule/       # 일반 일정 조율
│  │     ├─ travel/         # 여행 기간 조율
│  │     ├─ place/          # 장소 후보와 투표
│  │     ├─ expense/        # 비용 입력과 정산
│  │     └─ round/          # 모임 회차와 참여자
│  ├─ data/                 # 애플리케이션 CSV 데이터
│  └─ image/                # 화면 이미지 리소스
└─ bin/                     # 컴파일 결과(자동 생성, Git 제외)
```

`src`에는 소스 코드와 실행 리소스만 둡니다. 컴파일된 `.class` 파일은 `bin`에
생성되며 Git으로 관리하지 않습니다.

## Eclipse에서 실행

1. `File > Open Projects from File System...`을 선택합니다.
2. 이 프로젝트 폴더를 가져옵니다.
3. `src/com/groupmeeting/Main.java`를 우클릭합니다.
4. `Run As > Java Application`을 선택합니다.

## 터미널에서 실행

PowerShell 기준:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
$sources = Get-ChildItem src/com/groupmeeting -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d bin $sources
Copy-Item src/image bin/image -Recurse -Force
java -cp bin com.groupmeeting.Main
```

프로그램 데이터는 `src/data` 아래 CSV 파일에 저장됩니다.

`AvailabilityEntry`는 날짜와 시간 구간만 표현합니다. 추천 장소는 일정 데이터에
중복 저장하지 않고 `PlaceRepository`와 `places.csv`에서 독립적으로 관리합니다.

## 주요 기능

- 회원가입, 로그인, 프로필 관리
- 모임방 생성 및 참여
- 일정 조율과 최종 일정 선택
- 장소 후보 등록, 투표 및 결과 확인
- 회차별 비용 입력과 정산

전체 기능과 세부 동작은 [SYSTEM_FEATURES.md](SYSTEM_FEATURES.md)에서 확인할 수 있습니다.

코드 구조, 클래스·메서드 설명과 발표 예상 질문은
[CODE_PRESENTATION_GUIDE.md](CODE_PRESENTATION_GUIDE.md)에서 확인할 수 있습니다.
