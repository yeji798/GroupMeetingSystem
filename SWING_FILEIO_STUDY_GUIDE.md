# Swing & 파일 입출력(File I/O) 학습 자료

> 이 문서는 **레포트가 아니라, 레포트를 쓰기 위한 학습 자료**입니다.
> "우리 프로젝트의 어디에 어떤 개념이 쓰였는지"를 실제 코드 위치(파일:줄번호)와 함께 정리했습니다.
> 각 항목을 직접 파일을 열어 읽어보고, **왜 그렇게 짰는지 스스로 설명할 수 있게 된 다음** 레포트 문장으로 옮기세요.

---

## 0. 전체 그림 먼저 잡기

이 프로젝트는 DB 없이 동작하는 Swing 데스크톱 앱입니다. 구조를 한 줄로 요약하면:

```
View (Swing 화면)  →  Repository (파일 입출력)  →  data/*.csv (실제 저장소)
        ↑                        ↓
     Model (Member, Expense, Room ...) ← CsvUtil (CSV 텍스트 ↔ 값 변환)
```

- **Swing** 은 사람이 보는 화면(입력창, 버튼, 목록)을 만드는 부분
- **파일 입출력** 은 그 화면에서 입력한 데이터를 CSV 파일에 저장하고, 다시 읽어오는 부분
- 두 부분은 **Repository 클래스**(`src/com/groupmeeting/repository/`)에서 맞닿습니다: View는 Repository만 호출하고, Repository가 내부적으로 파일을 읽고 씁니다.

레포트는 이 두 축(Swing / 파일 입출력)을 각각 "① 이론 → ② 우리 코드에서 어떻게 썼는지"로 구성하면 자연스럽습니다.

---

## 1. Swing 기초 개념

### 1.1 Swing이 뭔가
- `javax.swing` 패키지. Java의 GUI(그래픽 사용자 인터페이스) 라이브러리 중 하나.
- 더 오래된 `java.awt`(AWT) 위에 만들어진 **경량(lightweight) 컴포넌트** 집합. (AWT는 OS의 실제 버튼/창을 그대로 빌려쓰고, Swing은 Java가 직접 화면에 그려서 OS 상관없이 모양이 똑같음)
- 우리 프로젝트도 `import javax.swing.*;` 와 `import java.awt.*;` 를 항상 같이 쓰는데, **레이아웃/색상/폰트 같은 기본 도형·좌표 개념은 AWT가 제공**하고 그 위에 Swing 컴포넌트를 얹는 구조라서입니다.

### 1.2 컴포넌트 계층 구조 (핵심!)
```
Component (java.awt)
 └─ Container (java.awt)
     └─ JComponent (javax.swing)   ← 대부분의 Swing 위젯이 여기서 출발
         ├─ JPanel     (컨테이너: 다른 컴포넌트를 담는 그릇)
         ├─ JLabel     (텍스트/이미지 표시)
         ├─ JButton    (버튼)
         ├─ JTextField / JPasswordField (입력창)
         └─ ...
     최상위 창(Top-level container)은 JComponent가 아님:
 └─ Window
     ├─ JFrame  (독립된 하나의 창 — 프로그램의 메인 창)
     └─ JDialog (다른 창에 종속된 보조 창 — 팝업/모달창)
```
**레포트에 쓰기 좋은 문장 재료**: "JFrame은 최상위 창이고, JDialog는 부모 창(owner)이 있는 종속 창이다. 우리 프로젝트에서 `LoginView`, `MainView`, `RoomDetailView`만 `JFrame`을 상속하고([LoginView.java:42](src/com/groupmeeting/view/auth/LoginView.java)), 나머지 28개의 화면은 모두 `JDialog`를 상속해 부모 창 위에 팝업 형태로 뜬다."

**직접 확인해볼 것**: 아래 명령으로 우리 프로젝트에 JFrame/JDialog가 몇 개씩 있는지 직접 세어보세요.
```
grep -rl "extends JFrame" src/com/groupmeeting/view
grep -rl "extends JDialog" src/com/groupmeeting/view
```

### 1.3 레이아웃 매니저 (LayoutManager)
컴포넌트를 어디에 배치할지 정하는 규칙. 우리 프로젝트에서 실제 사용 빈도(코드 전체 검색 결과):

