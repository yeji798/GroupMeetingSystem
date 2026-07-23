/**
 * MemberListDialog.java
 *
 * 방 메인 화면의 "명단확인" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 참여자들의 닉네임을 목록으로 보여주고, 방장(room owner)이 보고 있을 때만 각 참여자 옆에
 * "강퇴" 버튼이 나타나서 다른 사람을 방에서 내보낼 수 있습니다. (방장 자기 자신은 강퇴 불가)
 *
 *   <필드>
 *   1. roomRepository   : 강퇴(=방 나가기와 같은 방식) 처리를 위한 저장소 객체
 *   2. memberRepository : 참여자 닉네임을 조회하기 위한 저장소 객체
 *   3. room               : 지금 보고 있는 방 (강퇴 시 이 객체의 참여자 목록도 함께 갱신됨)
 *   4. loginMember        : 지금 로그인한 사용자 (방장인지 확인하는 데 사용)
 *
 *   <중요 메소드>
 *   1. handleKick(memberId) : "강퇴" 버튼 -> 확인 후 이 방의 참여자 목록에서 제거
 *   2. renderMembers()       : 지금 room.getMemberIds() 기준으로 목록을 다시 그림
 */

package com.groupmeeting.view.room;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.RoomRepository;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MemberListDialog extends JDialog {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final Room room;
    private final Member loginMember;

    private JPanel membersPanel;

    public MemberListDialog(Window owner, RoomRepository roomRepository, MemberRepository memberRepository,
                             Room room, Member loginMember) {
        super(owner, "명단확인", ModalityType.APPLICATION_MODAL);
        this.roomRepository = roomRepository;
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

        JLabel titleLabel = new JLabel("참여자 (" + room.getMemberCount() + "명)");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        membersPanel = new JPanel();
        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
        membersPanel.setOpaque(false);
        membersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        closeButton.addActionListener(e -> dispose());

        // 참여자 목록을 먼저 채운 다음에 가로 스크롤 영역으로 감쌉니다. (닉네임이 길어서
        // 화면 폭보다 넓어지면 옆으로 스크롤해서 볼 수 있도록 함 - 잘려서 안 보이는 문제 방지)
        renderMembers();
        JScrollPane membersScroll = Theme.wrapHorizontalScrollable(membersPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(membersScroll);
        root.add(Box.createVerticalStrut(16));
        root.add(closeButton);

        Theme.alignAsCenteredColumn(membersScroll, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** room.getMemberIds()를 다시 읽어와 참여자 목록을 새로 그립니다. */
    private void renderMembers() {
        membersPanel.removeAll();

        // 강퇴 도중 목록이 바뀔 수 있으므로 복사본을 순회합니다.
        List<String> memberIds = new ArrayList<>(room.getMemberIds());
        boolean iAmOwner = loginMember.getId().equals(room.getOwnerId());

        for (String memberId : memberIds) {
            membersPanel.add(createMemberRow(memberId, iAmOwner));
            membersPanel.add(Box.createVerticalStrut(8));
        }

        membersPanel.revalidate();
        membersPanel.repaint();
    }

    /** 참여자 한 명의 행(닉네임 + 방장 표시 + (방장이 보는 경우) 강퇴 버튼)을 만듭니다. */
    private JPanel createMemberRow(String memberId, boolean iAmOwner) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isOwnerRow = memberId.equals(room.getOwnerId());
        String nickname = findNickname(memberId);

        JLabel nameLabel = new JLabel(nickname + (isOwnerRow ? " (방장)" : ""));
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setForeground(Theme.TEXT_DARK);
        row.add(nameLabel, BorderLayout.CENTER);

        // 방장이 이 화면을 보고 있고, 이 행이 방장 자신이 아닐 때만 "강퇴" 버튼을 보여줍니다.
        if (iAmOwner && !isOwnerRow) {
            JButton kickButton = new JButton("강퇴");
            kickButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            kickButton.setForeground(new Color(0xE5, 0x39, 0x35));
            kickButton.setBorderPainted(false);
            kickButton.setContentAreaFilled(false);
            kickButton.setFocusPainted(false);
            kickButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            kickButton.addActionListener(e -> handleKick(memberId, nickname));
            row.add(kickButton, BorderLayout.EAST);
        }

        return row;
    }

    /** "강퇴" 버튼 클릭 시 실행됩니다. 확인 후 이 회원을 방 참여자 목록에서 제거합니다. */
    private void handleKick(String memberId, String nickname) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "'" + nickname + "'님을 이 방에서 강퇴하시겠습니까?",
                "강퇴 확인", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        roomRepository.leaveRoom(room.getCode(), memberId); // CSV에 반영
        room.getMemberIds().remove(memberId); // 지금 화면이 들고 있는 Room 객체에도 즉시 반영

        renderMembers();
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
