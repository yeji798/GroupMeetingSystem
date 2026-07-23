package com.groupmeeting.model;

/**
 * "단체 약속" 방에서 참여자가 제출한 가능 날짜/시간/장소 한 건을 담는 모델(VO) 클래스입니다.
 * CSV 파일(availability.csv) 한 줄이 AvailabilityEntry 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: roomCode, memberId, date, startTime, endTime, place
 */
public class AvailabilityEntry {

    private String roomCode;   // 방 코드
    private String memberId;   // 제출한 회원의 로그인 아이디
    private String date;       // 날짜 (yyyy-MM-dd)
    private String startTime;  // 시작 시간 (HH:mm, 24시간제)
    private String endTime;    // 끝 시간 (HH:mm, 24시간제)
    private String place;      // 추천 장소

    public AvailabilityEntry(String roomCode, String memberId, String date,
                              String startTime, String endTime, String place) {
        this.roomCode = roomCode;
        this.memberId = memberId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.place = place;
    }

    // ---------------- Getter ----------------
    public String getRoomCode() { return roomCode; }
    public String getMemberId() { return memberId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getPlace() { return place; }

    // ---------------- Setter ----------------
    public void setPlace(String place) { this.place = place; }

    @Override
    public String toString() {
        return "AvailabilityEntry{" +
                "roomCode='" + roomCode + '\'' +
                ", memberId='" + memberId + '\'' +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", place='" + place + '\'' +
                '}';
    }
}