| 레이아웃 | 사용 횟수 | 특징 |
|---|---|---|
| `BoxLayout` | 62 | 컴포넌트를 세로(Y_AXIS) 또는 가로(X_AXIS)로 한 줄씩 순서대로 쌓음 |
| `BorderLayout` | 20 | 화면을 NORTH/SOUTH/EAST/WEST/CENTER 5구역으로 나눔 |
| `FlowLayout` | 8 | 왼쪽부터 순서대로 흘러가듯 배치, 넘치면 다음 줄로 |
| `GridLayout` | 6 | 행×열의 격자로 동일 크기 배치 |

- **BoxLayout**이 압도적으로 많이 쓰인 이유: 이 앱의 모든 화면이 "라벨 → 입력창 → 라벨 → 입력창 → 버튼"처럼 **세로로 쌓이는 폼(form) 형태**이기 때문. `LoginView`의 `initComponents()`가 대표적 예([LoginView.java:71-160](src/com/groupmeeting/view/auth/LoginView.java)).
- **GridLayout**은 요일/날짜처럼 "칸이 딱딱 맞아야 하는" 곳에 씀. `CalendarPanel`에서 요일 7칸, 날짜 그리드를 `new GridLayout(0, 7, 4, 4)`로 만듦([CalendarPanel.java:40](src/com/groupmeeting/view/common/CalendarPanel.java)) — 행 개수를 0으로 주면 "열은 7개로 고정, 행은 내용에 맞춰 자동 계산"이라는 뜻.
- **BorderLayout**은 "가운데 하나 + 좌우 보조 요소" 구조에 씀. `CalendarPanel`의 헤더(‹ 이전달 | 2026년 7월 | 다음달 ›)가 예([CalendarPanel.java:48-72](src/com/groupmeeting/view/common/CalendarPanel.java)).

**레포트에 쓰기 좋은 비교 포인트**: "왜 JTable 대신 JPanel+BoxLayout으로 목록을 직접 만들었을까?" → 우리 프로젝트에는 `JTable`이 단 한 번도 쓰이지 않았습니다(직접 grep해서 확인해보세요: `grep -rn "JTable" src`). 대신 지출/장소/일정 목록 화면들은 모두 반복문으로 `JPanel`을 하나씩 만들어 쌓는 방식입니다. 이유를 추론해서 레포트에 써보세요 (힌트: 각 항목마다 "수정"/"삭제" 버튼이 따로 붙어야 하고, 디자인을 자유롭게 커스터마이징하기 위해서일 가능성이 큽니다).

### 1.4 이벤트 처리 (리스너 패턴)
Swing은 "무언가 발생하면(이벤트) → 미리 등록해둔 코드가 실행된다(리스너)"는 **관찰자 패턴(Observer Pattern)**을 사용합니다.

우리 프로젝트에서 리스너 등록 방식은 전부 **람다식**입니다 (총 68개 파일에서 사용):
```java
// LoginView.java:119
loginButton.addActionListener(e -> handleLogin());
```
이건 사실 아래 코드를 줄인 것입니다 (익명 클래스 방식 — 옛날 Java 스타일):
```java
loginButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        handleLogin();
    }
});
```
`ActionListener`는 메서드가 딱 하나뿐인 인터페이스(**함수형 인터페이스**)이기 때문에 Java 8부터 람다식으로 줄여 쓸 수 있습니다. **레포트에 이 두 코드를 나란히 놓고 "람다식이 왜 가능한지"를 설명하면 좋은 심화 내용이 됩니다.**

### 1.5 EDT (Event Dispatch Thread) — 우리 프로젝트가 이미 지키고 있는 규칙
`Main.java`를 보면:
```java
// Main.java:29
SwingUtilities.invokeLater(() -> {
    LoginView loginView = new LoginView();
    loginView.setVisible(true);
});
```
Swing은 **스레드에 안전하지 않습니다(thread-unsafe)**. 화면을 그리고 수정하는 모든 코드는 반드시 **EDT라는 전용 스레드 하나**에서만 실행되어야 합니다. `main()` 메서드 자체는 EDT가 아니므로, `SwingUtilities.invokeLater(...)`로 "이 코드를 EDT에게 넘겨서 실행해달라"고 부탁하는 것입니다.

같은 이유로 `JoinRoomDialog.java:88`에서도 포커스를 옮기는 코드를 `SwingUtilities.invokeLater(...)`로 감싸둔 걸 확인할 수 있습니다. **이건 교수님이 좋아할 만한 심화 포인트**이니 꼭 레포트에 넣어보세요 (많은 대학 프로젝트가 이걸 놓치는데, 이 프로젝트는 지키고 있습니다).

