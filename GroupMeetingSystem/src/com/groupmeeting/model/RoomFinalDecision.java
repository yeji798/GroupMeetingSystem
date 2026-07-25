package com.groupmeeting.model;

/**
 * 방장이 최종적으로 확정한 "모임 최종 날짜 / 최종 시간 / 최종 장소"를 담는 모델(VO) 클래스입니다.
 * 아직 확정하지 않은 항목은 빈 문자열("")로 표현합니다. (방 하나당 이 정보는 한 세트만 존재)
 *
 * CSV 파일(room_final.csv) 한 줄이 RoomFinalDecision 객체 하나에 대응됩니다.
 * CSV 컬럼 순서: roomCode, finalDate, finalStartTime, finalEndTime, finalPlace
 *
 * - "단체 약속" 방: finalDate + finalStartTime + finalEndTime + finalPlace 모두 사용
 * - "단체 여행" 방: finalDate(날짜 구간 표시용 문자열) + finalPlace만 사용 (시간은 사용 안 함)
 */
public class RoomFinalDecision {

    private String roomCode;
    private String finalDate;      // 화면에 바로 보여줄 수 있는 형태로 저장 (예: "7월 3일" 또는 "7월 1일 ~ 7월 3일")
    private String finalStartTime; // "14:00" (단체 약속 전용)
    private String finalEndTime;   // "16:30" (단체 약속 전용)
    private String finalPlace;

    public RoomFinalDecision(String roomCode, String finalDate, String finalStartTime,
                              String finalEndTime, String finalPlace) {
        this.roomCode = roomCode;
        this.finalDate = finalDate;
        this.finalStartTime = finalStartTime;
        this.finalEndTime = finalEndTime;
        this.finalPlace = finalPlace;
    }

    /** 아직 아무것도 확정되지 않은, 방 코드만 있는 빈 상태의 객체를 만듭니다. */
    public static RoomFinalDecision empty(String roomCode) {
        return new RoomFinalDecision(roomCode, "", "", "", "");
    }

    // ---------------- Getter ----------------
    public String getRoomCode() { return roomCode; }
    public String getFinalDate() { return finalDate; }
    public String getFinalStartTime() { return finalStartTime; }
    public String getFinalEndTime() { return finalEndTime; }
    public String getFinalPlace() { return finalPlace; }

    // ---------------- Setter ----------------
    public void setFinalDate(String finalDate) { this.finalDate = finalDate; }
    public void setFinalStartTime(String finalStartTime) { this.finalStartTime = finalStartTime; }
    public void setFinalEndTime(String finalEndTime) { this.finalEndTime = finalEndTime; }
    public void setFinalPlace(String finalPlace) { this.finalPlace = finalPlace; }
}
