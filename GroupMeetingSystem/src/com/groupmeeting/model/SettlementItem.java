package com.groupmeeting.model;

/**
 * "정산" 결과, 한 사람이 다른 한 사람에게 돈을 보내야 한다는 내용 한 건을 담는 모델(VO) 클래스입니다.
 * 예) fromMemberId(주는 사람) -> toMemberId(받는 사람), amount(금액)
 * CSV 파일(settlements.csv) 한 줄이 SettlementItem 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: roomCode, fromMemberId, toMemberId, amount, confirmed
 */
public class SettlementItem {

    private String roomCode;     // 이 정산이 속한 방의 코드
    private String fromMemberId; // 돈을 보내야 하는(줘야 하는) 사람의 아이디
    private String toMemberId;   // 돈을 받아야 하는 사람의 아이디
    private long amount;         // 주고받을 금액 (원)
    private boolean confirmed;   // 실제로 입금(정산)이 완료되었다고 체크했는지 여부

    public SettlementItem(String roomCode, String fromMemberId, String toMemberId, long amount, boolean confirmed) {
        this.roomCode = roomCode;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.amount = amount;
        this.confirmed = confirmed;
    }

    // ---------------- Getter ----------------
    public String getRoomCode() { return roomCode; }
    public String getFromMemberId() { return fromMemberId; }
    public String getToMemberId() { return toMemberId; }
    public long getAmount() { return amount; }
    public boolean isConfirmed() { return confirmed; }

    // ---------------- Setter ----------------
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    /** 이 정산 항목에 memberId(나)가 관련되어 있는지(주거나 받는 쪽인지) 확인합니다. */
    public boolean involves(String memberId) {
        return fromMemberId.equals(memberId) || toMemberId.equals(memberId);
    }
}
