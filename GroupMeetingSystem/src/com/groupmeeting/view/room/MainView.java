package com.groupmeeting.view.room;

import com.groupmeeting.view.auth.ProfileEditDialog;
import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.RoomRepository;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 로그인 완료 후 진입하는 메인 화면입니다. (목업 디자인 "② 메인 화면" 반영)
 *
 * 구성 요소:
 *  - 상단: 검색 아이콘 + "모임." 타이틀 + 알림(종) 아이콘
 *  - 중앙: "방 리스트" 헤더(+ 방 만들기 단축 버튼) 및 현재 참여 중인 모임 방 목록
 *          각 행에는 아바타, 방 이름, 참여 인원(n/8), 신규 표시, 나가기 버튼이 포함됩니다.
 *  - 하단: "방 만들기" / "방 조인" 버튼
 */
public class MainView extends JFrame {

    private final Member loginMember;               // 현재 로그인한 사용자 정보
    private final RoomRepository roomRepository = new RoomRepository();

    // 이번 실행(세션) 중에 내가 새로 만든 방의 코드 목록 -> "신규" 표시에 사용
    private final Set<String> newlyCreatedCodes = new HashSet<>();

    private JPanel roomListPanel; // 방 목록이 그려지는 패널
    private JTextField searchField; // 방 이름 검색창

    public MainView(Member loginMember) {
        this.loginMember = loginMember;
        initFrame();
        initComponents();
    }

