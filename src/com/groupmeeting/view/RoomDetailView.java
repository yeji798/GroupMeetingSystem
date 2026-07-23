package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.util.AvailabilityRepository;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 방에 입장했을 때 보여지는 방 메인 화면입니다.
 *
 * 구성 요소:
 *  - 상단: 뒤로가기, 방 이름 + 인원수 + 카테고리, 마이페이지 버튼
 *  - "명단확인" 버튼
 *  - "날짜 및 시간" / "장소" / "예산" 섹션 (버튼들)
 *  - 하단 "다이어리" 안내 패널
 *
 * 이번 구현 범위에서는 "날짜 및 시간 선택"(ScheduleInputDialog 연동, CSV 저장)과
 * "장소 확인"(제출된 장소 목록 조회)만 실제로 동작하며, 그 외 버튼(투표, 랜덤 장소 추첨,
 * 예산, 다이어리)은 화면/버튼만 배치하고 클릭 시 "준비 중" 안내를 표시합니다.
 */
public class RoomDetailView extends JFrame {

    private final MainView owner;
    private final Room room;
    private final Member loginMember;

    private final MemberRepository memberRepository = new MemberRepository();
    private final AvailabilityRepository availabilityRepository = new AvailabilityRepository();

    public RoomDetailView(MainView owner, Room room, Member loginMember) {
        this.owner = owner;
        this.room = room;
        this.loginMember = loginMember;
        initFrame();
        initComponents();
    }

