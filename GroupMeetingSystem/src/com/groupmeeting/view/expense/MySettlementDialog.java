/**
 * MySettlementDialog.java
 *
 * SettlementListDialog에서 참여자 한 명을 클릭했을 때 뜨는 화면입니다.
 * 맨 위에 그 사람의 이름이 적혀 있고, 그 아래에 "OOO님에게 7,000원 주기" / "OOO님에게 1,000원 받기"
 * 형태로 정산 내역이 하나씩 나열됩니다. 주고받을 내역이 하나도 없으면 "해당없음"이라고 표시합니다.
 *
 * 입금(또는 수금)을 실제로 완료했다면 체크박스를 눌러서 "정산 완료"로 표시할 수 있습니다.
 * -> 요구사항: "각각의 참여자들이 자신의 정산 페이지에서" 체크할 수 있어야 하므로, 지금 로그인한
 *    사람(loginMember)이 보고 있는 화면의 주인(viewedMemberId)과 같을 때만 체크박스를 누를 수 있게
 *    하고, 다른 사람의 정산 페이지를 구경만 할 때는 체크박스를 눌러도 반응하지 않도록(비활성화) 합니다.
 *
 *   <필드>
 *   1. settlementRepository : 정산 결과를 읽고, 체크 상태를 저장하는 저장소 객체
 *   2. memberRepository     : 닉네임을 보여주기 위한 저장소 객체
 *   3. room                  : 지금 보고 있는 방
 *   4. viewedMemberId        : 지금 이 화면에 정산 내역이 표시되고 있는 사람의 아이디
 *   5. isEditable            : viewedMemberId가 로그인한 사용자 본인일 때만 true (체크박스 활성화 여부)
 *
 *   <중요 메소드>
 *   1. renderItems() : 정산 내역을 다시 불러와 "주기"/"받기" 줄과 체크박스를 화면에 그림
 */

package com.groupmeeting.view.expense;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.model.SettlementItem;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.SettlementRepository;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MySettlementDialog extends JDialog {

    private final SettlementRepository settlementRepository;
    private final MemberRepository memberRepository;
    private final Room room;
    private final String viewedMemberId;
    private final boolean isEditable; // 로그인한 본인의 정산 페이지를 보고 있을 때만 true

    private JPanel itemsPanel;
    private JScrollPane itemsScrollPane; // itemsPanel을 감싸는 가로 스크롤 영역

    public MySettlementDialog(Window owner, SettlementRepository settlementRepository, MemberRepository memberRepository,
                               Room room, String viewedMemberId, Member loginMember) {
        super(owner, "정산 내역", ModalityType.APPLICATION_MODAL);
        this.settlementRepository = settlementRepository;
        this.memberRepository = memberRepository;
        this.room = room;
        this.viewedMemberId = viewedMemberId;
        this.isEditable = viewedMemberId.equals(loginMember.getId());
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

    /** 화면 내부 컴포넌트(이름, 정산 내역 목록, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String myNickname = findNickname(viewedMemberId);

        JLabel nameLabel = new JLabel(myNickname + "님의 정산 내역");
        nameLabel.setFont(Theme.FONT_SUBTITLE);
        nameLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel(isEditable
                ? "입금(또는 수금)을 완료했으면 체크해주세요."
                : "다른 참여자의 정산 내역입니다. (체크는 본인만 가능)");
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        // 정산 내역을 먼저 채운 뒤, 이름/금액 글자가 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        renderItems();
        itemsScrollPane = Theme.wrapHorizontalScrollable(itemsPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(nameLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(hintLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(itemsScrollPane);
        root.add(Box.createVerticalStrut(20));
        root.add(closeButton);

        Theme.alignAsCenteredColumn(itemsScrollPane, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** 이 사람과 관련된 정산 항목들을 다시 불러와서 "주기"/"받기" 줄로 화면에 그립니다. */
    private void renderItems() {
        itemsPanel.removeAll();

        List<SettlementItem> all = settlementRepository.getForRoom(room.getCode());
        boolean hasAny = false;

        for (SettlementItem item : all) {
            if (item.getFromMemberId().equals(viewedMemberId)) {
                // 이 사람이 돈을 "줘야" 하는 경우
                String toNickname = findNickname(item.getToMemberId());
                String text = toNickname + "님에게 " + formatWon(item.getAmount()) + " 주기";
                itemsPanel.add(createItemRow(text, item));
                itemsPanel.add(Box.createVerticalStrut(6));
                hasAny = true;
            } else if (item.getToMemberId().equals(viewedMemberId)) {
                // 이 사람이 돈을 "받아야" 하는 경우
                String fromNickname = findNickname(item.getFromMemberId());
                String text = fromNickname + "님에게 " + formatWon(item.getAmount()) + " 받기";
                itemsPanel.add(createItemRow(text, item));
                itemsPanel.add(Box.createVerticalStrut(6));
                hasAny = true;
            }
        }

        if (!hasAny) {
            JLabel noneLabel = new JLabel("해당없음");
            noneLabel.setFont(Theme.FONT_NORMAL);
            noneLabel.setForeground(new Color(0x99, 0x99, 0x99));
            noneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemsPanel.add(noneLabel);
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();

        // 이미 가로 스크롤 영역으로 감싼 뒤(=체크박스를 눌러서 다시 그리는 경우)라면,
        // 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (itemsScrollPane != null) {
            Theme.resyncScrollableHeight(itemsScrollPane, itemsPanel);
            revalidate();
            repaint();
        }
    }

    /** 정산 항목 한 줄(설명 텍스트 + 완료 체크박스)을 만듭니다. */
    private JPanel createItemRow(String text, SettlementItem item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(item.isConfirmed() ? Theme.ACCENT_GREEN : Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(Theme.FONT_NORMAL);
        textLabel.setForeground(Theme.TEXT_DARK);

        JCheckBox checkBox = new JCheckBox("정산 완료");
        checkBox.setOpaque(false);
        checkBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        checkBox.setSelected(item.isConfirmed());
        checkBox.setEnabled(isEditable); // 본인 페이지가 아니면 체크할 수 없도록 막음
        checkBox.addActionListener(e -> {
            settlementRepository.setConfirmed(
                    item.getRoomCode(), item.getFromMemberId(), item.getToMemberId(), checkBox.isSelected());
            renderItems(); // 색상(완료 표시) 갱신을 위해 다시 그림
        });

        row.add(textLabel, BorderLayout.CENTER);
        row.add(checkBox, BorderLayout.EAST);
        return row;
    }

    private String formatWon(long amount) {
        return NumberFormat.getInstance(Locale.KOREA).format(amount) + "원";
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