    /** JFrame 기본 속성 설정 */
    private void initFrame() {
        setTitle("단체 모임 관리 시스템");
        setSize(432, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 전체 화면 레이아웃을 구성합니다. (상단바 / 중앙 방 리스트 / 하단 버튼) */
    private void initComponents() {
        setLayout(new BorderLayout());

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomNav(), BorderLayout.SOUTH);
    }

    /** 상단 바: 검색 아이콘 + "모임." 타이틀 + 알림 아이콘 */
    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.BACKGROUND);
        topBar.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));

        // 상단 왼쪽 로고 (/image/logo.png). 클릭하면 기존처럼 검색창을 여닫을 수 있습니다.
        JLabel searchIcon = new JLabel(loadLogoIcon(28));
        searchIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleSearchField();
            }
        });

        JLabel titleLabel = new JLabel("모이락!", SwingConstants.CENTER);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);

        // 벨 아이콘: 목업에는 알림 전용 아이콘이지만, 마이페이지 진입 동선을 함께 제공하기 위해
        // 클릭 시 "알림" / "마이페이지"를 선택할 수 있는 팝업 메뉴를 띄웁니다.
        JButton bellButton = new JButton("MY"); // 🔔
        bellButton.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        bellButton.setBorderPainted(false);
        bellButton.setFocusPainted(false);
        bellButton.setContentAreaFilled(false);
        bellButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPopupMenu bellMenu = new JPopupMenu();
        JMenuItem profileItem = new JMenuItem("마이페이지 - " + loginMember.getNickname());
        profileItem.addActionListener(e -> handleOpenProfile());
        //JMenuItem notificationItem = new JMenuItem("알림 (준비 중)");
        //notificationItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                //"알림 기능은 추후 지원될 예정입니다.", "알림", JOptionPane.INFORMATION_MESSAGE));
        bellMenu.add(profileItem);
        //bellMenu.add(notificationItem);

        bellButton.addActionListener(e -> bellMenu.show(bellButton, 0, bellButton.getHeight()));

        topBar.add(searchIcon, BorderLayout.WEST);
        topBar.add(titleLabel, BorderLayout.CENTER);
        topBar.add(bellButton, BorderLayout.EAST);

        return topBar;
    }

    /** 검색창을 보이거나 숨깁니다. (🔍 아이콘 클릭 시) */
    private void toggleSearchField() {
        if (searchField == null) return;
        searchField.setVisible(!searchField.isVisible());
        if (searchField.isVisible()) {
            searchField.requestFocus();
        } else {
            searchField.setText("");
            refreshRoomList("");
        }
    }

    /** 중앙 영역: 검색창 + "방 리스트" 헤더 + 방 목록 */
    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BACKGROUND);
        center.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        // 검색창 (기본은 숨김 상태, 🔍 클릭 시 표시)
        searchField = new JTextField();
        Theme.styleTextField(searchField);
        searchField.setVisible(false);
        searchField.putClientProperty("JTextField.placeholderText", "방 이름 검색");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            private void doSearch() { refreshRoomList(searchField.getText().trim()); }
        });

        // 헤더: "방 리스트" 제목
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Theme.BACKGROUND);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));

        JLabel listTitle = new JLabel("방 리스트");
        listTitle.setFont(Theme.FONT_SUBTITLE);
        listTitle.setForeground(Theme.TEXT_DARK);

        headerPanel.add(listTitle, BorderLayout.WEST);

        JPanel topOfCenter = new JPanel();
        topOfCenter.setLayout(new BoxLayout(topOfCenter, BoxLayout.Y_AXIS));
        topOfCenter.setBackground(Theme.BACKGROUND);
        topOfCenter.add(searchField);
        topOfCenter.add(headerPanel);

        // 방 목록 패널
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(Theme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);

        center.add(topOfCenter, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        refreshRoomList("");

        return center;
    }

    /**
     * 방 리스트를 다시 그립니다. 현재 로그인한 사용자가 참여 중인 방만 표시하며,
     * keyword가 비어있지 않으면 방 이름에 포함된 경우만 필터링합니다.
     */
    private void refreshRoomList(String keyword) {
        roomListPanel.removeAll();

        List<Room> myRooms = roomRepository.getRoomsForMember(loginMember.getId());

        boolean any = false;
        for (Room room : myRooms) {
            if (!keyword.isEmpty() && !room.getName().contains(keyword)) {
                continue;
            }
            roomListPanel.add(createRoomRow(room));
            roomListPanel.add(Box.createVerticalStrut(10));
            any = true;
        }

        if (!any) {
            JLabel emptyLabel = new JLabel(
                    keyword.isEmpty() ? "아직 참여 중인 모임 방이 없습니다." : "검색 결과가 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            roomListPanel.add(emptyLabel);
        }

        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    /**
     * 모임 방 한 개를 나타내는 행(row)을 만듭니다.
     * 아바타(방 이름 첫 글자) + 방 이름 + 인원수(n/8) + (신규 시) 신규 태그 + 나가기 버튼으로 구성됩니다.
     */
    private JPanel createRoomRow(Room room) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 좌측: 아바타 원 + 방 이름 + 인원수 + 신규 태그
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel avatar = new JLabel(room.getName().substring(0, 1), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(Theme.ACCENT_GREEN);
        avatar.setForeground(Theme.PRIMARY_GREEN_DARK);
        avatar.setFont(Theme.FONT_SUBTITLE);
        avatar.setPreferredSize(new Dimension(36, 36));

        JLabel nameLabel = new JLabel(room.getName());
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setForeground(Theme.TEXT_DARK);

        JLabel countLabel = new JLabel(room.getMemberCount() + "명");
        countLabel.setFont(Theme.FONT_NORMAL);
        countLabel.setForeground(new Color(0x99, 0x99, 0x99));

        // 이 방이 "단체 약속"인지 "단체 여행"인지 작게 표시하는 태그
        JLabel categoryTag = new JLabel(room.getCategory());
        categoryTag.setOpaque(true);
        categoryTag.setBackground(Theme.ACCENT_GREEN);
        categoryTag.setForeground(Theme.PRIMARY_GREEN_DARK);
        categoryTag.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
        categoryTag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        leftPanel.add(avatar);
        leftPanel.add(nameLabel);
        leftPanel.add(countLabel);
        leftPanel.add(categoryTag);

        if (newlyCreatedCodes.contains(room.getCode())) {
            JLabel newTag = new JLabel("신규");
            newTag.setOpaque(true);
            newTag.setBackground(new Color(0xE5, 0x39, 0x35));
            newTag.setForeground(Color.WHITE);
            newTag.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            newTag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            leftPanel.add(newTag);
        }

        // 우측: 나가기 버튼
        JButton leaveButton = new JButton("나가기");
        leaveButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        leaveButton.setForeground(new Color(0x99, 0x99, 0x99));
        leaveButton.setBorderPainted(false);
        leaveButton.setContentAreaFilled(false);
        leaveButton.setFocusPainted(false);
        leaveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        leaveButton.addActionListener(e -> handleLeaveRoom(room));

        row.add(leftPanel, BorderLayout.CENTER);
        row.add(leaveButton, BorderLayout.EAST);

        // 행 전체(나가기 버튼 제외)를 클릭하면 방에 입장합니다. (방 상세 기능은 추후 구현 예정)
        leftPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleEnterRoom(room);
            }
        });

        return row;
    }

    /** 하단: "방 만들기" / "방 조인" 버튼 영역 */
    private JPanel buildBottomNav() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setBackground(Theme.BACKGROUND);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 16, 20, 16));

        JButton createRoomButton = new JButton("\uD83C\uDFE0  방 만들기"); // 🏠
        Theme.styleButton(createRoomButton);
        createRoomButton.addActionListener(e -> handleCreateRoom());

        JButton joinRoomButton = new JButton("\u2192|  방 조인"); // →|
        Theme.styleButton(joinRoomButton);
        joinRoomButton.addActionListener(e -> handleJoinRoom());

        bottom.add(createRoomButton);
        bottom.add(joinRoomButton);

        return bottom;
    }

    // ---------------- 이벤트 핸들러 ----------------

    /** "마이페이지" 클릭: 회원 정보 수정 화면(ProfileEditDialog)을 엽니다. */
    private void handleOpenProfile() {
        ProfileEditDialog dialog = new ProfileEditDialog(this, new MemberRepository(), loginMember);
        dialog.setVisible(true);
    }

    /** "방 만들기" 클릭: CreateRoomDialog(이름/코드 입력 -> 카테고리 선택)를 열고, 성공 시 목록을 새로고침합니다. */
    private void handleCreateRoom() {
        CreateRoomDialog dialog = new CreateRoomDialog(this, roomRepository, loginMember.getId());
        dialog.setVisible(true);

        Room created = dialog.getCreatedRoom();
        if (created != null) {
            newlyCreatedCodes.add(created.getCode());
            openRoomDetail(created);
        }
    }

    /** "방 조인" 클릭: JoinRoomDialog(코드 입력)를 열고, 성공 시 목록을 새로고침합니다. */
    private void handleJoinRoom() {
        JoinRoomDialog dialog = new JoinRoomDialog(this, roomRepository, loginMember.getId());
        dialog.setVisible(true);

        Room joined = dialog.getJoinedRoom();
        if (joined != null) {
            refreshRoomList(searchField.getText().trim());
        }
    }

    /** 방 나가기: 확인 후 참여자 목록에서 제거하고 목록을 새로고침합니다. */
    private void handleLeaveRoom(Room room) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "'" + room.getName() + "' 방에서 나가시겠습니까?",
                "방 나가기", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            roomRepository.leaveRoom(room.getCode(), loginMember.getId());
            refreshRoomList(searchField.getText().trim());
        }
    }

    /** 방 입장: 방 메인 화면(RoomDetailView)으로 이동합니다. */
    private void handleEnterRoom(Room room) {
        openRoomDetail(room);
    }

    /** 방 메인 화면을 열고 이 화면(MainView)은 숨깁니다. */
    private void openRoomDetail(Room room) {
        RoomDetailView detailView = new RoomDetailView(this, room, loginMember);
        detailView.setVisible(true);
        this.setVisible(false);
    }

    /**
     * RoomDetailView에서 뒤로가기로 돌아왔을 때 호출됩니다.
     * 방 목록을 최신 상태로 새로고침하고 이 화면을 다시 보여줍니다.
     */
    public void refreshAndShow() {
        refreshRoomList(searchField.getText().trim());
        setVisible(true);
    }

    /** "/image/logo.png" 이미지를 불러와 size x size 크기로 축소한 아이콘을 반환합니다. */
    private ImageIcon loadLogoIcon(int size) {
        ImageIcon original = new ImageIcon(getClass().getResource("/image/logo.png"));
        Image scaled = original.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
