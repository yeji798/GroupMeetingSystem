# View 코드 발표 완벽 대비 가이드

## 0. 먼저 외울 핵심 문장

> View는 Swing 컴포넌트로 화면을 구성하고 사용자 이벤트를 받는다. 입력값을 검증한
> 다음 Repository 또는 Service를 호출하며, 저장 후 `render...()` 메서드로 화면을
> 다시 그린다. View는 데이터를 직접 CSV로 변환하지 않는다.

교수님이 어떤 View를 질문해도 다음 5단계로 답하면 된다.

```text
1. 화면 목적
2. 중요한 필드
3. 생성자가 전달받는 객체
4. 버튼 이벤트 handle...()
5. 호출하는 Repository와 최종 결과
```

---

## 1. 모든 View에서 반복되는 코드 패턴

### 1.1 `JFrame`과 `JDialog`

- `JFrame`: 독립된 주 화면이다.
  - `LoginView`
  - `MainView`
  - `RoomDetailView`
- `JDialog`: 부모 화면 위에서 하나의 작업을 처리하는 보조 창이다.
- `JPanel`: 다른 화면 안에 삽입해 재사용하는 UI 부품이다.

### 1.2 생성자 패턴

```java
public SomeDialog(
        Window owner,
        SomeRepository repository,
        Room room
) {
    super(owner, "창 제목", ModalityType.APPLICATION_MODAL);
    this.repository = repository;
    this.room = room;
    initDialog();
    initComponents();
}
```

- `owner`: 부모 창의 위치를 기준으로 창을 띄우고 부모·자식 관계를 유지한다.
- `APPLICATION_MODAL`: 현재 Dialog를 닫기 전에는 부모 창을 조작할 수 없다.
- Repository를 생성자로 받으면 같은 저장 기능을 재사용하기 쉽다.
- `Room`, `Member`, `memberId`는 현재 화면의 작업 대상을 알려준다.

### 1.3 초기화 메서드

- `initFrame()` / `initDialog()`
  - 창 크기, 위치, 크기 변경 가능 여부, 배경색을 설정한다.
- `initComponents()`
  - Label, Button, TextField, Panel을 생성한다.
  - Layout을 지정한다.
  - `addActionListener()`로 이벤트를 연결한다.

두 메서드를 분리한 이유는 창 자체 설정과 내부 UI 구성을 구분해 읽기 쉽게 하기 위해서다.

### 1.4 이벤트 처리

```java
button.addActionListener(e -> handleSave());
```

- 사용자가 버튼을 누르면 Swing이 `ActionEvent`를 발생시킨다.
- 람다식이 `handleSave()`를 호출한다.
- 실제 검증과 저장 코드는 handler에 모은다.

### 1.5 목록 다시 그리기

```java
private void renderItems() {
    itemsPanel.removeAll();
    // 최신 데이터로 행을 다시 생성
    itemsPanel.revalidate();
    itemsPanel.repaint();
}
```

- `removeAll()`: 이전 컴포넌트를 제거한다.
- Repository에서 최신 데이터를 읽는다.
- 행 또는 카드를 다시 만들어 추가한다.
- `revalidate()`: Layout을 다시 계산한다.
- `repaint()`: 화면을 다시 그린다.

### 1.6 `final` 필드

```java
private final Room room;
private final Member loginMember;
private final SomeRepository repository;
```

화면이 열려 있는 동안 작업 대상이 바뀌면 안 되는 값이다. 객체 내부 데이터는 바뀔 수
있지만, 필드가 다른 객체를 가리키도록 재할당할 수는 없다.

### 1.7 권한 검사

```java
boolean iAmOwner =
        loginMember.getId().equals(room.getOwnerId());
```

- 방장 여부는 로그인 ID와 방장 ID를 비교한다.
- 방장에게만 회차 추가, 강퇴, 최종 결정 버튼을 보여준다.
- 실제 웹 서비스라면 View뿐 아니라 서버에서도 권한을 다시 검사해야 한다.

---

## 2. `view/common`: 공통 UI 부품

## 2.1 `Theme`

### 목적

전체 화면의 색상, 폰트, 버튼, 입력창, 폭과 스크롤 모양을 통일한다.

### 주요 메서드