### 1.6 우리 프로젝트만의 재사용 패턴 — `Theme.java`
Swing 자체 기능은 아니지만, 실무적으로 중요한 패턴입니다. `Theme.java`([전체 보기](src/com/groupmeeting/view/common/Theme.java))는:
- 색상/폰트 상수 (`PRIMARY_GREEN`, `FONT_TITLE` 등)
- `styleButton()`, `styleTextField()` 같은 **정적 헬퍼 메서드**로 "버튼 하나를 초록 테마로 꾸미는 코드"를 한 곳에 모아둠

→ 모든 화면이 이 메서드를 호출하기만 하면 디자인이 통일됩니다. 이건 **중복 코드 제거(DRY 원칙)**를 Swing 화면 꾸미기에 적용한 사례로 레포트에 쓸 수 있습니다.

또한 `CalendarPanel`은 `JPanel`을 상속해서 만든 **커스텀 복합 컴포넌트**입니다([CalendarPanel.java:16](src/com/groupmeeting/view/common/CalendarPanel.java)). 달이 바뀔 때 `daysPanel.removeAll()` 후 다시 `add()`하고 `revalidate()`/`repaint()`를 호출해 화면을 다시 그리는 부분([CalendarPanel.java:100-118](src/com/groupmeeting/view/common/CalendarPanel.java))은 "Swing 컴포넌트를 동적으로 갱신하는 방법"을 보여주는 좋은 예제입니다.

**공부 과제**: `Theme.java`의 `wrapHorizontalScrollable()`와 `resyncScrollableHeight()`([Theme.java:125-198](src/com/groupmeeting/view/common/Theme.java))를 읽고, "목록에 항목을 추가했는데 화면에 바로 안 보이는 문제"를 `revalidate()`/`repaint()`/`validate()`로 어떻게 해결했는지 자기 말로 설명해보세요. (`revalidate()`는 나중에 다시 배치하라는 예약이고, `validate()`는 지금 즉시 다시 배치하라는 명령이라는 차이가 핵심입니다.)

### 1.7 화면에 나타난 컴포넌트 사용 빈도 (전체 프로젝트 기준)
| 컴포넌트 | 개수 | 용도 |
|---|---|---|
| JLabel | 128 | 텍스트/이미지 표시 (제목, 안내문구 등) |
| JPanel | 91 | 컴포넌트를 담는 컨테이너 |
| JButton | 67 | 버튼 |
| JScrollPane | 22 | 목록이 길어질 때 스크롤 제공 |
| JTextField | 15 | 한 줄 텍스트 입력 |
| JPasswordField | 3 | 비밀번호 입력(입력값이 별표로 가려짐) |
| JToggleButton | 2 | 눌린 상태를 유지하는 버튼(투표 선택 등에 사용 추정) |
| JSpinner | 2 | 숫자/날짜 등을 증감 화살표로 조절 |
| JCheckBox | 1 | 체크박스 |
| JPopupMenu / JMenuItem | 1 / 2 | 우클릭 메뉴 등 |

이 표는 여러분이 실제로 `JToggleButton`, `JSpinner`가 어느 화면에서 쓰였는지 찾아서(`grep -rn "JToggleButton" src/com/groupmeeting/view`) 레포트에 "이 컴포넌트는 OOO 화면에서 이런 용도로 썼다"라고 구체적으로 적으면 훨씬 좋은 레포트가 됩니다.

---

## 2. 파일 입출력(File I/O) 기초 개념

### 2.1 Java의 두 가지 I/O API
- **`java.io`** (전통적 방식): `File`, `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter` 등. 스트림(stream) 기반.
- **`java.nio.file`** (Java 7부터, NIO.2): `Path`, `Paths`, `Files`. 더 간결하고 예외 처리가 명확함.

우리 프로젝트는 **두 방식을 섞어서** 씁니다. 예를 들어 `MemberRepository.java`([전체 보기](src/com/groupmeeting/repository/MemberRepository.java)) 상단을 보면:
```java
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
```
- 파일 경로 표현·존재 확인·디렉터리 생성 → `java.nio.file`의 `Path`, `Paths`, `Files`
- 실제로 한 줄씩 읽고 쓰는 스트림 객체 → `java.io`의 `BufferedReader`, `BufferedWriter`

