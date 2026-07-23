/**
 * ExpenseEditDialog.java
 *
 * 지출 내역 하나를 "새로 추가"하거나 "수정"할 때 뜨는 화면입니다.
 * 생성자에 넘겨주는 existingExpense가 null이면 "추가 모드", 값이 있으면 "수정 모드"로 동작하며
 * 수정 모드에서는 기존 값들을 입력칸에 미리 채워 넣습니다.
 *
 * 입력받는 항목: 결제자(현재 방 참여자 중 선택), 결제 비용(숫자만), 지출 사유(문자열), 기타(문자열)
 *
 *   <필드>
 *   1. expenseRepository : 지출 내역 CSV를 읽고 쓰는 저장소 객체
 *   2. room                : 지금 지출을 등록/수정 중인 방
 *   3. existingExpense    : 수정 대상 지출 (추가 모드일 때는 null)
 *
 *   <생성자>
 *   : 창을 만들고, 수정 모드라면 기존 값을 입력칸에 미리 채워 넣음
 *
 *   <중요 메소드>
 *   1. handleSave() : "저장" 버튼 -> 입력값 검증 후, 추가 모드면 새로 등록 / 수정 모드면 기존 내용을 덮어씀
 *   2. parseAmount(text) : "10,000" 같은 문자열에서 콤마를 제거하고 숫자(long)로 바꿔줌
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Expense;
import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.util.ExpenseRepository;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class ExpenseEditDialog extends JDialog {

    private final ExpenseRepository expenseRepository;
    private final Room room;
    private final Expense existingExpense; // null이면 "추가 모드", 값이 있으면 "수정 모드"

    private JComboBox<MemberOption> payerCombo;
    private JTextField amountField;
    private JTextField reasonField;
    private JTextField noteField;

    public ExpenseEditDialog(Window owner, ExpenseRepository expenseRepository, MemberRepository memberRepository,
                              Room room, Expense existingExpense) {
        super(owner, existingExpense == null ? "지출 추가" : "지출 수정", ModalityType.APPLICATION_MODAL);
        this.expenseRepository = expenseRepository;
        this.room = room;
        this.existingExpense = existingExpense;
        initDialog();
        initComponents(memberRepository);
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(결제자/비용/사유/기타 입력칸, 저장 버튼)를 배치합니다. */
    private void initComponents(MemberRepository memberRepository) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel(existingExpense == null ? "지출 추가" : "지출 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        // ---------- 기타 ----------
        JLabel noteLabel = sectionLabel("기타");
        noteField = new JTextField();
        Theme.styleTextField(noteField);
        noteField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        noteField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 수정 모드라면, 기존 값을 각 입력칸에 미리 채워 넣는다.
        if (existingExpense != null) {
            selectPayerInCombo(existingExpense.getPayerId());
            amountField.setText(String.valueOf(existingExpense.getAmount()));
            reasonField.setText(existingExpense.getReason());
            noteField.setText(existingExpense.getNote());
        }

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveButton.addActionListener(e -> handleSave());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(24));
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
        root.add(noteLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(noteField);
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

        String note = noteField.getText().trim(); // 기타 항목은 비워둬도 됨

        if (existingExpense == null) {
            // ---------- 추가 모드 ----------
            String id = UUID.randomUUID().toString(); // 지출 내역을 구분할 고유 아이디를 새로 만든다.
            // "추가" 버튼으로 등록하는 지출은 특정 차수에 연결되지 않은 "일반 지출"이므로 roundId는 빈 문자열입니다.
            Expense newExpense = new Expense(id, room.getCode(), selectedPayer.memberId, amount, reason, note, "");
            expenseRepository.addExpense(newExpense);
        } else {
            // ---------- 수정 모드: 기존 객체의 값을 새 값으로 바꾼 뒤 그대로 저장 ----------
            existingExpense.setPayerId(selectedPayer.memberId);
            existingExpense.setAmount(amount);
            existingExpense.setReason(reason);
            existingExpense.setNote(note);
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
}
