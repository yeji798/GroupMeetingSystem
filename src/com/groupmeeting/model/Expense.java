package com.groupmeeting.model;

/**
 * 방의 지출(비용) 내역 한 건을 담는 모델(VO) 클래스입니다.
 * CSV 파일(expenses.csv) 한 줄이 Expense 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: id, roomCode, payerId, amount, reason, note, roundId
 *
 * roundId는 "차수별 비용 입력"으로 등록한 지출일 때만 값이 들어있고(그 차수의 아이디),
 * 평범하게 "추가" 버튼으로 등록한 지출이라면 빈 문자열("")입니다.
 * -> 정산할 때, roundId가 있는 지출은 "그 차수에 참여한 사람들끼리만" 나눠서 계산하고,
 *    roundId가 없는 일반 지출은 "방 전체 인원"이서 나눠서 계산합니다. (ExpenseListDialog 참고)
 */
public class Expense {

    private String id;       // 지출 내역을 구분하는 고유 아이디 (UUID)
    private String roomCode; // 이 지출이 속한 방의 코드
    private String payerId;  // 결제자(돈을 낸 사람)의 로그인 아이디
    private long amount;     // 결제 금액 (원)
    private String reason;   // 지출 사유
    private String note;     // 기타 메모
    private String roundId;  // 특정 차수 모임에 연결된 지출이면 그 차수의 아이디, 아니면 빈 문자열("")

    public Expense(String id, String roomCode, String payerId, long amount, String reason, String note, String roundId) {
        this.id = id;
        this.roomCode = roomCode;
        this.payerId = payerId;
        this.amount = amount;
        this.reason = reason;
        this.note = note;
        this.roundId = (roundId != null) ? roundId : "";
    }

    // ---------------- Getter ----------------
    public String getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public String getPayerId() { return payerId; }
    public long getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getNote() { return note; }
    public String getRoundId() { return roundId; }

    // ---------------- Setter ----------------
    // "수정" 기능에서 값을 바꿀 때 사용합니다.
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setReason(String reason) { this.reason = reason; }
    public void setNote(String note) { this.note = note; }
    public void setRoundId(String roundId) { this.roundId = (roundId != null) ? roundId : ""; }

    /** 이 지출이 특정 차수 모임에 연결되어 있는지 확인합니다. (roundId가 비어있지 않으면 true) */
    public boolean isRoundExpense() {
        return roundId != null && !roundId.isEmpty();
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id='" + id + '\'' +
                ", roomCode='" + roomCode + '\'' +
                ", payerId='" + payerId + '\'' +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                ", note='" + note + '\'' +
                ", roundId='" + roundId + '\'' +
                '}';
    }
}
