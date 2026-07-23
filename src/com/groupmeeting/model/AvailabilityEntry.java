package com.groupmeeting.model;

/**
 * 한 회원이 특정 모임에서 가능한 하나의 날짜·시간 구간입니다.
 *
 * <p>장소 후보는 일정과 생명주기가 다르므로 이 모델에 포함하지 않고
 * {@code PlaceRepository}에서 별도로 관리합니다.</p>
 */
public class AvailabilityEntry {

    private final String roomCode;
    private final String memberId;
    private final String date;
    private final String startTime;
    private final String endTime;

    public AvailabilityEntry(
            String roomCode,
            String memberId,
            String date,
            String startTime,
            String endTime
    ) {
        this.roomCode = roomCode;
        this.memberId = memberId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getRoomCode() { return roomCode; }
    public String getMemberId() { return memberId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    @Override
    public String toString() {
        return "AvailabilityEntry{" +
                "roomCode='" + roomCode + '\'' +
                ", memberId='" + memberId + '\'' +
                ", date='" + date + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