즉 **"경로/파일 관리는 최신 API(NIO.2), 줄 단위 읽기/쓰기는 전통 API(BufferedReader/Writer)"** 조합입니다. 왜 이렇게 섞어 썼는지, 각각의 장점이 무엇인지 비교해서 쓰면 좋은 레포트 내용이 됩니다.

### 2.2 핵심 클래스 정리

| 클래스 | 역할 | 프로젝트에서 쓰인 곳 |
|---|---|---|
| `Path` / `Paths.get(...)` | 파일/디렉터리의 "경로"를 표현하는 객체 (실제 존재 여부와 무관) | [MemberRepository.java:40](src/com/groupmeeting/repository/MemberRepository.java) |
| `Files.exists(path)` | 경로에 실제 파일/폴더가 있는지 확인 | [MemberRepository.java:41](src/com/groupmeeting/repository/MemberRepository.java) |
| `Files.createDirectories(path)` | 폴더 생성(중간 폴더까지 한 번에) | [MemberRepository.java:42](src/com/groupmeeting/repository/MemberRepository.java) |
| `Files.writeString(path, text, ...)` | 문자열을 파일에 한 번에 씀 | [MemberRepository.java:47](src/com/groupmeeting/repository/MemberRepository.java) |
| `Files.newBufferedReader(path, charset)` | 파일을 읽기용 `BufferedReader`로 열기 | [MemberRepository.java:63](src/com/groupmeeting/repository/MemberRepository.java) |
| `Files.newBufferedWriter(path, charset)` | 파일을 쓰기용 `BufferedWriter`로 열기(기존 내용 덮어씀) | [MemberRepository.java:103](src/com/groupmeeting/repository/MemberRepository.java) |
| `StandardOpenOption.CREATE` / `APPEND` | 파일이 없으면 만들고, 있으면 **끝에 이어붙임**(덮어쓰지 않음) | [MemberRepository.java:137](src/com/groupmeeting/repository/MemberRepository.java) |
| `StandardCharsets.UTF_8` | 문자 인코딩을 UTF-8로 명시 (한글이 깨지지 않게) | 모든 Repository 공통 |

### 2.3 왜 인코딩(UTF_8)을 항상 명시할까?
한글, 특수문자를 다루는데 OS/환경마다 기본 인코딩이 다를 수 있습니다(Windows는 기본이 MS949/CP949인 경우가 많음). `StandardCharsets.UTF_8`을 **모든 파일 읽기/쓰기에서 명시적으로 고정**해두면, 어떤 컴퓨터에서 실행해도 한글이 깨지지 않습니다. → **레포트에서 "인코딩을 명시하지 않았다면 어떤 문제가 생겼을지"를 가정해서 설명하면 이해도를 보여줄 수 있습니다.**

### 2.4 try-with-resources
```java
// MemberRepository.java:63
try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
    ...
} catch (IOException e) {
    System.err.println(...);
}
```
`BufferedReader`/`BufferedWriter`는 `AutoCloseable`을 구현하기 때문에, `try (...)` 괄호 안에서 생성하면 **블록이 끝날 때(정상 종료든 예외든) 자동으로 `close()`가 호출**됩니다. 옛날 방식이라면 `finally` 블록에서 직접 `reader.close()`를 호출해야 했습니다. 이 부분은 파일 입출력 레포트에서 반드시 다뤄야 할 핵심 문법입니다.

### 2.5 CSV(Comma-Separated Values)란?
- 값을 콤마(,)로 구분해 한 줄에 나열하는 단순 텍스트 파일 형식.
- 문제: 값 안에 콤마나 줄바꿈이 들어있으면 어떻게 구분하나? → **RFC 4180** 표준 규칙: 그런 값은 큰따옴표(`"..."`)로 감싸고, 값 안의 큰따옴표는 두 번(`""`) 반복해서 표시.

---

## 3. 프로젝트의 파일 입출력 구조 — 3단계로 이해하기

### 3.1 1단계: `CsvUtil.java` — 텍스트 ↔ 값 변환기
전체 코드가 짧으니 [CsvUtil.java](src/com/groupmeeting/util/CsvUtil.java) 전체를 꼭 한 번 읽어보세요. 핵심 메서드 3개:

