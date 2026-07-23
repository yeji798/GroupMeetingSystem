/**
 * AddRoundDialog.java
 *
 * "모임 차수 추가" 버튼(방장 전용)을 눌렀을 때 뜨는 화면입니다.
 * 차수 모임 이름(예: "1차 모임", "뒤풀이")을 입력받아 새 차수를 만듭니다.
 * "추가" 버튼을 누르면 이 방의 모든 참여자가 보는 차수 목록에 즉시 나타나게 됩니다.
 *
 *   <필드>
 *   1. repository : 모임 차수 CSV를 읽고 쓰는 저장소 객체
 *   2. room        : 지금 차수를 추가하려는 방
 *
 *   <중요 메소드>
 *   1. handleAdd() : "추가" 버튼 -> 입력값 검증 후 새 차수를 저장하고 창을 닫음
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.util.MeetingRoundRepository;

import javax.swing.*;
import java.awt.*;

public class AddRoundDialog extends JDialog {

    private final MeetingRoundRepository repository;
    private final Room room;

    private JTextField nameField;
    private boolean added = false; // "추가"에 성공했는지 여부 (호출한 쪽에서 목록 새로고침 여부 판단용)

    public AddRoundDialog(Window owner, MeetingRoundRepository repository, Room room) {
        super(owner, "모임 차수 추가", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
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

    /** 화면 내부 컴포넌트(제목, 차수 이름 입력칸, 추가 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("모임 차수 추가");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("차수 모임 이름");
        nameLabel.setFont(Theme.FONT_NORMAL);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField = new JTextField();
        Theme.styleTextField(nameField);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameField.addActionListener(e -> handleAdd()); // Enter 키로도 추가되도록 함

        JLabel hintLabel = new JLabel("예) 1차 모임, 2차 모임, 뒤풀이");
        hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addButton = new JButton("추가");
        Theme.styleButton(addButton);
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        addButton.addActionListener(e -> handleAdd());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(24));
        root.add(nameLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(nameField);
        root.add(Box.createVerticalStrut(6));
        root.add(hintLabel);
        root.add(Box.createVerticalStrut(24));
        root.add(addButton);

        setContentPane(root);
    }

    /** "추가" 버튼 클릭 시 실행됩니다. 이름이 비어있지 않으면 새 차수를 저장합니다. */
    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "차수 모임 이름을 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        repository.addRound(room.getCode(), name);
        added = true;
        dispose();
    }

    /** "추가"를 눌러서 실제로 새 차수가 만들어졌는지 여부를 반환합니다. */
    public boolean isAdded() {
        return added;
    }
}
