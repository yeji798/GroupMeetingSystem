/**
 * SettlementListDialog.java
 *
 * "정산" 버튼을 눌러서 n빵 계산이 끝난 뒤, 또는 "정산확인" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 방 참여자들의 이름이 목록으로 뜨고, 그 중 한 명을 클릭하면 그 사람이 정산 상황에서
 * 누구에게 얼마를 줘야 하는지 / 받아야 하는지를 보여주는 화면(MySettlementDialog)이 열립니다.
 *
 * 각 참여자 이름 옆에는 그 사람과 관련된 정산 항목들의 완료 상태도 함께 보여줍니다.
 * - 관련된 정산 항목이 하나도 없으면 "해당없음"
 * - 관련된 항목이 전부 "정산 완료" 체크되어 있으면 "완료"
 * - 일부만 체크되어 있으면 "미완료 (체크된 개수/전체 개수)"
 *
 *   <필드>
 *   1. settlementRepository : 정산 결과(및 완료 체크 상태) CSV를 읽는 저장소 객체
 *   2. memberRepository     : 참여자 닉네임을 보여주기 위한 저장소 객체
 *   3. room                  : 지금 보고 있는 방
 *   4. loginMember           : 지금 로그인한 사용자
 *      (본인의 정산 페이지를 열었을 때만 체크박스를 누를 수 있도록 MySettlementDialog에 전달합니다)
 *
 *   <중요 메소드>
 *   1. createMemberRow(memberId) : 참여자 한 명의 "닉네임 + 완료 상태" 한 줄을 만듦
 *   2. handleSelectMember(memberId) : 참여자 이름 클릭 -> 그 사람의 MySettlementDialog를 염
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.model.SettlementItem;
import com.groupmeeting.util.MemberRepository;
import com.groupmeeting.util.SettlementRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SettlementListDialog extends JDialog {

    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final Room room;
    private final Member loginMember;

    public SettlementListDialog(Window owner, SettlementRepository settlementRepository,
                                 MemberRepository memberRepository, Room room, Member loginMember) {
        super(owner, "정산 결과", ModalityType.APPLICATION_MODAL);
        this.settlementRepository = settlementRepository;
        this.memberRepository = memberRepository;
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

    /** 화면 내부 컴포넌트(제목, 참여자 목록, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("참여자를 선택해주세요");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("각자 얼마를 주고받아야 하는지, 정산을 완료했는지 확인할 수 있습니다.");
        subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        subLabel.setForeground(new Color(0x99, 0x99, 0x99));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 참여자 목록 패널을 먼저 내용으로 채운 뒤(render-before-freeze), 이름/상태 글자가
        // 길어도 잘리지 않도록 가로 스크롤이 가능한 영역으로 감쌉니다.
        JPanel membersPanel = new JPanel();
        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
        membersPanel.setOpaque(false);
        membersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<SettlementItem> allItems = settlementRepository.getForRoom(room.getCode());
        for (String memberId : room.getMemberIds()) {
            membersPanel.add(createMemberRow(memberId, allItems));
            membersPanel.add(Box.createVerticalStrut(8));
        }
        JScrollPane membersScrollPane = Theme.wrapHorizontalScrollable(membersPanel, Theme.STANDARD_CONTENT_WIDTH);

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        closeButton.addActionListener(e -> dispose());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(subLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(membersScrollPane);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /**
     * 참여자 한 명의 "닉네임 + 완료 상태"를 보여주는 클릭 가능한 행을 만듭니다.
     * 완료 상태는 이 사람과 관련된 정산 항목(SettlementItem)들을 모아서 계산합니다.
     * - 관련 항목이 없으면 "해당없음"
     * - 전부 체크되어 있으면 "완료"
     * - 일부만 체크되어 있으면 "미완료 (체크된 개수/전체 개수)"
     */
    private JPanel createMemberRow(String memberId, List<SettlementItem> allItems) {
        int totalCount = 0;
        int confirmedCount = 0;
        for (SettlementItem item : allItems) {
            if (item.involves(memberId)) {
                totalCount++;
                if (item.isConfirmed()) {
                    confirmedCount++;
                }
            }
        }

        String statusText;
        Color statusColor;
        if (totalCount == 0) {
            statusText = "해당없음";
            statusColor = new Color(0x99, 0x99, 0x99);
        } else if (confirmedCount == totalCount) {
            statusText = "완료";
            statusColor = Theme.PRIMARY_GREEN_DARK;
        } else {
            statusText = "미완료 (" + confirmedCount + "/" + totalCount + ")";
            statusColor = new Color(0xE5, 0x39, 0x35);
        }

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        String nickname = findNickname(memberId);
        boolean isMe = memberId.equals(loginMember.getId());

        JLabel nameLabel = new JLabel(nickname + (isMe ? " (나)" : ""));
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setForeground(Theme.TEXT_DARK);

        // 오른쪽에 "완료 상태 + 화살표"를 함께 보여줍니다.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        statusLabel.setForeground(statusColor);

        JLabel arrowLabel = new JLabel("  ›");
        arrowLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        arrowLabel.setForeground(new Color(0x99, 0x99, 0x99));

        rightPanel.add(statusLabel);
        rightPanel.add(arrowLabel);

        row.add(nameLabel, BorderLayout.CENTER);
        row.add(rightPanel, BorderLayout.EAST);

        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleSelectMember(memberId);
            }
        });

        return row;
    }

    /** 참여자 이름 클릭 시 실행됩니다. 그 사람의 정산 내역 화면을 엽니다. */
    private void handleSelectMember(String memberId) {
        MySettlementDialog dialog =
                new MySettlementDialog(this, settlementRepository, memberRepository, room, memberId, loginMember);
        dialog.setVisible(true);
    }

    private String findNickname(String memberId) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(memberId)) {
                return m.getNickname();
            }
        }
        return memberId;
    }
}