**① `escape(value)`** — 값 하나를 안전하게 CSV에 쓸 수 있게 변환
```java
public static String escape(String value) {
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
    return value;
}
```
→ "홍길동, 팀장" 같이 콤마가 들어간 값을 그대로 쓰면 나중에 읽을 때 컬럼이 어긋납니다. 그래서 콤마/큰따옴표/줄바꿈이 있을 때만 큰따옴표로 감쌉니다.

**② `toCsvLine(values...)`** — 여러 컬럼 값을 한 줄로 합침 (가변 인자 `String...` 사용)

**③ `parseLine(line)`** — CSV 한 줄을 다시 값들의 리스트로 분해. **상태 기계(state machine)** 방식으로 구현되어 있음: `inQuotes`라는 boolean 변수로 "지금 큰따옴표 안에 있는지"를 추적하면서 한 글자씩(`charAt`) 검사합니다.

**공부 과제 (중요!)**: `parseLine("a,\"b,c\",d")`를 손으로 한 글자씩 따라가며 `inQuotes`와 `tokens`가 어떻게 변하는지 표로 그려보세요. 이 트레이싱을 레포트에 그림/표로 넣으면 "코드를 진짜로 이해했다"는 걸 확실하게 보여줄 수 있습니다.

### 3.2 2단계: Repository 클래스 — 파일 입출력을 담당하는 계층
프로젝트에는 11개의 Repository 클래스가 있고(`MemberRepository`, `ExpenseRepository`, `RoomRepository` 등), **모두 똑같은 패턴**을 따릅니다. `MemberRepository.java`를 기준으로 그 패턴을 정리하면:

| 메서드 | 역할 | 파일에 대한 동작 |
|---|---|---|
| `ensureFileExists()` (생성자에서 호출) | 폴더/파일이 없으면 헤더만 있는 파일을 새로 만듦 | 쓰기(최초 1회) |
| `loadAll()` | 파일 전체를 읽어 `List<Member>`로 변환 | 읽기 |
| `saveAll(list)` | 리스트 전체를 파일에 **덮어쓰기** | 쓰기(전체 덮어씀) |
| `addMember(member)` | 한 줄만 파일 **끝에 추가**(append) | 쓰기(끝에 이어붙임) |
| `updateMember(updated)` | 전체를 불러온 뒤(`loadAll`) id가 같은 항목을 교체하고 다시 전체 저장(`saveAll`) | 읽기 + 쓰기 |
| `deleteXxx(id)` (다른 Repository, 예: `ExpenseRepository`) | 전체를 불러온 뒤 해당 항목만 제외하고 다시 저장 | 읽기 + 쓰기 |

**왜 `addMember`만 append이고 나머지는 전체를 다시 쓸까?**
- 새로 추가할 때는 기존 내용을 건드릴 필요가 없으니, 파일 끝에 한 줄만 붙이면 됨 → 효율적 (`StandardOpenOption.APPEND`).
- 수정/삭제는 "몇 번째 줄인지" 파일 안에서 찾아 바꾸는 것이 훨씬 복잡하므로, **전체를 메모리(List)로 읽어들여서 리스트를 고친 다음, 파일 전체를 다시 씀**. 데이터 양이 적은 소규모 프로젝트에서는 이 방식이 훨씬 단순하고 버그가 적습니다.
→ 이 트레이드오프(효율 vs 단순함)를 레포트에 서술하면 좋은 분석이 됩니다.

**실제 코드로 비교해보기** — `ExpenseRepository`의 삭제 메서드:
```java
// ExpenseRepository.java (deleteExpense)
public void deleteExpense(String expenseId) {
    List<Expense> all = loadAll();
    List<Expense> kept = new ArrayList<>();
    for (Expense expense : all) {
        if (!expense.getId().equals(expenseId)) {
            kept.add(expense);
        }
    }
    saveAll(kept);
}
```
→ "지울 대상만 빼고 나머지를 새 리스트에 담아 그대로 다시 저장"하는, 파일 시스템에서 흔히 쓰는 **삭제 = 필터링 후 재저장** 패턴입니다.

### 3.3 3단계: 스키마 변경에 대한 대응 (레포트 심화 포인트로 강력 추천)
`ExpenseRepository.loadAll()`을 보면:
```java
// ExpenseRepository.java
String roundId = tokens.size() >= 7 ? tokens.get(6) : "";
```
주석에는 "`roundId` 칸은 나중에 추가된 컬럼이라, 예전 방식으로 저장된 줄(6개 칸)에는 없을 수도 있다"고 적혀 있습니다. 즉 **개발 중간에 CSV의 컬럼 구조(스키마)가 바뀌었는데, 기존에 저장되어 있던 옛 데이터 파일과도 호환되도록 방어적으로 코드를 짠 것**입니다. 이건 실무의 "데이터 마이그레이션/하위 호환성" 문제를 소규모로 겪고 해결한 사례라서, 레포트에서 눈에 띄는 심화 내용이 될 수 있습니다.

