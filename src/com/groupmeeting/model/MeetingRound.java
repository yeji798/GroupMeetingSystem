package com.groupmeeting.model;

/**
 * "차수별 인원 조사" 기능에서, 방 안에 만들어진 모임 차수(예: "1차 모임", "2차 모임", "뒤풀이") 하나를
 * 담는 모델(VO) 클래스입니다.
 * CSV 파일(meeting_rounds.csv) 한 줄이 MeetingRound 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: roomCode, id, name
 */
public class MeetingRound {

    private String roomCode; // 이 차수가 속한 방의 코드
    private String id;       // 차수를 구분하는 고유 아이디 (UUID)
    private String name;     // 차수 모임 이름 (예: "1차 모임")

    public MeetingRound(String roomCode, String id, String name) {
        this.roomCode = roomCode;
        this.id = id;
        this.name = name;
    }

    // ---------------- Getter ----------------
    public String getRoomCode() { return roomCode; }
    public String getId() { return id; }
    public String getName() { return name; }
}