| 메서드 | 작동 |
|---|---|
| `styleButton(button)` | 기본 녹색 버튼 스타일 적용 |
| `styleSecondaryButton(button)` | 흰 배경·녹색 테두리 보조 버튼 적용 |
| `styleTextField(field)` | 입력창 폰트·테두리·안쪽 여백 적용 |
| `alignAsCenteredColumn(...)` | 전달된 요소를 같은 폭의 중앙 세로 열로 배치 |
| `centerAtStandardWidth(...)` | 요소 폭을 표준 370px로 고정하고 중앙 정렬 |
| `wrapHorizontalScrollable(panel,width)` | 긴 목록을 가로 스크롤 영역으로 감쌈 |
| `resyncScrollableHeight(scroll,panel)` | 목록 변경 후 스크롤 높이를 다시 계산 |

### 예상 질문

**Q. Theme를 왜 따로 만들었나요?**  
A. 화면마다 같은 스타일 코드를 반복하지 않고, 디자인을 한 곳에서 변경하기 위해서다.

## 2.2 `CalendarPanel`

### 필드가 저장하는 내용

- 현재 표시 중인 연·월
- 사용자가 선택한 `LocalDate`
- 월 제목 Label
- 날짜 버튼을 담는 Panel

### 주요 메서드

| 메서드 | 반환/작동 |
|---|---|
| `buildHeader()` | 이전 달·월 제목·다음 달 영역 생성 |
| `buildWeekdayHeader()` | 일~토 요일 표시 |
| `renderDays()` | 해당 월의 날짜 버튼을 다시 생성 |
| `createDayButton(date)` | 날짜 하나를 선택할 수 있는 버튼 생성 |
| `getSelectedDate()` | 선택 날짜를 `LocalDate`로 반환 |
| `selectDate(date)` | 수정 화면에서 기존 날짜를 미리 선택 |

## 2.3 `TimePickerPanel`

- 오전/오후 ToggleButton
- 시·분 Spinner
- `setTime24(hour,minute)`: 기존 24시간 값을 UI에 설정한다.
- `get24HourTime()`: 선택값을 `"HH:mm"` 문자열로 반환한다.

## 2.4 `CodeInputPanel`

- 숫자 한 자리 입력칸 4개를 가진다.
- `DocumentFilter`가 숫자 한 자리만 허용한다.
- 입력하면 다음 칸으로, Backspace면 이전 칸으로 이동한다.
- `getCode()`: 네 칸을 합친 문자열
- `setCode(code)`: 네 칸에 코드 설정
- `isComplete()`: 네 자리가 모두 입력됐는지 반환
- `focusFirstBox()`: 첫 입력칸으로 커서 이동

---

## 3. `view/auth`: 회원 화면

## 3.1 `LoginView`

### 필드

- `MemberRepository`: 회원 인증
- `idField`: 아이디 입력
- `pwField`: 비밀번호 입력

### 핵심 흐름

```text
로그인 클릭
→ 빈 값 검사
→ MemberRepository.authenticate(id,password)
→ 실패: 비밀번호 초기화
→ 성공: MainView(member) 생성, LoginView 종료
```

- `handleOpenSignup()`: 회원가입 Dialog를 열고 가입 성공 ID를 입력창에 자동 설정한다.

## 3.2 `SignupDialog`

### 필드

- 이름, 닉네임, ID, 비밀번호, 확인, 이메일 입력창
- `EMAIL_PATTERN`: 이메일 정규식
- `registeredId`: 성공한 ID를 LoginView에 돌려주기 위한 값

### `handleSubmit()`

1. 필수값 검사
2. 비밀번호 4자 이상 검사
3. 비밀번호 확인 일치 검사
4. 이메일 형식 검사
5. ID 중복 검사
6. `Member` 생성
7. `MemberRepository.addMember()`

- `getRegisteredId()`: 성공 시 ID, 취소/실패 시 `null`

## 3.3 `ProfileEditDialog`

- `loginMember`: 현재 로그인 객체
- 이름, 닉네임, 이메일, 새 비밀번호를 수정한다.
- ID는 표시만 하고 변경하지 않는다.
- 새 비밀번호가 비어 있으면 기존 값을 유지한다.
- `handleSave()`가 객체 Setter를 호출한 뒤 `updateMember()`로 CSV를 갱신한다.

---

## 4. `view/room`: 메인과 모임방

## 4.1 `MainView`

### 중요 필드

