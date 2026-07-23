package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.model.RoomFinalDecision;
import com.groupmeeting.util.AvailabilityRepository;
import com.groupmeeting.util.ExpenseRepository;
import com.groupmeeting.util.MeetingRoundRepository;
import com.groupmeeting.util.MemberRepository;
import com.groupmeeting.util.PlaceRepository;
import com.groupmeeting.util.PlaceVoteRepository;
import com.groupmeeting.util.RoomFinalRepository;
import com.groupmeeting.util.RoomRepository;
import com.groupmeeting.util.RoundParticipantRepository;
import com.groupmeeting.util.SettlementRepository;
import com.groupmeeting.util.TravelDateRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 방에 입장했을 때 보여지는 방 메인 화면입니다.
 *
 * 구성 요소:
 *  - 상단: 뒤로가기, 방 이름 + 인원수 + 카테고리, 마이페이지 버튼
 *  - "명단확인" 버튼
 *  - "날짜 및 시간" / "장소" / "예산" 섹션 (버튼들)
 *
 * "날짜 및 시간 산출/자신의 일정 수정"(ScheduleResultDialog, MyScheduleEditDialog 연동),
 * "장소 확인/장소 투표/랜덤 장소 추천"(PlaceListDialog, PlaceVoteDialog 연동),
 * "예산 확인"(ExpenseListDialog 연동, 지출 관리 + 정산 계산),
 * "명단확인"(MemberListDialog 연동, 방장은 강퇴 가능)이 모두 실제로 동작합니다.
 * (다이어리 기능은 이번 구현 범위에서 제외되어 화면에서 빠졌습니다.)
 */
public class RoomDetailView extends JFrame {

    private final MainView owner;
    private final Room room;
    private final Member loginMember;

    private final RoomRepository roomRepository = new RoomRepository();
    private final MemberRepository memberRepository = new MemberRepository();
    private final AvailabilityRepository availabilityRepository = new AvailabilityRepository();
    private final PlaceRepository placeRepository = new PlaceRepository();
    private final PlaceVoteRepository placeVoteRepository = new PlaceVoteRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final SettlementRepository settlementRepository = new SettlementRepository();
    private final TravelDateRepository travelDateRepository = new TravelDateRepository();
    private final RoomFinalRepository finalRepository = new RoomFinalRepository();
    private final MeetingRoundRepository meetingRoundRepository = new MeetingRoundRepository();
    private final RoundParticipantRepository roundParticipantRepository = new RoundParticipantRepository();

    // 대한민국 전국 도시 목록 ("단체 여행" 방의 "백지도 랜덤 장소 추천" 버튼에서 무작위로 하나를 뽑는 데 사용)
    private static final String[] KOREAN_CITIES = {
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "수원", "춘천", "강릉", "전주", "여수", "순천", "경주", "안동",
            "제주", "서귀포", "목포", "통영", "거제", "포항", "창원", "청주", "천안", "속초"
    };

    public RoomDetailView(MainView owner, Room room, Member loginMember) {
        this.owner = owner;
        this.room = room;
        this.loginMember = loginMember;
        initFrame();
        initComponents();
    }

