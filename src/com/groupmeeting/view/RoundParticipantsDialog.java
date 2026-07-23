/**
 * RoundParticipantsDialog.java
 *
 * RoundListDialog에서 차수 이름을 클릭했을 때 뜨는 화면입니다.
 * 그 차수 모임에 "참여"를 확정한 사람들의 닉네임을 목록으로 보여줍니다.
 *
 *   <필드>
 *   1. participantRepository : 차수별 참여자 기록을 읽는 저장소 객체
 *   2. memberRepository       : 참여자 닉네임을 조회하기 위한 저장소 객체
 *   3. round                   : 지금 보고 있는 차수 모임
 */

package com.groupmeeting.view;

import com.groupmeeting.model.MeetingRound;
import com.groupmeeting.model.Member;
import com.groupmeeting.util.MemberRepository;
import com.groupmeeting.util.RoundParticipantRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RoundParticipantsDialog extends JDialog {

    private final RoundParticipantRepository participantRepository;
    private final MemberRepository memberRepository;
    private final MeetingRound round;

    public RoundParticipantsDialog(Window owner, RoundParticipantRepository participantRepository,
                                    MemberRepository memberRepository, MeetingRound round) {
        super(owner, "참여자 명단", ModalityType.APPLICATION_MODAL);
        this.participantRepository = participantRepository;
        this.memberRepository = memberRepository;
        this.round = round;
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

    /** 화면 내부 컴포넌트(제목, 참여자 명단, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(round.getName() + " · 참여자 명단");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        List<String> participantIds = participantRepository.getParticipantIds(round.getId());
        if (participantIds.isEmpty()) {
            JLabel emptyLabel = new JLabel("아직 참여를 확정한 사람이 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            listPanel.add(emptyLabel);
        } else {
            for (String memberId : participantIds) {
                JLabel nameLabel = new JLabel("- " + findNickname(memberId));
                nameLabel.setFont(Theme.FONT_NORMAL);
                nameLabel.setForeground(Theme.TEXT_DARK);
                nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(nameLabel);
                listPanel.add(Box.createVerticalStrut(6));
            }
        }

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        // 닉네임이 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        JScrollPane listScroll = Theme.wrapHorizontalScrollable(listPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(listScroll);
        root.add(Box.createVerticalStrut(20));
        root.add(closeButton);

        Theme.alignAsCenteredColumn(listScroll, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
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