- `loginMember`: 현재 사용자
- `RoomRepository`: 방 조회·생성·참여·탈퇴
- `roomListPanel`: 내 방 목록
- `searchField`: 방 이름 검색

### 중요 메서드

| 메서드 | 설명 |
|---|---|
| `buildTopBar()` | 로고, 검색, MY 영역 |
| `toggleSearchField()` | 검색창 표시 상태 전환 |
| `refreshRoomList(keyword)` | 내 방만 읽고 이름에 keyword가 포함된 방 표시 |
| `createRoomRow(room)` | 방 이름·인원·종류·나가기 버튼 생성 |
| `handleCreateRoom()` | 생성 Dialog를 열고 성공 시 상세 화면 이동 |
| `handleJoinRoom()` | 참여 Dialog를 열고 목록 갱신 |
| `handleLeaveRoom(room)` | 확인 후 방 참여자 목록에서 본인 제거 |
| `openRoomDetail(room)` | 상세 JFrame 표시, 메인 JFrame 숨김 |
| `refreshAndShow()` | 상세 화면에서 돌아올 때 목록 갱신 |

## 4.2 `CategorySelectDialog`

- 단체 약속/단체 여행 카드를 표시한다.
- 카드 클릭 시 선택 문자열을 저장하고 창을 닫는다.
- `getSelectedCategory()`: 선택값 또는 취소 시 `null`

## 4.3 `CreateRoomDialog`

### 필드

- `RoomRepository`
- `ownerId`
- 방 이름, `CodeInputPanel`
- `createdRoom`: 생성 성공 결과

### `handleCreate()`

1. 방 이름 검사
2. 코드 4자리·숫자·1000~9999 검사
3. 코드 중복 검사
4. Category Dialog 표시
5. 방장을 첫 참여자로 설정
6. `RoomRepository.addRoom()`
7. 방 종류별 최초 일정 Dialog 표시

- `getCreatedRoom()`: 성공한 `Room` 또는 `null`

## 4.4 `JoinRoomDialog`

1. 4자리 코드 검사
2. `findByCode()`로 방 조회
3. 이미 참여 중인지 검사
4. `joinRoom()` 호출
5. 종류별 최초 일정 Dialog 표시

- `getJoinedRoom()`: 성공한 `Room` 또는 `null`

## 4.5 `MemberListDialog`

- 참여자 ID를 닉네임으로 바꿔 표시한다.
- `renderMembers()`: 목록 재생성
- `createMemberRow()`: 한 회원 행 생성
- 방장에게만 다른 회원 강퇴 버튼 표시
- `handleKick()`: 확인 후 `RoomRepository.leaveRoom()`

## 4.6 `RoomDetailView`

### 역할

모든 세부 기능으로 이동하는 허브 화면이다.

### 필드

- 부모 `MainView`
- 현재 `Room`, `loginMember`
- 방·회원·일정·장소·투표·비용·정산·여행·최종결정·회차 Repository

### 화면 구성 메서드

- `buildTopBar()`: 뒤로가기, 방 이름, 코드, 인원수, MY
- `buildSectionsCard()`: 일정·장소·비용·회차 기능 버튼
- `buildFinalCard()`: 최종 날짜·시간·장소
- `buildSection()`: 기능 섹션 공통 생성
- `button()`: 공통 버튼 생성

### 이벤트 메서드

- `handleComputeSchedule()` / `handleEditMySchedule()`
- `handleComputeTravelSchedule()` / `handleEditTravelSchedule()`
- `handleCheckPlaces()` / `handleVotePlaces()`
- `handleRandomPlace()` / `handleRandomMapPlace()`
- `handleCheckExpenses()` / `handleShowRounds()`
- `handleEditFinalDate/Time/Place()`
- `handleShowMemberList()`

### 예상 질문

**Q. 왜 Repository가 이렇게 많나요?**  
A. 상세 화면이 모든 기능의 진입점이기 때문이다. 다만 결합도가 높으므로 개선한다면
Controller/Service를 주입하여 Repository 직접 의존성을 줄일 수 있다.

---

## 5. `view/schedule`: 단체 약속

## 5.1 `ScheduleInputDialog`

### 필드

- `AvailabilityRepository`
- `PlaceRepository`
- `Room`, `memberId`
- `savedEntries`: 제출 전 임시 일정 목록
- 달력, 시작·종료 시간, 장소 입력창

### 흐름