    private void initFrame() {
        setTitle(room.getName());
        setSize(495, 880);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(Theme.BACKGROUND);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleBack();
            }
        });
    }

    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 20, 18));

        root.add(buildTopBar());
        root.add(Box.createVerticalStrut(10));
        root.add(buildMemberListRow());
        root.add(Box.createVerticalStrut(16));
        root.add(buildSectionsCard());
        root.add(Box.createVerticalStrut(20));
        root.add(buildDiaryPanel());

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);

        setContentPane(scrollPane);
    }

    /** 상단바: 뒤로가기 + 방 이름/인원수/카테고리 + 마이페이지 버튼 */
    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JButton backButton = new JButton("←");
        backButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        backButton.setForeground(Theme.TEXT_DARK);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> handleBack());

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);

        JLabel houseIcon = new JLabel("🏠"); // 🏠
        houseIcon.setFont(new Font("맑은 고딕", Font.PLAIN, 18));

        JLabel nameLabel = new JLabel(room.getName());
        nameLabel.setFont(Theme.FONT_SUBTITLE);
        nameLabel.setForeground(Theme.TEXT_DARK);

        JLabel countBadge = new JLabel(String.valueOf(room.getMemberCount()), SwingConstants.CENTER);
        countBadge.setOpaque(true);
        countBadge.setBackground(Theme.ACCENT_GREEN);
        countBadge.setForeground(Theme.PRIMARY_GREEN_DARK);
        countBadge.setFont(new Font("맑은 고딕", Font.BOLD, 11));
        countBadge.setPreferredSize(new Dimension(20, 20));

        nameRow.add(houseIcon);
        nameRow.add(nameLabel);
        nameRow.add(countBadge);

        JLabel categoryLabel = new JLabel(room.getCategory() + " · " + room.getMemberCount() + "/" + Room.MAX_MEMBERS + "명");
        categoryLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(0x99, 0x99, 0x99));
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));

        infoPanel.add(nameRow);
        infoPanel.add(categoryLabel);

        JButton profileButton = new JButton("👤"); // 👤
        profileButton.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        profileButton.setBorderPainted(false);
        profileButton.setContentAreaFilled(false);
        profileButton.setFocusPainted(false);
        profileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileButton.addActionListener(e -> handleOpenProfile());

        topBar.add(backButton, BorderLayout.WEST);
        topBar.add(infoPanel, BorderLayout.CENTER);
        topBar.add(profileButton, BorderLayout.EAST);
        return topBar;
    }

    /** "명단확인" 버튼 행 (우측 정렬) */
    private JPanel buildMemberListRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);

        JButton memberListButton = new JButton("👥  명단확인"); // 👥
        Theme.styleSecondaryButton(memberListButton);
        memberListButton.addActionListener(e -> handleShowMemberList());

        row.add(memberListButton);
        return row;
    }

    /** "날짜 및 시간" / "장소" / "예산" 섹션들을 담은 카드 */
    private JPanel buildSectionsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(buildSection("📅", "날짜 및 시간", "날짜와 시간을 선택해 주세요.",
                button("날짜 및 시간 선택", true, e -> handleOpenSchedule()),
                button("수정", false, e -> handleOpenSchedule())
        ));
        card.add(Box.createVerticalStrut(14));
        card.add(separator());
        card.add(Box.createVerticalStrut(14));

        card.add(buildSection("📍", "장소", "장소를 확인하고 투표해주세요.",
                button("장소 확인", true, e -> handleCheckPlaces()),
                button("투표", false, e -> handleNotReady()),
                button("수정", false, e -> handleNotReady()),
                button("랜덤 장소 추첨", false, e -> handleNotReady())
        ));
        card.add(Box.createVerticalStrut(14));
        card.add(separator());
        card.add(Box.createVerticalStrut(14));

        card.add(buildSection("💰", "예산", "비용을 입력하고 확인해주세요.",
                button("입력", false, e -> handleNotReady()),
                button("확인", false, e -> handleNotReady())
        ));

        return card;
    }

    private JPanel buildSection(String emoji, String title, String desc, JButton... buttons) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.TEXT_DARK);

        titleRow.add(emojiLabel);
        titleRow.add(titleLabel);

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        descLabel.setForeground(new Color(0x99, 0x99, 0x99));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(2, 24, 8, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton b : buttons) {
            buttonRow.add(b);
        }

        section.add(titleRow);
        section.add(descLabel);
        section.add(buttonRow);
        return section;
    }

    private JButton button(String text, boolean primary, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        if (primary) {
            Theme.styleButton(b);
        } else {
            Theme.styleSecondaryButton(b);
        }
        b.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        b.addActionListener(listener);
        return b;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER_GREEN);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    /** 하단 "다이어리" 안내 패널 */
    private JPanel buildDiaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.ACCENT_GREEN);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("다이어리");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("모임과 관련된 내용을 기록해보세요.");
        descLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        descLabel.setForeground(Theme.TEXT_DARK);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));

        JButton enterButton = new JButton("입장하기  ›");
        Theme.styleSecondaryButton(enterButton);
        enterButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        enterButton.addActionListener(e -> handleNotReady());

        panel.add(titleLabel);
        panel.add(descLabel);
        panel.add(enterButton);
        return panel;
    }

    // ---------------- 이벤트 핸들러 ----------------

    /** 뒤로가기: 이 창을 닫고 방 목록 화면(MainView)으로 돌아갑니다. */
    private void handleBack() {
        dispose();
        owner.refreshAndShow();
    }

    private void handleOpenProfile() {
        String info = "이름: " + loginMember.getName() + "\n"
                + "닉네임: " + loginMember.getNickname() + "\n"
                + "아이디: " + loginMember.getId() + "\n"
                + "이메일: " + loginMember.getEmail() + "\n\n"
                + "(회원 정보 수정 기능은 추후 지원될 예정입니다.)";

        JOptionPane.showMessageDialog(this, info, "마이페이지", JOptionPane.INFORMATION_MESSAGE);
    }

    /** "명단확인" 버튼: 현재 참여자들의 닉네임 목록을 보여줍니다. */
    private void handleShowMemberList() {
        List<String> memberIds = room.getMemberIds();
        StringBuilder sb = new StringBuilder();
        sb.append("참여자 (").append(memberIds.size()).append("/").append(Room.MAX_MEMBERS).append("명)\n\n");

        for (String id : memberIds) {
            Member member = findMemberById(id);
            String nickname = (member != null) ? member.getNickname() : id;
            sb.append("- ").append(nickname);
            if (id.equals(room.getOwnerId())) {
                sb.append(" (방장)");
            }
            sb.append("\n");
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "명단확인", JOptionPane.INFORMATION_MESSAGE);
    }

    /** "날짜 및 시간 선택" / "수정" 버튼: ScheduleInputDialog를 열어 CSV에 저장합니다. */
    private void handleOpenSchedule() {
        ScheduleInputDialog dialog = new ScheduleInputDialog(this, availabilityRepository, room, loginMember.getId());
        dialog.setVisible(true);
    }

    /** "장소 확인" 버튼: 지금까지 제출된 참여자별 추천 장소를 보여줍니다. */
    private void handleCheckPlaces() {
        Map<String, String> placesByMember = availabilityRepository.getLatestPlaceByMember(room.getCode());

        if (placesByMember.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "아직 제출된 장소가 없습니다.",
                    "장소 확인", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : placesByMember.entrySet()) {
            Member member = findMemberById(entry.getKey());
            String nickname = (member != null) ? member.getNickname() : entry.getKey();
            sb.append("- ").append(nickname).append(": ").append(entry.getValue()).append("\n");
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "장소 확인", JOptionPane.INFORMATION_MESSAGE);
    }

    /** 아직 구현되지 않은 기능(투표, 랜덤 장소 추첨, 예산, 다이어리) 공통 안내 */
    private void handleNotReady() {
        JOptionPane.showMessageDialog(this,
                "이 기능은 추후 지원될 예정입니다.",
                "준비 중", JOptionPane.INFORMATION_MESSAGE);
    }

    private Member findMemberById(String id) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }
}
