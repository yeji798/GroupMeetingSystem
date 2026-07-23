package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.util.AvailabilityRepository;
import com.groupmeeting.util.RoomRepository;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 목업 디자인의 "방 생성" 화면을 구현한 모달 다이얼로그입니다.
 *
 * 흐름: 방 이름 + 4자리 방 코드 입력 -> "생성!" 클릭 -> 유효성 검증
 *       -> CategorySelectDialog로 카테고리(단체 약속/단체 여행) 선택
 *       -> 모두 완료되면 Room을 생성하여 CSV에 저장
 */
public class CreateRoomDialog extends JDialog {

    private final RoomRepository roomRepository;
    private final String ownerId; // 방을 생성하는 현재 로그인 사용자의 아이디

    private JTextField nameField;
    private CodeInputPanel codeInputPanel;

    // 생성이 완료된 경우 결과로 담기는 Room (실패/취소 시 null)
    private Room createdRoom = null;

    public CreateRoomDialog(JFrame owner, RoomRepository roomRepository, String ownerId) {
        super(owner, "방 생성", true);
        this.roomRepository = roomRepository;
        this.ownerId = ownerId;
        initDialog();
        initComponents();
    }

    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("방 생성");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("방 이름");
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField = new JTextField();
        Theme.styleTextField(nameField);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel codeLabel = new JLabel("방 코드 (4자리 숫자)");
        codeLabel.setFont(Theme.FONT_NORMAL);
        codeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        codeInputPanel = new CodeInputPanel();
        codeInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 편의를 위해 중복되지 않는 코드를 기본값으로 미리 채워줍니다. 원하면 직접 수정 가능합니다.
        codeInputPanel.setCode(roomRepository.generateUniqueCode());

        JLabel hintLabel = new JLabel("※ 1000 ~ 9999 사이의 숫자");
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createButton = new JButton("생성 !");
        Theme.styleButton(createButton);
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        createButton.addActionListener(e -> handleCreate());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(24));
        root.add(nameLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(nameField);
        root.add(Box.createVerticalStrut(20));
        root.add(codeLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(codeInputPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(hintLabel);
        root.add(Box.createVerticalStrut(28));
        root.add(createButton);

        setContentPane(root);
    }

    /**
     * "생성!" 버튼 클릭 시 실행됩니다.
     * 1) 입력값 검증 -> 2) 카테고리 선택 다이얼로그 오픈 -> 3) Room 생성 및 저장
     */
    private void handleCreate() {
        String roomName = nameField.getText().trim();
        String code = codeInputPanel.getCode();

        if (roomName.isEmpty()) {
            showWarning("방 이름을 입력해주세요.");
            return;
        }
        if (!codeInputPanel.isComplete()) {
            showWarning("방 코드 4자리를 모두 입력해주세요.");
            return;
        }

        int codeNumber;
        try {
            codeNumber = Integer.parseInt(code);
        } catch (NumberFormatException ex) {
            showWarning("방 코드는 숫자여야 합니다.");
            return;
        }
        if (codeNumber < 1000 || codeNumber > 9999) {
            showWarning("방 코드는 1000 ~ 9999 사이의 숫자여야 합니다.");
            return;
        }
        if (roomRepository.isCodeDuplicate(code)) {
            showWarning("이미 사용 중인 방 코드입니다. 다른 코드를 입력해주세요.");
            return;
        }

        // 카테고리 선택 화면으로 이동 (여행 vs 약속 골라!)
        CategorySelectDialog categoryDialog = new CategorySelectDialog(this);
        categoryDialog.setVisible(true);
        String category = categoryDialog.getSelectedCategory();

        if (category == null) {
            // 카테고리를 선택하지 않고 닫은 경우 생성을 취소합니다.
            return;
        }

        // 방장은 자동으로 첫 번째 참여자가 됩니다.
        List<String> members = new ArrayList<>();
        members.add(ownerId);

        Room room = new Room(code, roomName, category, ownerId, members);
        boolean saved = roomRepository.addRoom(room);

        if (saved) {
            this.createdRoom = room;
            JOptionPane.showMessageDialog(this,
                    "'" + roomName + "' 방이 생성되었습니다!",
                    "생성 완료", JOptionPane.INFORMATION_MESSAGE);

            // "단체 약속" 방이면, 방장이 바로 자신의 가능 날짜/시간/장소를 입력할 수 있도록 안내합니다.
            if (Room.CATEGORY_PROMISE.equals(category)) {
                ScheduleInputDialog scheduleDialog =
                        new ScheduleInputDialog(this, new AvailabilityRepository(), room, ownerId);
                scheduleDialog.setVisible(true);
            }

            dispose();
        } else {
            showWarning("방 생성 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }

    /** 생성이 완료된 Room을 반환합니다. 취소되었거나 실패한 경우 null을 반환합니다. */
    public Room getCreatedRoom() {
        return createdRoom;
    }
}