    private void initFrame() {
        setTitle(room.getName());
        setSize(432, 768);
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
        JPanel root = new ScrollableContentPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 20, 18));

        JPanel topBar = buildTopBar();
        JPanel memberListRow = buildMemberListRow();
        JPanel sectionsCard = buildSectionsCard();
        JPanel finalCard = buildFinalCard();

        // topBar, memberListRow, sectionsCard, finalCard 네 줄의 가로 폭을 카드의 폭에 맞춰 전부
        // 똑같이 맞추고, 화면(root) 가운데에 나란히 정렬합니다. 이렇게 하면 줄들의 왼쪽/오른쪽 끝이
        // 서로 딱 맞아떨어지고, 화면 좌우 여백도 항상 똑같아서 한쪽으로 쏠려 보이지 않습니다.
        int contentWidth = sectionsCard.getPreferredSize().width;
        applyCenteredFixedWidth(topBar, contentWidth);
        applyCenteredFixedWidth(memberListRow, contentWidth);
        applyCenteredFixedWidth(sectionsCard, contentWidth);
        applyCenteredFixedWidth(finalCard, contentWidth);

        root.add(topBar);
        root.add(Box.createVerticalStrut(10));
        root.add(memberListRow);
        root.add(Box.createVerticalStrut(16));
        root.add(sectionsCard);
        root.add(Box.createVerticalStrut(16));
        root.add(finalCard);
        // 남는 세로 공간은 이 "글루(glue)"가 다 흡수하도록 해서, 위의 실제 내용(상단바 등)이
        // 세로로 늘어나 서로 멀리 떨어지는 문제를 막습니다. (Swing BoxLayout의 잘 알려진 함정 대응)
        root.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);

        setContentPane(scrollPane);
    }

    /**
     * 컴포넌트의 가로 폭을 width로 고정하고(더 늘어나거나 줄어들지 않게), 세로는 원래
     * 필요한 만큼만 차지하도록 한 뒤, BoxLayout 안에서 가로 방향으로 가운데 정렬되게 만듭니다.
     * -> topBar / memberListRow / sectionsCard 세 줄의 폭을 똑같이 맞춰서 화면 가운데
     *    나란히 정렬하는 데 사용합니다.
     */
    private void applyCenteredFixedWidth(JComponent component, int width) {
        int height = component.getPreferredSize().height;
        Dimension fixedSize = new Dimension(width, height);
        component.setPreferredSize(fixedSize);
        component.setMaximumSize(fixedSize);
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
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

        JLabel categoryLabel = new JLabel(room.getCategory() + " · " + room.getMemberCount() + "명");
        categoryLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(0x99, 0x99, 0x99));
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));

        infoPanel.add(nameRow);
        infoPanel.add(categoryLabel);

        JButton profileButton = new JButton("MY"); // 마이페이지로 이동하는 버튼 (아이콘 대신 글씨로 표시해 더 직관적으로 만듦)
        profileButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
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

    /** "날짜 및 시간" / "장소" / "예산" / (단체 약속이면) "차수별 인원 조사" 섹션들을 담은 카드 */
    private JPanel buildSectionsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isTravel = Room.CATEGORY_TRAVEL.equals(room.getCategory());

        // ---------- 날짜 및 시간 (단체 약속: 시간 포함 / 단체 여행: 날짜만) ----------
        if (isTravel) {
            card.add(buildSection("📅", "날짜 및 시간", "여행 날짜를 선택해 주세요.",
                    new JButton[]{
                            button("날짜 산출", true, e -> handleComputeTravelSchedule()),
                            button("자신의 일정 수정", false, e -> handleEditTravelSchedule())
                    }
            ));
        } else {
            card.add(buildSection("📅", "날짜 및 시간", "날짜와 시간을 선택해 주세요.",
                    new JButton[]{
                            button("날짜 및 시간 산출", true, e -> handleComputeSchedule()),
                            button("자신의 일정 수정", false, e -> handleEditMySchedule())
                    }
            ));
        }
        card.add(Box.createVerticalStrut(16));
        card.add(separator());
        card.add(Box.createVerticalStrut(16));

        // ---------- 장소 (단체 여행이면 "백지도 랜덤 장소 추천" 버튼이 추가로 붙음) ----------
        if (isTravel) {
            card.add(buildSection("📍", "장소", "장소를 확인하고 투표해주세요.",
                    new JButton[]{
                            button("장소 확인", true, e -> handleCheckPlaces()),
                            button("장소 투표", false, e -> handleVotePlaces())
                    },
                    new JButton[]{
                            button("랜덤 장소 추천", false, e -> handleRandomPlace()),
                            button("백지도 랜덤 장소 추천", false, e -> handleRandomMapPlace())
                    }
            ));
        } else {
            card.add(buildSection("📍", "장소", "장소를 확인하고 투표해주세요.",
                    new JButton[]{
                            button("장소 확인", true, e -> handleCheckPlaces()),
                            button("장소 투표", false, e -> handleVotePlaces()),
                            button("랜덤 장소 추천", false, e -> handleRandomPlace())
                    }
            ));
        }
        card.add(Box.createVerticalStrut(16));
        card.add(separator());
        card.add(Box.createVerticalStrut(16));

        // ---------- 예산 ----------
        card.add(buildSection("💰", "예산", "비용을 입력하고 확인해주세요.",
                new JButton[]{
                        button("확인", true, e -> handleCheckExpenses())
                }
        ));

        // ---------- 차수별 인원 조사 (단체 약속 방에서만 보임, 단체 여행 방에는 없음) ----------
        if (!isTravel) {
            card.add(Box.createVerticalStrut(16));
            card.add(separator());
            card.add(Box.createVerticalStrut(16));

            card.add(buildSection("🗓", "차수별 인원 조사", "차수 별 인원 조사를 할 수 있습니다.",
                    new JButton[]{
                            button("차수별 인원조사", true, e -> handleShowRounds())
                    }
            ));
        }

        return card;
    }

    /**
     * 방 메인 화면 맨 아래의 "모임 최종 날짜/시간/장소" 표시 카드를 만듭니다.
     * 아직 확정되지 않은 항목은 "미정"으로 표시하고, 방장이 보고 있을 때만 각 항목 옆에
     * "수정" 버튼이 나타나서 직접 값을 정하거나 바꿀 수 있습니다.
     * (단체 여행 방은 시간 개념이 없으므로 "모임 최종 시간" 줄이 표시되지 않습니다)
     */
    private JPanel buildFinalCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isTravel = Room.CATEGORY_TRAVEL.equals(room.getCategory());
        boolean iAmOwner = loginMember.getId().equals(room.getOwnerId());
        RoomFinalDecision finalDecision = finalRepository.getForRoom(room.getCode());

        JLabel titleLabel = new JLabel("모임 최종 정보");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.TEXT_DARK);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));

        String dateText = finalDecision.getFinalDate().isEmpty() ? "미정" : finalDecision.getFinalDate();
        card.add(buildFinalRow("모임 최종 날짜", dateText, iAmOwner, e -> handleEditFinalDate()));

        // 단체 여행 방은 시간 개념이 없으므로 "모임 최종 시간" 줄을 아예 표시하지 않습니다.
        if (!isTravel) {
            card.add(Box.createVerticalStrut(8));
            String timeText = finalDecision.getFinalStartTime().isEmpty()
                    ? "미정"
                    : finalDecision.getFinalStartTime() + " ~ " + finalDecision.getFinalEndTime();
            card.add(buildFinalRow("모임 최종 시간", timeText, iAmOwner, e -> handleEditFinalTime()));
        }

        card.add(Box.createVerticalStrut(8));
        String placeText = finalDecision.getFinalPlace().isEmpty() ? "미정" : finalDecision.getFinalPlace();
        card.add(buildFinalRow("모임 최종 장소", placeText, iAmOwner, e -> handleEditFinalPlace()));

        return card;
    }

    /**
     * "모임 최종 OO" 줄을 만듭니다. 라벨(+ 방장이면 "수정" 버튼)을 위쪽 한 줄에,
     * 실제 값(날짜/시간/장소)은 그 아래 줄에 따로 둡니다.
     * -> 라벨과 값과 버튼을 전부 한 줄에 넣으면 날짜/시간처럼 긴 글자가 잘려서 안 보이는
     *    문제가 있었기 때문에, 값 부분을 아예 별도의 줄로 분리해서 폭을 넉넉하게 쓰도록 했습니다.
     */
    private JPanel buildFinalRow(String label, String value, boolean showEditButton,
                                  java.awt.event.ActionListener onEdit) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);

        JPanel labelRow = new JPanel(new BorderLayout(8, 0));
        labelRow.setOpaque(false);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        labelText.setForeground(new Color(0x99, 0x99, 0x99));
        labelRow.add(labelText, BorderLayout.CENTER);

        if (showEditButton) {
            JButton editButton = new JButton("수정");
            editButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            editButton.setForeground(Theme.PRIMARY_GREEN_DARK);
            editButton.setBorderPainted(false);
            editButton.setContentAreaFilled(false);
            editButton.setFocusPainted(false);
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editButton.addActionListener(onEdit);
            labelRow.add(editButton, BorderLayout.EAST);
        }

        // 값 부분은 줄 폭 전체를 혼자 차지하고, 글자가 길면 자동으로 줄바꿈되도록 HTML로 감쌉니다.
        JLabel valueLabel = new JLabel("<html><div style='width:320px;'>" + escapeHtml(value) + "</div></html>");
        valueLabel.setFont(Theme.FONT_NORMAL);
        valueLabel.setForeground(Theme.TEXT_DARK);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        row.add(labelRow);
        row.add(valueLabel);

        return row;
    }

    /** HTML 라벨 안에 넣을 텍스트에서 특수문자(<, >, &)를 안전한 형태로 바꿔줍니다. */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 섹션 하나(아이콘+제목, 설명, 버튼 줄들)를 만듭니다.
     * buttonRows의 배열 하나가 화면의 한 줄에 해당하며, 그 줄 안의 버튼들은 GridLayout으로
     * 폭을 똑같이 나눠 가져서 카드 너비에 꽉 차게 보이도록 합니다. (화면 크기에 맞춰 자동으로 늘어남)
     */
    private JPanel buildSection(String emoji, String title, String desc, JButton[]... buttonRows) {
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
        descLabel.setBorder(BorderFactory.createEmptyBorder(2, 24, 10, 0));

        section.add(titleRow);
        section.add(descLabel);

        for (int i = 0; i < buttonRows.length; i++) {
            JPanel buttonRow = new JPanel(new GridLayout(1, buttonRows[i].length, 8, 0));
            buttonRow.setOpaque(false);
            buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            for (JButton b : buttonRows[i]) {
                buttonRow.add(b);
            }
            section.add(buttonRow);
            if (i < buttonRows.length - 1) {
                section.add(Box.createVerticalStrut(8));
            }
        }

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

    // ---------------- 이벤트 핸들러 ----------------

    /** 뒤로가기: 이 창을 닫고 방 목록 화면(MainView)으로 돌아갑니다. */
    private void handleBack() {
        dispose();
        owner.refreshAndShow();
    }

    /** "마이페이지" 클릭: 회원 정보 수정 화면(ProfileEditDialog)을 엽니다. */
    private void handleOpenProfile() {
        ProfileEditDialog dialog = new ProfileEditDialog(this, memberRepository, loginMember);
        dialog.setVisible(true);
    }

    /**
     * "명단확인" 버튼: 참여자 목록 화면(MemberListDialog)을 엽니다.
     * 방장이라면 다른 참여자를 강퇴할 수 있고, 강퇴로 인원수가 바뀌었을 수 있으므로
     * 창이 닫히면 화면 전체를 다시 그려서 상단의 인원수 표시 등을 최신 상태로 맞춥니다.
     */
    private void handleShowMemberList() {
        MemberListDialog dialog = new MemberListDialog(this, roomRepository, memberRepository, room, loginMember);
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /**
     * "날짜 및 시간 산출" 버튼(단체 약속 전용): 참여자들이 지금까지 입력한 가능 시간을 모두
     * 불러와서 겹치는 시간(모두가 참석 가능한 시간)을 계산해 1~3순위로 보여주는 화면을 엽니다.
     * 방장이라면 그 화면 안에서 순위를 클릭해 최종 날짜/시간으로 확정할 수도 있으므로,
     * 화면이 닫히면 이 방 메인 화면도 다시 그려서 "모임 최종 정보"가 바뀌었을 수 있는 걸 반영합니다.
     */
    private void handleComputeSchedule() {
        ScheduleResultDialog dialog = new ScheduleResultDialog(
                this, availabilityRepository, memberRepository, finalRepository, room, loginMember);
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /**
     * "자신의 일정 수정" 버튼(단체 약속 전용): 내가 예전에 입력해둔 가능 시간 목록을 불러와서
     * 추가/수정/삭제한 뒤 다시 저장할 수 있는 화면을 엽니다.
     * -> ScheduleInputDialog(방 생성 직후 화면)와 달리 장소 입력이 없고, 기존 값을 불러와 보여줍니다.
     */
    private void handleEditMySchedule() {
        MyScheduleEditDialog dialog =
                new MyScheduleEditDialog(this, availabilityRepository, room, loginMember.getId());
        dialog.setVisible(true);
    }

    /**
     * "날짜 산출" 버튼(단체 여행 전용): 참여자들이 제출한 여행 날짜 중 모두가 겹치는 날짜
     * 구간을 계산해 가장 빠른 날짜 순서로 1~5순위를 보여주는 화면을 엽니다.
     * 방장이라면 그 화면에서 클릭해 최종 날짜로 확정할 수 있으므로, 닫히면 다시 그립니다.
     */
    private void handleComputeTravelSchedule() {
        TravelScheduleResultDialog dialog = new TravelScheduleResultDialog(
                this, travelDateRepository, finalRepository, room, loginMember);
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /**
     * "자신의 일정 수정" 버튼(단체 여행 전용): 내가 예전에 제출한 여행 날짜 목록을 불러와서
     * 추가/수정/삭제한 뒤 다시 저장할 수 있는 화면을 엽니다. (시간 없이 날짜만 다룸)
     */
    private void handleEditTravelSchedule() {
        TravelScheduleEditDialog dialog =
                new TravelScheduleEditDialog(this, travelDateRepository, room, loginMember.getId());
        dialog.setVisible(true);
    }

    /**
     * "장소 확인" 버튼: 등록된 장소 후보 목록을 보여주고, 새 장소를 추가하거나 삭제할 수 있는
     * PlaceListDialog 화면을 엽니다.
     */
    private void handleCheckPlaces() {
        ensurePlaceListSeeded();
        PlaceListDialog dialog = new PlaceListDialog(this, placeRepository, placeVoteRepository, room);
        dialog.setVisible(true);
    }

    /** "장소 투표" 버튼: 등록된 장소 후보들에 투표할 수 있는 PlaceVoteDialog 화면을 엽니다. */
    private void handleVotePlaces() {
        ensurePlaceListSeeded();
        PlaceVoteDialog dialog = new PlaceVoteDialog(
                this, placeRepository, placeVoteRepository, memberRepository, room, loginMember.getId());
        dialog.setVisible(true);
    }

    /**
     * "랜덤 장소 추천" 버튼: 등록된 장소 후보 중 하나를 무작위로 뽑아서 보여줍니다.
     * 등록된 장소가 하나도 없으면 먼저 장소를 추가하라고 안내합니다.
     */
    private void handleRandomPlace() {
        ensurePlaceListSeeded();
        List<String> places = placeRepository.getPlaces(room.getCode());

        if (places.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "등록된 장소가 없습니다. 먼저 '장소 확인'에서 장소를 추가해주세요.",
                    "랜덤 장소 추천", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 0 이상 places.size() 미만의 정수 중 하나를 무작위로 뽑아서, 그 자리의 장소를 선택한다.
        int randomIndex = new Random().nextInt(places.size());
        String picked = places.get(randomIndex);

        JOptionPane.showMessageDialog(this,
                "오늘의 추천 장소는...\n\n\"" + picked + "\"  입니다!",
                "랜덤 장소 추천", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * "백지도 랜덤 장소 추천" 버튼(단체 여행 전용): 참여자가 입력한 장소 목록과는 상관없이,
     * 대한민국 전국 도시 목록(KOREAN_CITIES) 중에서 하나를 무작위로 뽑아서 보여줍니다.
     */
    private void handleRandomMapPlace() {
        int randomIndex = new Random().nextInt(KOREAN_CITIES.length);
        String picked = KOREAN_CITIES[randomIndex];

        JOptionPane.showMessageDialog(this,
                "이번 여행 추천 도시는...\n\n\"" + picked + "\"  입니다!",
                "백지도 랜덤 장소 추천", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 이 방에 등록된 장소 후보가 아직 하나도 없다면, 예전에 "날짜 및 시간" 입력 화면에서
     * 참여자들이 적어두었던 장소 추천(availability.csv의 place 칸)을 가져와 초기 목록으로 채워 넣습니다.
     * -> 이미 장소가 하나라도 등록되어 있다면 아무 일도 하지 않습니다. (PlaceRepository.seedIfEmpty 참고)
     */
    private void ensurePlaceListSeeded() {
        Map<String, String> placesByMember = availabilityRepository.getLatestPlaceByMember(room.getCode());
        placeRepository.seedIfEmpty(room.getCode(), placesByMember.values());
    }

    /**
     * "확인" 버튼(예산): 지금까지 등록된 지출 내역을 보여주고, 추가/수정/삭제 및 정산까지
     * 할 수 있는 ExpenseListDialog 화면을 엽니다.
     */
    private void handleCheckExpenses() {
        ExpenseListDialog dialog = new ExpenseListDialog(
                this, expenseRepository, settlementRepository, memberRepository,
                meetingRoundRepository, roundParticipantRepository, room, loginMember);
        dialog.setVisible(true);
    }

    /**
     * "차수별 인원조사" 버튼(단체 약속 전용): 이 방의 모임 차수 목록 화면(RoundListDialog)을 엽니다.
     * 방장이라면 그 화면에서 새 차수를 추가할 수도 있습니다.
     */
    private void handleShowRounds() {
        RoundListDialog dialog = new RoundListDialog(
                this, meetingRoundRepository, roundParticipantRepository, memberRepository, room, loginMember);
        dialog.setVisible(true);
    }

    /** "모임 최종 날짜"의 "수정" 버튼(방장 전용): 날짜를 직접 정하는 화면을 엽니다. */
    private void handleEditFinalDate() {
        FinalDateEditDialog dialog = new FinalDateEditDialog(this, finalRepository, room);
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /** "모임 최종 시간"의 "수정" 버튼(방장 전용, 단체 약속만): 시간을 직접 정하는 화면을 엽니다. */
    private void handleEditFinalTime() {
        FinalTimeEditDialog dialog = new FinalTimeEditDialog(this, finalRepository, room);
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /** "모임 최종 장소"의 "수정" 버튼(방장 전용): 장소를 직접 입력하는 화면을 엽니다. */
    private void handleEditFinalPlace() {
        RoomFinalDecision current = finalRepository.getForRoom(room.getCode());
        FinalPlaceEditDialog dialog =
                new FinalPlaceEditDialog(this, finalRepository, room, current.getFinalPlace());
        dialog.setVisible(true);

        initComponents();
        revalidate();
        repaint();
    }

    /**
     * 세로 스크롤은 허용하되, 가로 폭은 항상 JScrollPane의 보이는 영역(뷰포트) 너비에
     * 정확히 맞춰지도록 만든 패널입니다. Scrollable 인터페이스를 구현해서
     * getScrollableTracksViewportWidth()가 true를 반환하도록 하면, 이 패널의 너비는
     * 절대 뷰포트보다 넓어질 수 없습니다 -> 내용이 옆으로 잘리거나 가로 스크롤이 생기는
     * 문제를 근본적으로 막아줍니다. (root 패널을 이 클래스로 만들어서 사용합니다)
     */
    private static class ScrollableContentPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // 가로 폭은 항상 뷰포트 너비에 맞춘다 -> 가로 스크롤/잘림을 원천적으로 방지
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false; // 세로는 내용이 창보다 길어지면 정상적으로 스크롤되도록 허용
        }
    }
}
