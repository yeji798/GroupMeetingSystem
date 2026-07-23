# 단체 모임 관리 시스템 (Java Swing)

## 실행 환경
- Java 21
- Eclipse IDE

## Eclipse로 열기
1. Eclipse 실행 -> `File > Open Projects from File System...`
2. 압축 해제한 `GroupMeetingSystem` 폴더 선택 후 Import
3. `src/com/groupmeeting/Main.java` 파일을 우클릭 -> `Run As > Java Application`

## 폴더 구조
```
GroupMeetingSystem/
 ├─ src/com/groupmeeting/
 │   ├─ Main.java                # 프로그램 진입점
 │   ├─ model/Member.java        # 회원 정보 모델
 │   ├─ util/CsvUtil.java        # CSV 인코딩/디코딩 유틸
 │   ├─ util/MemberRepository.java # 회원 CSV 파일 저장소 (로드/저장/인증)
 │   └─ view/
 │       ├─ Theme.java           # 초록색 테마 색상/스타일
 │       ├─ LoginView.java       # 메인(로그인) 화면
 │       ├─ SignupDialog.java    # 회원가입 다이얼로그
 │       └─ MainView.java        # 로그인 완료 후 메인 화면
 └─ data/members.csv             # 회원 정보 저장 파일 (최초 실행 시 자동 생성됨)
```

## 현재 구현된 기능
- **회원가입**: 이름/닉네임/아이디/비밀번호/이메일 입력, 아이디 중복 확인, 이메일 형식 검증, 비밀번호 확인 일치 검사
- **로그인**: CSV 파일 기반 아이디/비밀번호 인증
- **로그인 완료 화면**: 상단 타이틀 + 프로필 버튼, 참여 중인 모임 방 리스트(현재는 빈 상태), 방 만들기/참여하기 버튼 (추후 기능 구현 예정 안내)

## 데이터 저장 방식
- 회원 정보는 DB 없이 `data/members.csv` 파일로 저장/관리됩니다.
- 프로그램 최초 실행 시 `data` 폴더와 `members.csv`(헤더만 포함)가 자동으로 생성됩니다.
- CSV 컬럼 순서: `name, nickname, id, password, email`

## 추후 구현 예정 (요구사항 명세 기준)
- 모임 방 생성/초대/나가기 등 모임 관리 기능
- 일정 조율, 참여 인원 조사, 장소 랜덤 추천
- 비용 정산, 입금 확인
- 다이어리(사진 + 한줄 일기) 기능
