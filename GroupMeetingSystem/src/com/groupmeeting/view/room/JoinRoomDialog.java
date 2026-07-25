package com.groupmeeting.view.room;

import com.groupmeeting.view.common.CodeInputPanel;
import com.groupmeeting.view.common.Theme;
import com.groupmeeting.view.schedule.ScheduleInputDialog;
import com.groupmeeting.view.travel.TravelDateInputDialog;

import com.groupmeeting.model.Room;
import com.groupmeeting.repository.AvailabilityRepository;
import com.groupmeeting.repository.PlaceRepository;
import com.groupmeeting.repository.RoomRepository;
import com.groupmeeting.repository.TravelDateRepository;

import javax.swing.*;
import java.awt.*;

/**
 * 목업 디자인의 "방 조인" 화면을 구현한 모달 다이얼로그입니다.
 * 4자리 초대 코드를 입력하면 해당 코드의 방을 찾아 현재 사용자를 참여시킵니다.
 */
public class JoinRoomDialog extends JDialog {

    private final RoomRepository roomRepository;
    private final String memberId; // 참여를 시도하는 현재 로그인 사용자의 아이디

    private CodeInputPanel codeInputPanel;

    // 참여에 성공한 경우 결과로 담기는 Room (실패/취소 시 null)
    private Room joinedRoom = null;

    public JoinRoomDialog(JFrame owner, RoomRepository roomRepository, String memberId) {
        super(owner, "방 조인", true);
        this.roomRepository = roomRepository;
        this.memberId = memberId;
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
        root.setBorder(BorderFactory.createEmptyBorder(30, 28, 30, 28));

        JLabel titleLabel = new JLabel("방 조인 ~!");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel codeLabel = new JLabel("방 코드 입력");
        codeLabel.setFont(Theme.FONT_NORMAL);
        codeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        codeInputPanel = new CodeInputPanel();
        codeInputPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("초대 코드를 입력하세요");
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton joinButton = new JButton("참여 !");
        Theme.styleButton(joinButton);
        joinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        joinButton.addActionListener(e -> handleJoin());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(30));
        root.add(codeLabel);
        root.add(Box.createVerticalStrut(10));
        root.add(codeInputPanel);
        root.add(Box.createVerticalStrut(8));
        root.add(hintLabel);
        root.add(Box.createVerticalStrut(30));
        root.add(joinButton);

        setContentPane(root);

        // 다이얼로그가 열리자마자 바로 코드 입력을 시작할 수 있도록 포커스를 줍니다.
        SwingUtilities.invokeLater(codeInputPanel::focusFirstBox);
    }

    /** "참여!" 버튼 클릭 시 실행됩니다. 코드 검증 -> 방 조회 -> 참여 처리 순서로 진행합니다. */
    private void handleJoin() {
        String code = codeInputPanel.getCode();

        if (!codeInputPanel.isComplete()) {
            showWarning("방 코드 4자리를 모두 입력해주세요.");
            return;
        }

        Room existing = roomRepository.findByCode(code);
        if (existing == null) {
            showWarning("존재하지 않는 방 코드입니다.");
            return;
        }
        if (existing.hasMember(memberId)) {
            showWarning("이미 참여 중인 방입니다.");
            return;
        }

        Room result = roomRepository.joinRoom(code, memberId);
        if (result != null) {
            this.joinedRoom = result;
            JOptionPane.showMessageDialog(this,
                    "'" + result.getName() + "' 방에 참여했습니다!",
                    "참여 완료", JOptionPane.INFORMATION_MESSAGE);

            // 방 생성 직후와 마찬가지로, 카테고리에 맞춰 곧바로 날짜(및 장소) 입력 화면을 띄워줍니다.
            if (Room.CATEGORY_PROMISE.equals(result.getCategory())) {
                ScheduleInputDialog scheduleDialog =
                        new ScheduleInputDialog(
                                this, new AvailabilityRepository(), new PlaceRepository(), result, memberId);
                scheduleDialog.setVisible(true);
            } else if (Room.CATEGORY_TRAVEL.equals(result.getCategory())) {
                TravelDateInputDialog travelDialog =
                        new TravelDateInputDialog(this, new TravelDateRepository(), result, memberId);
                travelDialog.setVisible(true);
            }

            dispose();
        } else {
            showWarning("방 참여 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }

    /** 참여에 성공한 Room을 반환합니다. 취소되었거나 실패한 경우 null을 반환합니다. */
    public Room getJoinedRoom() {
        return joinedRoom;
    }
}
