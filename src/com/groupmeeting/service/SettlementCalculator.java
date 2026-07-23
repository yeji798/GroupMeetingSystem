/**
 * SettlementCalculator.java
 *
 * 지출 내역들을 정해진 인원수대로 "n빵"(똑같이 나누기)한 뒤, 실제로 누가 누구에게
 * 얼마를 보내야 하는지 계산하는 순수 계산 전용 클래스입니다.
 *
 * <지출 종류>
 *  - 일반 지출: 방 전체 참여자가 다같이 나눠서 부담하는 지출
 *  - 차수별 지출: "차수별 비용 입력"으로 등록한 지출로, 그 차수에 참여한 사람들끼리만 나눠서 부담
 *      --> calculateCombined()는 그룹을 여러 개 받아서 한 번에 계산
 */

package com.groupmeeting.service;

import com.groupmeeting.model.Expense;
import com.groupmeeting.model.SettlementItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettlementCalculator {

    // 인스턴스를 만들 필요가 없는 계산 전용 클래스이므로 생성자를 막습니다.
    private SettlementCalculator() {
    }

    /**
     * "지출 내역 목록 + 그 지출들을 나눠 부담할 인원 목록" 한 묶음을 나타내는 값 객체입니다.
     * (예: 일반 지출 묶음은 memberIds가 "방 전체 참여자", 차수별 지출 묶음은 memberIds가
     *  "그 차수에 참여한 사람들"이 됩니다)
     */
    public static class Group {
        private final List<Expense> expenses;
        private final List<String> memberIds;

        public Group(List<Expense> expenses, List<String> memberIds) {
            this.expenses = expenses;
            this.memberIds = memberIds;
        }
    }

    /**
     * 여러 개의 지출 묶음(groups)을 한꺼번에 계산해서, 최종적으로 누가 누구에게 얼마를
     * 보내야 하는지 정리한 정산 목록을 반환합니다.
     */
    public static List<SettlementItem> calculateCombined(String roomCode, List<Group> groups) {
        // 그룹마다 계산한 balance(잔액)를 사람별로 누적해서 합칩니다.
        Map<String, Long> totalBalance = new LinkedHashMap<>();

        for (Group group : groups) {
            if (group.memberIds.isEmpty()) {
                continue; // 나눠 부담할 사람이 없는 그룹은 건너뜀 (방어적 처리)
            }
            Map<String, Long> groupBalance = computeGroupBalance(group.expenses, group.memberIds);
            for (Map.Entry<String, Long> entry : groupBalance.entrySet()) {
                totalBalance.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }

        return settleBalances(roomCode, totalBalance);
    }

    /**
     * 지출 묶음 하나(expenses)를 인원(memberIds)에게 n빵했을 때, 각자의 balance(잔액)를 계산합니다.
     * balance = 이 사람이 실제로 낸 금액 - 이 사람이 부담해야 할 1인당 금액
     */
    private static Map<String, Long> computeGroupBalance(List<Expense> expenses, List<String> memberIds) {
        // 1) 지출 총합 계산
        long totalAmount = 0;
        for (Expense expense : expenses) {
            totalAmount += expense.getAmount();
        }

        // 2) 1인당 부담 금액 계산 (나머지는 앞쪽 참여자부터 1원씩 추가 부담)
        int memberCount = memberIds.size();
        long baseShare = totalAmount / memberCount;
        long remainder = totalAmount % memberCount;

        Map<String, Long> fairShareByMember = new LinkedHashMap<>();
        for (int i = 0; i < memberCount; i++) {
            long share = baseShare + (i < remainder ? 1 : 0);
            fairShareByMember.put(memberIds.get(i), share);
        }

        // 3) 각 참여자가 실제로 낸 금액 합산
        Map<String, Long> paidByMember = new LinkedHashMap<>();
        for (String memberId : memberIds) {
            paidByMember.put(memberId, 0L); //모든 참여자의 금액을 0으로 초기화
            // 한번도 결제하지 않은 회원이 MAP에 존재하지 않는 경우를 방지하기 위해 
        }
        for (Expense expense : expenses) {
            String payer = expense.getPayerId();
            if (paidByMember.containsKey(payer)) { //현재 정산 참여자인지 확인
                paidByMember.put(payer, paidByMember.get(payer) + expense.getAmount());
                // ㄴ> 기존 결제 금액에 현재 지출 금액 누적
            }
        }

        // 4) balance = 실제로 낸 금액 - 1인당 부담 금액
        Map<String, Long> balance = new LinkedHashMap<>();
        for (String memberId : memberIds) {
            balance.put(memberId, paidByMember.get(memberId) - fairShareByMember.get(memberId));
        }
        return balance;
    }

    /**
     * 최종 balance(잔액) 맵을 가지고, balance가 양수인 사람들(받을 사람)과 음수인 사람들(줄
     * 사람)을 짝지어서 최대한 적은 횟수의 송금으로 모든 balance가 0이 되도록 정리합니다.
     */
    private static List<SettlementItem> settleBalances(String roomCode, Map<String, Long> totalBalance) {
        List<SettlementItem> result = new ArrayList<>();

        List<MemberBalance> creditors = new ArrayList<>(); // balance > 0 : 돈을 받아야 하는 사람들
        List<MemberBalance> debtors = new ArrayList<>();   // balance < 0 : 돈을 보내야 하는 사람들

        //돈 보내 & 받는 목록 만듦
        for (Map.Entry<String, Long> entry : totalBalance.entrySet()) {
            long balance = entry.getValue();
            if (balance > 0) {
                creditors.add(new MemberBalance(entry.getKey(), balance));
            } else if (balance < 0) {
                debtors.add(new MemberBalance(entry.getKey(), balance));
            }
        }

        // 받을 금액이 큰 사람 / 보낼 금액(절댓값)이 큰 사람 순으로 정렬해서 매칭한다.
        creditors.sort((a, b) -> Long.compare(b.balance, a.balance));       // 내림차순 (큰 금액 먼저)
        debtors.sort((a, b) -> Long.compare(a.balance, b.balance));         // 오름차순 (가장 많이 음수인 사람 먼저)

        // 가장 많이 받을 사람과 가장 많이 보낼 사람을 짝지어 반복 처리
        int ci = 0;
        int di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            MemberBalance creditor = creditors.get(ci);
            MemberBalance debtor = debtors.get(di);

            long amountToMove = Math.min(creditor.balance, -debtor.balance);
            if (amountToMove > 0) {
                result.add(new SettlementItem(roomCode, debtor.memberId, creditor.memberId, amountToMove, false));
            }

            creditor.balance -= amountToMove;
            debtor.balance += amountToMove;

            if (creditor.balance == 0) {
                ci++; // 이 사람은 받을 돈을 다 정리했으므로 다음 사람으로
            }
            if (debtor.balance == 0) {
                di++; // 이 사람은 보낼 돈을 다 정리했으므로 다음 사람으로
            }
        }

        return result;
    }

    /** 계산 중간에만 사용하는 "참여자 아이디 + 잔액" 값 객체입니다. (이 파일 밖에서는 쓰이지 않음) */
    private static class MemberBalance {
        final String memberId;
        long balance;

        MemberBalance(String memberId, long balance) {
            this.memberId = memberId;
            this.balance = balance;
        }
    }
}
