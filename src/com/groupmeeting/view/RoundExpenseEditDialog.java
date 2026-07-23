/**
 * RoundExpenseEditDialog.java
 *
 * "차수별 비용 입력" 화면입니다. ExpenseEditDialog(일반 지출 추가/수정)과 거의 같지만,
 * "기타" 자유 입력칸 대신 "이 지출이 어느 차수 모임에서 쓰였는지"를 고르는 드롭다운이 있습니다.
 *
 * 여기서 등록한 지출은 나중에 예산 화면에서 "정산" 버튼을 눌렀을 때, 방 전체 인원이 아니라
 * 그 차수에 "참여"를 확정한 사람들끼리만 n빵해서 정산됩니다. (SettlementCalculator.Group 참고)
 *
 * existingExpense가 null이면 "추가 모드", 값이 있으면 "수정 모드"로 동작합니다.
 * (이 화면은 반드시 차수가 하나 이상 존재하는 방에서만 열려야 하므로, 이 화면을 여는 쪽인
 *  ExpenseListDialog에서 미리 "이 방에 만들어진 차수가 있는지"를 확인하고 없으면 안내만 띄웁니다)
 *
 *   <필드>
 *   1. expenseRepository : 지출 내역 CSV를 읽고 쓰는 저장소 객체
 *   2. room                : 지금 지출을 등록/수정 중인 방
 *   3. existingExpense    : 수정 대상 지출 (추가 모드일 때는 null)
 *
 *   <중요 메소드>
 *   1. handleSave() : "저장" 버튼 -> 입력값 검증 후, 추가 모드면 새로 등록 / 수정 모드면 기존 내용을 덮어씀
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Expense;
import com.groupmeeting.model.Member;
import com.groupmeeting.model.MeetingRound;
import com.groupmeeting.model.Room;
import com.groupmeeting.util.ExpenseRepository;
import com.groupmeeting.util.MeetingRoundRepository;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class RoundExpenseEditDialog extends JDialog {

    private final ExpenseRepository expenseRepository;
    private final Room room;
    private final Expense existingExpense; // null이면 "추가 모드", 값이 있으면 "수정 모드"

    private JComboBox<MemberOption> payerCombo;
    private JTextField amountField;
    private JTextField reasonField;
    private JComboBox<RoundOption> roundCombo;

    public RoundExpenseEditDialog(Window owner, ExpenseRepository expenseRepository, MemberRepository memberRepository,
                                   MeetingRoundRepository meetingRoundRepository, Room room, Expense existingExpense) {
        super(owner, existingExpense == null ? "차수별 비용 입력" : "차수별 비용 수정", ModalityType.APPLICATION_MODAL);
        this.expenseRepository = expenseRepository;
        this.room = room;
        this.existingExpense = existingExpense;
        initDialog();
        initComponents(memberRepository, meetingRoundRepository);
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(결제자/비용/사유/차수 입력칸, 저장 버튼)를 배치합니다. */
    private void initComponents(MemberRepository memberRepository, MeetingRoundRepository meetingRoundRepository) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel(existingExpense == null ? "차수별 비용 입력" : "차수별 비용 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("정산 시 아래에서 고른 차수의 참여자들끼리만 나눠서 계산됩니다.");
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ---------- 결제자 선택 (방 참여자 목록을 콤보박스로) ----------
        JLabel payerLabel = sectionLabel("결제자");
        payerCombo = new JComboBox<>();
        for (String memberId : room.getMemberIds()) {
            Member member = findMemberById(memberRepository, memberId);
            String nickname = (member != null) ? member.getNickname() : memberId;
            payerCombo.addItem(new MemberOption(memberId, nickname));
        }
        payerCombo.setFont(Theme.FONT_NORMAL);
        payerCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        payerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        // ---------- 결제 비용 ----------
        JLabel amountLabel = sectionLabel("결제 비용 (원)");
        amountField = new JTextField();
        Theme.styleTextField(amountField);
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---------- 지출 사유 ----------
        JLabel reasonLabel = sectionLabel("지출 사유");
        reasonField = new JTextField();
        Theme.styleTextField(reasonField);
        reasonField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        reasonField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---------- 차수 선택 (기타 칸 대신, 이 방에 만들어진 차수 모임 중 하나를 고름) ----------
        JLabel roundLabel = sectionLabel("차수 선택");
        roundCombo = new JComboBox<>();
        List<MeetingRound> rounds = meetingRoundRepository.getForRoom(room.getCode());
        for (MeetingRound round : rounds) {
            roundCombo.addItem(new RoundOption(round.getId(), round.getName()));
        }
        roundCombo.setFont(Theme.FONT_NORMAL);
        roundCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        roundCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        // 수정 모드라면, 기존 값을 각 입력칸에 미리 채워 넣는다.
        if (existingExpense != null) {
            selectPayerInCombo(existingExpense.getPayerId());
            amountField.setText(String.valueOf(existingExpense.getAmount()));
            reasonField.setText(existingExpense.getReason());
            selectRoundInCombo(existingExpense.getRoundId());
        }

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveButton.addActionListener(e -> handleSave());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(hintLabel);
        root.add(Box.createVerticalStrut(20));
        root.add(payerLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(payerCombo);
        root.add(Box.createVerticalStrut(16));
        root.add(amountLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(amountField);
        root.add(Box.createVerticalStrut(16));
        root.add(reasonLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(reasonField);
        root.add(Box.createVerticalStrut(16));
        root.add(roundLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(roundCombo);
        root.add(Box.createVerticalStrut(28));
        root.add(saveButton);

        setContentPane(root);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_NORMAL);
        label.setForeground(Theme.TEXT_DARK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** 콤보박스에서 memberId가 payerId와 일치하는 항목을 찾아 선택 상태로 만듭니다. (수정 모드 초기값용) */
    private void selectPayerInCombo(String payerId) {
        for (int i = 0; i < payerCombo.getItemCount(); i++) {
            if (payerCombo.getItemAt(i).memberId.equals(payerId)) {
                payerCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** 콤보박스에서 roundId가 일치하는 항목을 찾아 선택 상태로 만듭니다. (수정 모드 초기값용) */
    private void selectRoundInCombo(String roundId) {
        for (int i = 0; i < roundCombo.getItemCount(); i++) {
            if (roundCombo.getItemAt(i).roundId.equals(roundId)) {
                roundCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * "저장" 버튼 클릭 시 실행됩니다.
     * 입력값을 검증한 뒤, 추가 모드면 새 지출로 등록하고 수정 모드면 기존 지출 내용을 덮어씁니다.
     */
    private void handleSave() {
        MemberOption selectedPayer = (MemberOption) payerCombo.getSelectedItem();
        if (selectedPayer == null) {
            showWarning("결제자를 선택해주세요.");
            return;
        }

        long amount;
        try {
            amount = parseAmount(amountField.getText());
        } catch (NumberFormatException ex) {
            showWarning("결제 비용은 숫자로 입력해주세요.");
            return;
        }
        if (amount <= 0) {
            showWarning("결제 비용은 0원보다 커야 합니다.");
            return;
        }

        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            showWarning("지출 사유를 입력해주세요.");
            return;
        }

        RoundOption selectedRound = (RoundOption) roundCombo.getSelectedItem();
        if (selectedRound == null) {
            showWarning("차수를 선택해주세요.");
            return;
        }

        if (existingExpense == null) {
            // ---------- 추가 모드 ----------
            String id = UUID.randomUUID().toString(); // 지출 내역을 구분할 고유 아이디를 새로 만든다.
            Expense newExpense = new Expense(id, room.getCode(), selectedPayer.memberId, amount, reason, "", selectedRound.roundId);
            expenseRepository.addExpense(newExpense);
        } else {
            // ---------- 수정 모드: 기존 객체의 값을 새 값으로 바꾼 뒤 그대로 저장 ----------
            existingExpense.setPayerId(selectedPayer.memberId);
            existingExpense.setAmount(amount);
            existingExpense.setReason(reason);
            existingExpense.setRoundId(selectedRound.roundId);
            expenseRepository.updateExpense(existingExpense);
        }

        dispose();
    }

    /** "10,000"처럼 콤마가 섞인 문자열에서 숫자가 아닌 문자를 모두 제거한 뒤 long으로 변환합니다. */
    private long parseAmount(String text) {
        String digitsOnly = text.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            throw new NumberFormatException("숫자가 입력되지 않았습니다.");
        }
        return Long.parseLong(digitsOnly);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }

    private Member findMemberById(MemberRepository memberRepository, String id) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 결제자 콤보박스에 표시되는 "아이디 + 닉네임" 한 쌍을 담는 작은 클래스입니다.
     * -> 콤보박스 화면에는 toString()이 반환하는 닉네임만 보이고, 실제 저장에는 memberId를 사용합니다.
     */
    private static class MemberOption {
        final String memberId;
        final String nickname;

        MemberOption(String memberId, String nickname) {
            this.memberId = memberId;
            this.nickname = nickname;
        }

        @Override
        public String toString() {
            return nickname;
        }
    }

    /**
     * 차수 콤보박스에 표시되는 "아이디 + 차수 이름" 한 쌍을 담는 작은 클래스입니다.
     * -> 콤보박스 화면에는 toString()이 반환하는 차수 이름만 보이고, 실제 저장에는 roundId를 사용합니다.
     */
    private static class RoundOption {
        final String roundId;
        final String name;

        RoundOption(String roundId, String name) {
            this.roundId = roundId;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
