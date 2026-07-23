/**
 * ExpenseListDialog.java
 *
 * 방 메인 화면의 예산 "확인" 버튼을 눌렀을 때 뜨는 화면입니다. 이 방의 지출(비용) 내역을
 * 관리하는 중심 화면으로, 아래 기능들을 전부 이 창 하나에서 시작합니다.
 *
 *   - 지출 내역 목록 표시 (없으면 "아직 입력된 내역이 없습니다" 안내)
 *   - "추가" 버튼으로 새 지출 등록 (방 전체 인원이 나눠서 부담하는 "일반 지출")
 *   - "차수별 비용 입력" 버튼으로 특정 차수 모임에서 쓴 지출 등록 ("단체 약속" 방에서만 보임)
 *   - 각 항목의 "수정"/"삭제" 버튼 (결제자 본인일 때만 보임)
 *   - 총 지출액 표시
 *   - "정산" 버튼 -> 지금 등록된 지출 내역 전부를 대상으로 정산 계산
 *     (일반 지출은 방 전체 인원이서, 차수별 지출은 그 차수에 참여를 확정한 사람들끼리서 n빵합니다)
 *   - "정산확인" 버튼으로 정산 결과 및 참여자별 정산 완료 상태 확인
 *
 *   <필드>
 *   1. expenseRepository       : 지출 내역 CSV를 읽고 쓰는 저장소 객체
 *   2. settlementRepository    : 정산 결과 CSV를 읽고 쓰는 저장소 객체
 *   3. memberRepository        : 결제자 닉네임을 보여주기 위한 저장소 객체
 *   4. meetingRoundRepository  : 이 방에 만들어진 모임 차수 목록을 가져오는 저장소 객체
 *   5. roundParticipantRepository : 각 차수에 참여를 확정한 사람 목록을 가져오는 저장소 객체
 *   6. room                     : 지금 보고 있는 방
 *   7. loginMember              : 지금 로그인한 사용자 (수정/삭제 권한 확인에 사용)
 *
 *   <중요 메소드>
 *   1. renderExpenses()        : 최신 지출 목록을 불러와 화면을 다시 그리고 총액을 계산함
 *   2. handleAddExpense()      : "추가" 버튼 -> ExpenseEditDialog(추가 모드)를 염
 *   3. handleAddRoundExpense() : "차수별 비용 입력" 버튼 -> RoundExpenseEditDialog를 염 (차수가 없으면 안내만)
 *   4. handleEditExpense()     : 항목의 "수정" 버튼 -> 일반 지출이면 ExpenseEditDialog, 차수별 지출이면
 *                                 RoundExpenseEditDialog를 염 (결제자만 클릭 가능)
 *   5. handleDeleteExpense()   : 항목의 "삭제" 버튼 -> 확인 후 삭제 (결제자만 클릭 가능)
 *   6. handleSettle()          : "정산" 버튼 -> 지금 등록된 지출 전부를 그룹별로 나눠 SettlementCalculator로
 *                                 계산 후 결과 저장, 결과 화면(SettlementListDialog)을 염
 *   7. handleCheckSettlement() : "정산확인" 버튼 -> SettlementListDialog를 염
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Expense;
import com.groupmeeting.model.MeetingRound;
import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.model.SettlementItem;
import com.groupmeeting.util.ExpenseRepository;
import com.groupmeeting.util.MeetingRoundRepository;
import com.groupmeeting.util.MemberRepository;
import com.groupmeeting.util.RoundParticipantRepository;
import com.groupmeeting.util.SettlementCalculator;
import com.groupmeeting.util.SettlementRepository;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseListDialog extends JDialog {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final RoundParticipantRepository roundParticipantRepository;
    private final Room room;
    private final Member loginMember;

    private JPanel expensesPanel;
    private JScrollPane expensesScrollPane; // expensesPanel을 감싸는 가로 스크롤 영역
    private JLabel totalLabel;

    public ExpenseListDialog(Window owner, ExpenseRepository expenseRepository, SettlementRepository settlementRepository,
                              MemberRepository memberRepository, MeetingRoundRepository meetingRoundRepository,
                              RoundParticipantRepository roundParticipantRepository, Room room, Member loginMember) {
        super(owner, "예산 확인", ModalityType.APPLICATION_MODAL);
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.memberRepository = memberRepository;
        this.meetingRoundRepository = meetingRoundRepository;
        this.roundParticipantRepository = roundParticipantRepository;
        this.room = room;
        this.loginMember = loginMember;
        initDialog();
        initComponents();
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(제목, 지출 목록, 총액, 추가/차수별비용/정산/정산확인/닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 예산 확인");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        expensesPanel = new JPanel();
        expensesPanel.setLayout(new BoxLayout(expensesPanel, BoxLayout.Y_AXIS));
        expensesPanel.setOpaque(false);
        expensesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalLabel = new JLabel();
        totalLabel.setFont(Theme.FONT_SUBTITLE);
        totalLabel.setForeground(Theme.TEXT_DARK);
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        JButton addButton = new JButton("추가");
        Theme.styleSecondaryButton(addButton);
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        addButton.addActionListener(e -> handleAddExpense());

        // "차수별 비용 입력" 버튼은 "단체 약속" 방에서만 보입니다. ("단체 여행" 방에는 차수 개념이 없음)
        boolean isPromiseRoom = Room.CATEGORY_PROMISE.equals(room.getCategory());
        JButton roundExpenseButton = new JButton("차수별 비용 입력");
        Theme.styleSecondaryButton(roundExpenseButton);
        roundExpenseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        roundExpenseButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        roundExpenseButton.addActionListener(e -> handleAddRoundExpense());

        JButton settleButton = new JButton("정산");
        Theme.styleButton(settleButton);
        settleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        settleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        settleButton.addActionListener(e -> handleSettle());

        JButton statusButton = new JButton("정산확인");
        Theme.styleSecondaryButton(statusButton);
        statusButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        statusButton.addActionListener(e -> handleCheckSettlement());

        JButton closeButton = new JButton("닫기");
        closeButton.setFont(Theme.FONT_NORMAL);
        closeButton.setForeground(new Color(0x99, 0x99, 0x99));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dispose());

        // 지출 목록을 먼저 채운 뒤(render-before-freeze), 글자가 길어도 잘리지 않도록
        // 가로 스크롤이 가능한 영역으로 감쌉니다.
        renderExpenses();
        expensesScrollPane = Theme.wrapHorizontalScrollable(expensesPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(expensesScrollPane);
        root.add(Box.createVerticalStrut(10));
        root.add(totalLabel);
        root.add(Box.createVerticalStrut(20));
        root.add(addButton);
        if (isPromiseRoom) {
            root.add(Box.createVerticalStrut(8));
            root.add(roundExpenseButton);
        }
        root.add(Box.createVerticalStrut(8));
        root.add(settleButton);
        root.add(Box.createVerticalStrut(8));
        root.add(statusButton);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** 지출 목록을 CSV에서 다시 불러와 화면을 새로 그리고, 총 지출액도 다시 계산합니다. */
    private void renderExpenses() {
        expensesPanel.removeAll();

        List<Expense> expenses = expenseRepository.getForRoom(room.getCode());

        if (expenses.isEmpty()) {
            JLabel emptyLabel = new JLabel("아직 입력된 내역이 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            expensesPanel.add(emptyLabel);
        } else {
            for (Expense expense : expenses) {
                expensesPanel.add(createExpenseRow(expense));
                expensesPanel.add(Box.createVerticalStrut(6));
            }
        }

        // 총 지출액 = 목록에 있는 모든 항목의 금액 합계
        long total = 0;
        for (Expense expense : expenses) {
            total += expense.getAmount();
        }
        totalLabel.setText("총 지출액: " + formatWon(total));

        expensesPanel.revalidate();
        expensesPanel.repaint();

        // 이미 가로 스크롤 영역으로 감싼 뒤(=추가/수정/삭제로 다시 그리는 경우)라면,
        // 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (expensesScrollPane != null) {
            Theme.resyncScrollableHeight(expensesScrollPane, expensesPanel);
            revalidate();
            repaint();
        }
    }

    /**
     * 지출 항목 한 줄을 만듭니다.
     * 왼쪽: 결제자·금액·사유(+차수 이름 또는 기타) 정보 / 오른쪽: 수정·삭제 버튼
     * (수정·삭제 버튼은 로그인한 사용자가 이 지출의 결제자일 때만 보이도록 합니다.)
     */
    private JPanel createExpenseRow(Expense expense) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---------- 왼쪽: 결제자 / 금액 / 사유 / (차수 이름 또는 기타) ----------
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        String payerNickname = findNickname(expense.getPayerId());
        JLabel mainLine = new JLabel(payerNickname + " · " + formatWon(expense.getAmount()));
        mainLine.setFont(Theme.FONT_NORMAL);
        mainLine.setForeground(Theme.TEXT_DARK);
        mainLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 차수별 지출이면 차수 이름을 태그처럼 붙여주고, 아니면 기존처럼 기타 메모를 붙여줍니다.
        String subText;
        if (expense.isRoundExpense()) {
            subText = expense.getReason() + " · " + findRoundName(expense.getRoundId());
        } else {
            subText = expense.getReason() + (expense.getNote().isEmpty() ? "" : " (" + expense.getNote() + ")");
        }
        JLabel subLine = new JLabel(subText);
        subLine.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        subLine.setForeground(new Color(0x99, 0x99, 0x99));
        subLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(mainLine);
        infoPanel.add(subLine);

        // ---------- 오른쪽: 수정/삭제 버튼 (결제자 본인만 보이도록) ----------
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);

        boolean isOwner = loginMember.getId().equals(expense.getPayerId());
        if (isOwner) {
            JButton editButton = smallLinkButton("수정", Theme.PRIMARY_GREEN_DARK);
            editButton.addActionListener(e -> handleEditExpense(expense));

            JButton deleteButton = smallLinkButton("삭제", new Color(0x99, 0x99, 0x99));
            deleteButton.addActionListener(e -> handleDeleteExpense(expense));

            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);
        }

        row.add(infoPanel, BorderLayout.CENTER);
        row.add(buttonPanel, BorderLayout.EAST);
        return row;
    }

    /** 테두리/배경 없이 글씨만 있는 작은 버튼(수정/삭제용)을 만듭니다. */
    private JButton smallLinkButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        button.setForeground(color);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** "추가" 버튼 클릭 시 실행됩니다. (방 전체 인원이 나눠 부담하는 "일반 지출" 추가 화면) */
    private void handleAddExpense() {
        ExpenseEditDialog dialog = new ExpenseEditDialog(this, expenseRepository, memberRepository, room, null);
        dialog.setVisible(true);
        renderExpenses();
    }

    /**
     * "차수별 비용 입력" 버튼 클릭 시 실행됩니다.
     * 이 방에 만들어진 차수가 하나도 없으면, 먼저 차수를 추가하라고 안내만 하고 화면을 열지 않습니다.
     */
    private void handleAddRoundExpense() {
        List<MeetingRound> rounds = meetingRoundRepository.getForRoom(room.getCode());
        if (rounds.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "먼저 '차수별 인원조사'에서 차수를 하나 이상 추가해주세요.",
                    "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RoundExpenseEditDialog dialog = new RoundExpenseEditDialog(
                this, expenseRepository, memberRepository, meetingRoundRepository, room, null);
        dialog.setVisible(true);
        renderExpenses();
    }

    /**
     * 항목의 "수정" 버튼 클릭 시 실행됩니다. (결제자 본인만 이 버튼을 볼 수 있으므로 별도 권한 확인은 생략)
     * 차수별 지출이면 RoundExpenseEditDialog, 일반 지출이면 ExpenseEditDialog를 엽니다.
     */
    private void handleEditExpense(Expense expense) {
        if (expense.isRoundExpense()) {
            RoundExpenseEditDialog dialog = new RoundExpenseEditDialog(
                    this, expenseRepository, memberRepository, meetingRoundRepository, room, expense);
            dialog.setVisible(true);
        } else {
            ExpenseEditDialog dialog = new ExpenseEditDialog(this, expenseRepository, memberRepository, room, expense);
            dialog.setVisible(true);
        }
        renderExpenses();
    }

    /** 항목의 "삭제" 버튼 클릭 시 실행됩니다. 확인 후 삭제합니다. */
    private void handleDeleteExpense(Expense expense) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "'" + expense.getReason() + "' 지출 내역을 삭제하시겠습니까?",
                "삭제 확인", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            expenseRepository.deleteExpense(expense.getId());
            renderExpenses();
        }
    }

    /**
     * "정산" 버튼 클릭 시 실행됩니다.
     * 지금 등록된 지출 내역 전부를 대상으로 정산을 계산합니다.
     * - roundId가 없는 "일반 지출"은 방 전체 참여자가 n빵합니다.
     * - roundId가 있는 "차수별 지출"은 그 차수에 참여를 확정한 사람들끼리만 n빵합니다.
     * (한 사람이 여러 그룹에 걸쳐 있어도 SettlementCalculator가 한 번에 합쳐서 계산해줍니다)
     */
    private void handleSettle() {
        List<Expense> allExpenses = expenseRepository.getForRoom(room.getCode());

        if (allExpenses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "정산할 지출 내역이 없습니다.",
                    "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 일반 지출(roundId 없음)과 차수별 지출(roundId 있음, 차수마다 따로)로 나눠서 그룹을 만듭니다.
        List<Expense> regularExpenses = new ArrayList<>();
        Map<String, List<Expense>> expensesByRound = new LinkedHashMap<>();
        for (Expense expense : allExpenses) {
            if (expense.isRoundExpense()) {
                expensesByRound.computeIfAbsent(expense.getRoundId(), k -> new ArrayList<>()).add(expense);
            } else {
                regularExpenses.add(expense);
            }
        }

        List<SettlementCalculator.Group> groups = new ArrayList<>();
        if (!regularExpenses.isEmpty()) {
            groups.add(new SettlementCalculator.Group(regularExpenses, room.getMemberIds()));
        }
        for (Map.Entry<String, List<Expense>> entry : expensesByRound.entrySet()) {
            List<String> participantIds = roundParticipantRepository.getParticipantIds(entry.getKey());
            groups.add(new SettlementCalculator.Group(entry.getValue(), participantIds));
        }

        List<SettlementItem> plan = SettlementCalculator.calculateCombined(room.getCode(), groups);
        settlementRepository.replaceForRoom(room.getCode(), plan);

        JOptionPane.showMessageDialog(this,
                "정산이 계산되었습니다. 참여자를 선택해서 각자 얼마를 주고받아야 하는지 확인해보세요.",
                "정산 완료", JOptionPane.INFORMATION_MESSAGE);

        SettlementListDialog dialog = new SettlementListDialog(this, settlementRepository, memberRepository, room, loginMember);
        dialog.setVisible(true);
    }

    /** "정산확인" 버튼 클릭 시 실행됩니다. 정산 결과 및 참여자별 완료 상태를 보여주는 화면을 엽니다. */
    private void handleCheckSettlement() {
        SettlementListDialog dialog = new SettlementListDialog(this, settlementRepository, memberRepository, room, loginMember);
        dialog.setVisible(true);
    }

    /** 숫자를 "10,000원" 형태의 문자열로 바꿉니다. */
    private String formatWon(long amount) {
        return NumberFormat.getInstance(Locale.KOREA).format(amount) + "원";
    }

    /** 회원 아이디로 닉네임을 찾습니다. 못 찾으면 아이디를 그대로 반환합니다. */
    private String findNickname(String memberId) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(memberId)) {
                return m.getNickname();
            }
        }
        return memberId;
    }

    /** 차수 아이디로 차수 이름을 찾습니다. 못 찾으면(삭제된 경우 등) 안내 문구를 반환합니다. */
    private String findRoundName(String roundId) {
        for (MeetingRound round : meetingRoundRepository.getForRoom(room.getCode())) {
            if (round.getId().equals(roundId)) {
                return round.getName();
            }
        }
        return "삭제된 차수";
    }
}