---

## 4. Swing ↔ 파일 입출력이 실제로 연결되는 흐름 (End-to-End 예시)

로그인 과정을 예로 전체 흐름을 따라가 보세요:

1. `LoginView`의 "로그인" 버튼 클릭 → `addActionListener`가 등록해둔 `handleLogin()` 실행 ([LoginView.java:169](src/com/groupmeeting/view/auth/LoginView.java))
2. `idField.getText()`, `pwField.getPassword()`로 **Swing 컴포넌트에서 값을 꺼냄**
3. `memberRepository.authenticate(id, password)` 호출 → Repository 내부에서 `loadAll()`이 **CSV 파일을 읽고**, `CsvUtil.parseLine()`으로 각 줄을 분해해서 비교
4. 결과에 따라 `JOptionPane.showMessageDialog(...)`로 **성공/실패를 다시 화면에 표시**
5. 성공 시 `new MainView(member)`로 **새 Swing 창을 열고** 로그인 창은 `dispose()`로 닫음

이 흐름 하나를 그림(순서도)으로 그려서 레포트에 넣으면, "Swing 파트"와 "파일 입출력 파트"가 서로 어떻게 맞물리는지 한눈에 보여줄 수 있어 좋은 자료가 됩니다.

---

## 5. 레포트 작성 가이드

### 5.1 추천 목차
1. 서론 — 프로젝트 개요, DB 없이 CSV 파일로 데이터를 관리하는 구조 소개
2. Swing 이론 정리 (1장 내용을 요약 + 본인 언어로 재정리)
3. 우리 프로젝트의 Swing 활용 사례 (표, 코드 인용, 스크린샷 추천)
4. 파일 입출력 이론 정리 (2장 내용 요약)
5. 우리 프로젝트의 파일 입출력 구조 (`CsvUtil` → `Repository` 2단 구조를 그림으로)
6. 심화 분석 — EDT, 스키마 호환성 처리, append vs 전체 재저장 트레이드오프 중 택1~2개를 깊게
7. 결론 — 배운 점, 아쉬운 점(개선하면 좋을 부분)

### 5.2 "아쉬운 점/개선 아이디어"에 넣을 수 있는 소재 (직접 검증해보고 쓰세요)
- CSV + 전체 재저장 방식은 데이터가 아주 많아지면 느려질 수 있음 → 실제 DB(SQLite 등)로 바꾸면 어떻게 달라질지
- `System.err.println`으로만 오류를 출력하는 부분들 → 로깅 프레임워크(SLF4J 등)를 쓰면 어떤 점이 좋아지는지
- 비밀번호를 평문으로 CSV에 저장하는 부분(`MemberRepository`) → 보안 관점에서 해시(hash) 처리가 필요하다는 점

이런 "왜 이렇게 안 했을까/이렇게 하면 더 좋지 않을까"는 교수님이 좋아하는 비판적 사고를 보여주는 좋은 문단이 됩니다.

### 5.3 직접 실습해보면 좋은 것
- `data/members.csv` 파일을 메모장으로 직접 열어서, 회원가입할 때마다 줄이 추가되는 걸 눈으로 확인
- 이름에 콤마가 들어간 값(예: `"김철수, 부회장"`)으로 회원가입해보고, CSV 파일에서 큰따옴표로 어떻게 감싸지는지 확인 (`CsvUtil.escape` 동작 확인)
- `CalendarPanel`에서 "다음달 ›" 버튼을 눌렀을 때 `renderDays()`가 다시 호출되는 과정을 디버거(브레이크포인트)로 따라가보기

---

## 6. 참고 (공식 문서)
- Oracle 공식 Swing 튜토리얼: https://docs.oracle.com/javase/tutorial/uiswing/
- Oracle 공식 I/O 튜토리얼: https://docs.oracle.com/javase/tutorial/essential/io/

(위 링크는 자바 공식 튜토리얼 사이트로, 각 장의 목차에서 이 문서의 개념들을 더 깊게 찾아볼 수 있습니다.)
