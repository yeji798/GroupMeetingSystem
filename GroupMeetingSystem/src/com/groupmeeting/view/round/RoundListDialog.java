/**
 * RoundListDialog.java
 *
 * 방 메인 화면의 "차수별 인원조사" 버튼을 눌렀을 때 뜨는 화면입니다. (단체 약속 방 전용)
 * 이 방에 만들어진 모임 차수(예: "1차 모임", "2차 모임", "뒤풀이")들을 목록으로 보여줍니다.
 *
 *   - 각 차수 행에는: 차수 이름(클릭하면 그 차수의 참여자 명단을 보여줌),
 *                      내가 그 차수에 참여 중인지 표시("참여중"/"불참"),
 *                      아직 참여하지 않았다면 "참여" 버튼이 나타납니다.
 *   - "참여" 버튼을 누르면 되돌릴 수 없다는 확인을 거친 뒤 참여자로 등록됩니다.
 *   - 방장이라면 맨 아래에 "모임 차수 추가" 버튼이 나타나서 새 차수를 만들 수 있습니다.
 *
 *   <필드>
 *   1. roundRepository      : 모임 차수 목록을 읽고 쓰는 저장소 객체
 *   2. participantRepository : 차수별 참여자 기록을 읽고 쓰는 저장소 객체
 *   3. memberRepository      : 닉네임 조회 및 참여자 명단 화면에 사용
 *   4. room                   : 지금 보고 있는 방
 *   5. loginMember            : 지금 로그인한 사용자 (참여 확정 대상 + 방장 여부 확인)
 *
 *   <중요 메소드>
 *   1. handleSelectRound() : 차수 이름 클릭 -> RoundParticipantsDialog(참여자 명단)를 염
 *   2. handleJoinRound()   : "참여" 버튼 -> 확인 후 되돌릴 수 없는 참여 확정 처리
 *   3. handleAddRound()    : "모임 차수 추가" 버튼(방장 전용) -> AddRoundDialog를 열고, 추가되면 새로고침
 */

package com.groupmeeting.view.round;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.MeetingRound;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.MeetingRoundRepository;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.RoundParticipantRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RoundListDialog extends JDialog {

    private final MeetingRoundRepository roundRepository;
    private final RoundParticipantRepository participantRepository;
    private final MemberRepository memberRepository;
    private final Room room;
    private final Member loginMember;

    private JPanel listPanel;
    private JScrollPane listScrollPane; // listPanel을 감싸는 가로 스크롤 영역
    private JButton addRoundButton; // 방장 전용, 폭 계산 시 같이 정렬해야 하므로 필드로 둠

    public RoundListDialog(Window owner, MeetingRoundRepository roundRepository, RoundParticipantRepository participantRepository,
                            MemberRepository memberRepository, Room room, Member loginMember) {
        super(owner, "차수별 인원조사", ModalityType.APPLICATION_MODAL);
        this.roundRepository = roundRepository;
        this.participantRepository = participantRepository;
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

    /** 화면 내부 컴포넌트(제목, 차수 목록, (방장이면) 차수 추가 버튼, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 차수별 인원조사");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        boolean iAmOwner = loginMember.getId().equals(room.getOwnerId());

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        // 차수 목록을 먼저 채운 뒤, 차수 이름이 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        renderRounds(iAmOwner);
        listScrollPane = Theme.wrapHorizontalScrollable(listPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(listScrollPane);
        root.add(Box.createVerticalStrut(16));

        if (iAmOwner) {
            addRoundButton = new JButton("모임 차수 추가");
            Theme.styleSecondaryButton(addRoundButton);
            addRoundButton.addActionListener(e -> handleAddRound());
            root.add(addRoundButton);
            root.add(Box.createVerticalStrut(10));
        }

        root.add(closeButton);

        if (iAmOwner) {
            Theme.alignAsCenteredColumn(listScrollPane, addRoundButton, closeButton);
        } else {
            Theme.alignAsCenteredColumn(listScrollPane, closeButton);
        }

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** 이 방의 차수 목록을 다시 불러와서 화면에 그립니다. */
    private void renderRounds(boolean iAmOwner) {
        listPanel.removeAll();

        List<MeetingRound> rounds = roundRepository.getForRoom(room.getCode());

        if (rounds.isEmpty()) {
            JLabel emptyLabel = new JLabel(iAmOwner
                    ? "아직 만들어진 차수가 없습니다. 아래 '모임 차수 추가'로 만들어보세요."
                    : "아직 만들어진 차수가 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            listPanel.add(emptyLabel);
        } else {
            for (MeetingRound round : rounds) {
                listPanel.add(createRoundRow(round));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();

        // 이미 가로 스크롤 영역으로 감싼 뒤(=차수를 추가하거나 참여한 뒤 다시 그리는 경우)라면,
        // 새로 바뀐 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (listScrollPane != null) {
            Theme.resyncScrollableHeight(listScrollPane, listPanel);
            revalidate();
            repaint();
        }
    }

    /** 차수 하나의 행(이름 + 내 참여 상태 + 참여 버튼)을 만듭니다. */
    private JPanel createRoundRow(MeetingRound round) {
        boolean iAmParticipating = participantRepository.isParticipating(round.getId(), loginMember.getId());

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // 왼쪽: 차수 이름 (클릭하면 참여자 명단을 보여줌) + 내 참여 상태
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(round.getName());
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setForeground(Theme.TEXT_DARK);

        JLabel statusLabel = new JLabel(iAmParticipating ? "참여중" : "불참");
        statusLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        statusLabel.setForeground(iAmParticipating ? Theme.PRIMARY_GREEN_DARK : new Color(0x99, 0x99, 0x99));

        leftPanel.add(nameLabel);
        leftPanel.add(Box.createHorizontalStrut(8));
        leftPanel.add(statusLabel);
        leftPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleSelectRound(round);
            }
        });

        row.add(leftPanel, BorderLayout.CENTER);

        // 오른쪽: 아직 참여하지 않았을 때만 "참여" 버튼을 보여줌 (참여는 되돌릴 수 없으므로)
        if (!iAmParticipating) {
            JButton joinButton = new JButton("참여");
            Theme.styleSecondaryButton(joinButton);
            joinButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            joinButton.addActionListener(e -> handleJoinRound(round));
            row.add(joinButton, BorderLayout.EAST);
        }

        return row;
    }

    /** 차수 이름 클릭 시 실행됩니다. 그 차수의 참여자 명단 화면을 엽니다. */
    private void handleSelectRound(MeetingRound round) {
        RoundParticipantsDialog dialog =
                new RoundParticipantsDialog(this, participantRepository, memberRepository, round);
        dialog.setVisible(true);
    }

    /** "참여" 버튼 클릭 시 실행됩니다. 되돌릴 수 없다는 확인을 거친 뒤 참여자로 등록합니다. */
    private void handleJoinRound(MeetingRound round) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "'" + round.getName() + "'에 참여하시겠습니까?\n한 번 참여하면 다시 수정할 수 없습니다.",
                "참여 확정", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            participantRepository.addParticipant(room.getCode(), round.getId(), loginMember.getId());
            renderRounds(loginMember.getId().equals(room.getOwnerId()));
        }
    }

    /** "모임 차수 추가" 버튼(방장 전용) 클릭 시 실행됩니다. */
    private void handleAddRound() {
        AddRoundDialog dialog = new AddRoundDialog(this, roundRepository, room);
        dialog.setVisible(true);

        if (dialog.isAdded()) {
            renderRounds(true);
        }
    }
}