- `handleSave()`: 선택한 날짜·시간을 임시 목록에 추가
- `renderEntries()`: 임시 일정 목록 표시
- `createEntryRow()`: 일정 한 행과 삭제 버튼 생성
- `handleSubmit()`
  - 일정이 없으면 중단
  - 장소는 `PlaceRepository`
  - 일정은 `AvailabilityRepository.appendEntries()`

## 5.2 `MyScheduleEditDialog`

- 시작할 때 `getForRoomAndMember()`로 기존 일정 로딩
- `handleAddEntry()`: 일정 추가
- `isOverlapping()`: 같은 날짜의 시간 겹침 검사
- `handleEditEntry()`: 선택 일정을 입력기로 복원
- `handleDeleteEntry()`: 임시 목록 삭제
- `handleFinalSave()`: `replaceForMember()`로 내 일정 전체 교체

시간 겹침 공식:

```text
newStart < existingEnd && newEnd > existingStart
```

## 5.3 `ParticipantScheduleDialog`

- 방 참여자별 카드 생성
- `buildMemberCard(memberId)`: 회원의 일정 목록 표시
- `findMemberById()`: 회원 ID를 Member로 변환

## 5.4 `ScheduleResultDialog`

### 가장 중요한 알고리즘

- `groupByMember()`: 일정 데이터를 회원별 Map으로 그룹화
- `extractIntervalsForDate()`: 특정 날짜의 시간 구간만 추출
- `intersectIntervalLists()`: 시간 구간 교집합
- `computeTopCommonSlots()`: 모든 제출 회원의 공통 시간 상위 3개
- `handleFinalize()`: 방장이 결과를 최종 날짜·시간으로 저장

교집합 공식:

```text
start = max(startA, startB)
end   = min(endA, endB)
start < end이면 유효
```

## 5.5 `FinalDateEditDialog`

- `isTravel`로 약속/여행 화면을 분기한다.
- 약속은 날짜 하나, 여행은 시작·종료 날짜를 선택한다.
- `handleSave()`가 날짜를 문자열로 만들어 `setFinalDate()` 호출

## 5.6 `FinalTimeEditDialog`

- 시작·종료 `TimePickerPanel`
- 종료 시간이 시작 시간보다 늦은지 검사
- `RoomFinalRepository.setFinalTime()` 호출

---

## 6. `view/travel`: 단체 여행

## 6.1 `TravelDateInputDialog`

- `savedEntries`: 제출 전 기간 목록
- `handleSave()`: 시작일·종료일 검증 후 추가
- `isOverlapping()`: 기존 기간과 중복 검사
- `renderEntries()`: 목록 갱신
- `handleSubmit()`: `appendEntries()`

## 6.2 `TravelScheduleEditDialog`

- 기존 내 기간을 불러온다.
- 추가·수정·삭제를 임시 목록에 반영한다.
- `handleFinalSave()`: `replaceForMember()` 호출

## 6.3 `TravelScheduleResultDialog`

- `computeTopRanges()`: 회원별 여행 기간의 반복 교집합
- `intersectRanges()`: 두 날짜 범위 목록의 교집합
- 시작 날짜순 정렬 후 상위 5개
- `formatRange()`: `N박 M일` 문자열 생성
- `handleFinalize()`: 방장이 최종 날짜로 저장

날짜 교집합 공식:

```text
start = later(startA, startB)
end   = earlier(endA, endB)
start <= end이면 유효
```

---

## 7. `view/place`: 장소

## 7.1 `PlaceListDialog`

- `PlaceRepository`, `PlaceVoteRepository`, `Room`
- `handleAddPlace()`: 후보 추가
- `handleDeletePlace()`: 장소와 관련 투표 함께 삭제
- `renderPlaces()`: 장소 목록 다시 표시
- `createPlaceRow()`: 장소명+삭제 버튼

## 7.2 `PlaceVoteDialog`

- 방, 로그인 회원 ID, 장소·투표·회원 Repository
- `renderPlaces()`: 현재 본인 표를 표시
- `handleVote(place)`: `castVote()`로 기존 표 교체
- `handleCancelVote()`: 본인 표 삭제
- `handleShowResult()`: 결과 Dialog 표시

## 7.3 `VoteResultDialog`

- `getVoteCounts()`: 장소별 표 수
- `getVotersByPlace()`: 장소별 투표자
- `renderResult()`: 득표 내림차순 정렬
- `createResultCard()`: 장소·득표·닉네임 목록 카드

