/**
 * ParticipantScheduleDialog.java
 *
 * "참여자 일정 보기" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 방에 참여 중인 사람들을 한 명씩 순서대로 보여주면서, 각자 CSV에 제출해둔
 * "가능한 날짜 + 시작~끝 시간" 목록을 그 사람 이름 아래에 정리해서 보여줍니다.
 * 아직 아무것도 입력하지 않은 참여자는 "아직 입력하지 않음"이라고 표시합니다.
 *
 *   <필드>
 *   1. repository       : 가능 시간 CSV를 읽는 저장소 객체
 *   2. memberRepository : 회원 닉네임을 조회하는 저장소 객체
 *   3. room              : 지금 보고 있는 방 정보 (참여자 목록을 여기서 가져옴)
 *
 *   <생성자>
 *   : 창을 생성하고, 방의 참여자를 한 명씩 순회하며 각자의 일정 카드를 만들어 화면에 표시함
 *
 *   <중요 메소드>
 *   1. buildMemberCard(memberId) : 참여자 한 명의 이름 + 일정 목록을 카드 하나로 만듦
 */

package com.groupmeeting.view;

import com.groupmeeting.model.AvailabilityEntry;
import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.util.AvailabilityRepository;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ParticipantScheduleDialog extends JDialog {

    private final AvailabilityRepository repository;
    private final MemberRepository memberRepository;
    private final Room room;

    public ParticipantScheduleDialog(Window owner, AvailabilityRepository repository,
                                      MemberRepository memberRepository, Room room) {
        super(owner, "참여자 일정 보기", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.room = room;
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

    /** 화면 내부 컴포넌트를 배치합니다. 참여자 수만큼 카드를 만들어 세로로 쌓습니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("참여자별 가능 시간");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 참여자 카드들을 담을 별도 패널을 만들고, 방에 있는 참여자를 한 명씩 순서대로 카드로
        // 만들어 추가한다. (아직 시간을 입력하지 않은 사람도 포함해서, 누가 안 했는지도 알 수 있도록 함)
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);
        for (String memberId : room.getMemberIds()) {
            cardsPanel.add(buildMemberCard(memberId));
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        // 날짜/시간 글자가 길어서 화면 폭을 넘어가도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        JScrollPane cardsScroll = Theme.wrapHorizontalScrollable(cardsPanel, Theme.STANDARD_CONTENT_WIDTH);

        JButton closeButton = new JButton("닫기");
        Theme.styleSecondaryButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(cardsScroll);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        Theme.alignAsCenteredColumn(cardsScroll, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /**
     * 참여자 한 명의 카드(이름 + 방장 표시 + 가능 시간 목록)를 만듭니다.
     * 이 사람이 아직 아무것도 입력하지 않았다면 "아직 입력하지 않음"이라고 안내합니다.
     */
    private JPanel buildMemberCard(String memberId) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 아이디로 닉네임을 찾아온다. (탈퇴 등으로 못 찾으면 아이디를 그대로 보여줌)
        Member member = findMemberById(memberId);
        String nickname = (member != null) ? member.getNickname() : memberId;

        JLabel nameLabel = new JLabel(nickname + (memberId.equals(room.getOwnerId()) ? " (방장)" : ""));
        nameLabel.setFont(Theme.FONT_SUBTITLE);
        nameLabel.setForeground(Theme.TEXT_DARK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));

        // 이 회원이 이 방에 제출한 가능 시간 항목들을 불러온다.
        List<AvailabilityEntry> entries = repository.getForRoomAndMember(room.getCode(), memberId);

        if (entries.isEmpty()) {
            JLabel emptyLabel = new JLabel("아직 입력하지 않음");
            emptyLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(emptyLabel);
            return card;
        }

        // 날짜 -> 시작 시간 순으로 정렬해서 보기 편하게 만든다.
        entries.sort(Comparator
                .comparing(AvailabilityEntry::getDate)
                .thenComparing(AvailabilityEntry::getStartTime));

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);

        for (AvailabilityEntry entry : entries) {
            LocalDate date = LocalDate.parse(entry.getDate());
            String line = date.format(dateFormat) + "  " + entry.getStartTime() + " ~ " + entry.getEndTime();

            JLabel entryLabel = new JLabel(line);
            entryLabel.setFont(Theme.FONT_NORMAL);
            entryLabel.setForeground(Theme.TEXT_DARK);
            entryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(entryLabel);
        }

        return card;
    }

    /** 회원 아이디로 전체 회원 목록에서 일치하는 회원을 찾아 반환합니다. 없으면 null. */
    private Member findMemberById(String id) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }
}
