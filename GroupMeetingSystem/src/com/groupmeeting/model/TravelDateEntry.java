package com.groupmeeting.model;

/**
 * "단체 여행" 방에서 참여자가 제안한 여행 시작일/종료일 한 건을 담는 모델(VO) 클래스입니다.
 * (단체 약속의 AvailabilityEntry와 달리 시간 없이 날짜만 가집니다.)
 * CSV 파일(travel_dates.csv) 한 줄이 TravelDateEntry 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: roomCode, memberId, startDate, endDate
 */
public class TravelDateEntry {

    private String roomCode;   // 방 코드
    private String memberId;   // 제출한 회원의 로그인 아이디
    private String startDate;  // 여행 시작 날짜 (yyyy-MM-dd)
    private String endDate;    // 여행 종료 날짜 (yyyy-MM-dd)

    public TravelDateEntry(String roomCode, String memberId, String startDate, String endDate) {
        this.roomCode = roomCode;
        this.memberId = memberId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // ---------------- Getter ----------------
    public String getRoomCode() { return roomCode; }
    public String getMemberId() { return memberId; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}