## 7.4 `FinalPlaceEditDialog`

- 현재 장소를 입력창에 표시
- 빈 값 검사
- `RoomFinalRepository.setFinalPlace()`

## 7.5 `PlaceRecommendDialog`

- 부모 `JFrame`과 `List<String> placeList`를 받는다.
- 전달받은 장소 목록을 추천 Dialog에서 표시한다.
- 다른 장소 화면과 달리 현재 `view` 루트 패키지에 있으므로, 구조 통일을 위해 향후
  `view.place`로 이동할 수 있다.

---

## 8. `view/round`: 회차

## 8.1 `AddRoundDialog`

- `MeetingRoundRepository`, `Room`
- 회차 이름 빈 값 검사
- `handleAdd()`: `addRound()` 호출
- `isAdded()`: 부모 목록에 성공 여부 반환

## 8.2 `RoundListDialog`

- 회차·회차참여·회원 Repository
- `renderRounds()`: 회차 목록과 본인 참여 상태
- `createRoundRow()`: 회차명·참여 상태·참여 버튼
- `handleSelectRound()`: 참여자 명단
- `handleJoinRound()`: 확인 후 참여 등록
- `handleAddRound()`: 방장만 회차 생성

## 8.3 `RoundParticipantsDialog`

- `getParticipantIds(roundId)`로 참여자 조회
- `findNickname()`으로 ID를 닉네임으로 변환

---

## 9. `view/expense`: 비용과 정산

## 9.1 `ExpenseEditDialog`

- 추가와 수정 화면을 하나의 클래스로 처리한다.
- `existingExpense == null`: 추가 모드
- null이 아님: 수정 모드
- 결제자·금액·사유·메모 입력
- `parseAmount()`: 숫자 변환 실패 시 예외 대신 검증용 값 반환
- 추가 시 UUID 생성, 수정 시 기존 ID 유지
- 내부 `MemberOption`: ComboBox에 회원 객체와 표시 닉네임을 연결

## 9.2 `RoundExpenseEditDialog`

- 일반 지출 화면에 회차 선택 ComboBox가 추가된 형태
- `MemberOption`, `RoundOption` 내부 클래스 사용
- `selectPayerInCombo()`, `selectRoundInCombo()`: 수정 시 기존 값 선택
- 저장 결과의 `roundId`에 선택한 회차 UUID 포함

## 9.3 `ExpenseListDialog`

### 필드

- 지출·정산·회원·회차·회차참여 Repository
- 현재 Room, loginMember
- 지출 목록 Panel, 총액 Label

### 메서드

- `renderExpenses()`: 목록과 총액 재계산
- `createExpenseRow()`: 결제자·금액·사유·회차 표시
- 결제자 본인에게만 수정·삭제 버튼
- `handleAddExpense()`: 일반 지출
- `handleAddRoundExpense()`: 회차별 지출
- `handleEditExpense()`: `roundId` 여부에 따라 수정 Dialog 분기
- `handleDeleteExpense()`: 확인 후 삭제
- `handleSettle()`: 지출을 일반/회차 그룹으로 나눠 `SettlementCalculator` 호출
- `handleCheckSettlement()`: 기존 정산 결과 조회

## 9.4 `SettlementListDialog`

- 참여자별 관련 정산 항목 수와 완료 수 계산
- 상태: 해당 없음 / 완료 / 미완료(n/전체)
- 회원 행 클릭 시 `MySettlementDialog`

## 9.5 `MySettlementDialog`

- `viewedMemberId`: 보고 있는 회원
- `isEditable`: 로그인 사용자 본인 화면인지
- `renderItems()`: 줄 돈과 받을 돈 구분
- `createItemRow()`: 설명+완료 CheckBox
- 본인 화면에서만 CheckBox 활성화
- 변경 시 `SettlementRepository.setConfirmed()`

---

## 10. 교수님이 파고들 만한 View 질문

### Q1. 왜 View가 Repository를 직접 호출하나요?

현재 프로젝트 규모에서는 구조를 단순하게 유지하기 위해 View가 Repository를 호출한다.
더 큰 프로젝트라면 Controller/Service 계층을 사이에 두어 View와 저장소 결합도를 낮춘다.

### Q2. `render...()`와 `create...Row()`를 왜 분리했나요?

`render...()`는 전체 목록의 흐름을 담당하고 `create...Row()`는 한 행의 UI 생성만
담당한다. 반복 코드가 줄고 각 메서드의 책임이 명확해진다.

### Q3. Dialog 결과를 부모에게 어떻게 전달하나요?

- `CreateRoomDialog.getCreatedRoom()`
- `JoinRoomDialog.getJoinedRoom()`
- `CategorySelectDialog.getSelectedCategory()`
- `SignupDialog.getRegisteredId()`
- `AddRoundDialog.isAdded()`

Modal Dialog이 닫힌 뒤 부모가 Getter로 결과를 확인한다.

### Q4. 수정 화면에서 왜 임시 List를 사용하나요?

사용자가 여러 항목을 편집하는 동안 CSV를 매번 바꾸지 않고, 최종 저장을 눌렀을 때
한 번에 교체하기 위해서다. 취소하면 임시 변경을 버릴 수 있다.

### Q5. 날짜와 시간 비교가 문자열인데 안전한가요?

`yyyy-MM-dd`, `HH:mm`처럼 자릿수가 고정된 ISO 형식은 문자열 순서와 시간 순서가
같다. 복잡한 계산에서는 `LocalDate`, `LocalTime`으로 파싱한다.

### Q6. 왜 내부 클래스 `TimeInterval`, `RankedSlot`, `DateRange`를 사용하나요?

해당 화면의 계산에만 필요한 두세 값을 하나로 묶는다. 다른 패키지에서 사용할 필요가
없으므로 private static 내부 클래스로 범위를 제한한다.

### Q7. 목록에서 `revalidate()`와 `repaint()` 차이는?

- `revalidate()`: 컴포넌트 크기와 배치를 다시 계산한다.
- `repaint()`: 계산된 상태를 화면에 다시 그린다.

### Q8. Swing UI 스레드는 왜 중요하나요?

Swing 컴포넌트는 thread-safe하지 않다. 시작점에서 `SwingUtilities.invokeLater()`를
사용해 Event Dispatch Thread에서 화면을 생성한다.

### Q9. `JPasswordField.getPassword()`가 `char[]`인 이유는?

문자열보다 메모리에서 빨리 지울 수 있어 보안상 유리하다. 현재 코드는 인증 편의를
위해 String으로 변환하지만, 실제 서비스에서는 가능한 한 char 배열을 빨리 지우는
방식이 낫다.

### Q10. 권한 검사를 버튼 표시로만 해도 되나요?

로컬 단일 프로그램에서는 동작하지만 보안 경계로는 부족하다. 실제 서비스에서는
서버 또는 Service에서도 방장·본인 권한을 다시 검사해야 한다.

---

## 11. 1시간 암기 전략

### 0~10분

1장의 공통 패턴을 전원이 읽는다.

### 10~35분

팀원별 분담:

- 회원: `auth`, `common`
- 방·장소: `room`, `place`
- 일정: `schedule`, `travel`
- 비용: `round`, `expense`

### 35~50분

각 팀원이 다음 형식으로 3분 발표한다.

```text
이 클래스의 목적은 ___입니다.
중요 필드는 ___입니다.
사용자가 ___ 버튼을 누르면 handle___()이 실행됩니다.
이 메서드는 ___를 검증한 뒤 ___Repository의 ___()를 호출합니다.
결과는 ___CSV에 저장되고 render___()로 갱신됩니다.
```

### 50~60분

10장의 질문을 서로 묻는다.

---

## 12. 최종 체크리스트

- [ ] JFrame, JDialog, JPanel 차이를 설명할 수 있다.
- [ ] 생성자에서 Repository와 Room을 받는 이유를 안다.
- [ ] `initComponents()`와 `handle...()` 역할을 구분한다.
- [ ] `render...()`가 왜 필요한지 설명할 수 있다.
- [ ] Dialog 결과 Getter 5개를 알고 있다.
- [ ] 방장 권한 비교 코드를 설명할 수 있다.
- [ ] 시간·날짜 교집합 공식을 설명할 수 있다.
- [ ] 임시 `savedEntries` 목록을 사용하는 이유를 안다.
- [ ] 일반 지출과 회차 지출 화면 차이를 안다.
- [ ] `RoomDetailView`가 많은 Repository를 가지는 이유와 개선점을 말할 수 있다.
