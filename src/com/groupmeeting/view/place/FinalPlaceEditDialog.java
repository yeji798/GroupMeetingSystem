/**
 * FinalPlaceEditDialog.java
 *
 * 방 메인 화면 맨 아래 "모임 최종 장소"의 "수정" 버튼(방장 전용)을 눌렀을 때 뜨는 화면입니다.
 * 장소 이름을 직접 입력해서 방의 최종 장소로 저장합니다.
 *
 *   <필드>
 *   1. finalRepository : 최종 확정 정보를 저장하는 저장소 객체
 *   2. room              : 지금 최종 장소를 정하려는 방
 */

package com.groupmeeting.view.place;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Room;
import com.groupmeeting.repository.RoomFinalRepository;

import javax.swing.*;
import java.awt.*;

public class FinalPlaceEditDialog extends JDialog {

    private final RoomFinalRepository finalRepository;
    private final Room room;

    private JTextField placeField;

    public FinalPlaceEditDialog(Window owner, RoomFinalRepository finalRepository, Room room, String currentPlace) {
        super(owner, "모임 최종 장소 수정", ModalityType.APPLICATION_MODAL);
        this.finalRepository = finalRepository;
        this.room = room;
        initDialog();
        initComponents(currentPlace);
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(제목, 장소 입력칸, 저장 버튼)를 배치합니다. */
    private void initComponents(String currentPlace) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("모임 최종 장소 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("최종 장소");
        label.setFont(Theme.FONT_NORMAL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        placeField = new JTextField(currentPlace);
        Theme.styleTextField(placeField);
        placeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        placeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        placeField.addActionListener(e -> handleSave());

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveButton.addActionListener(e -> handleSave());
        Theme.centerAtStandardWidth(label, placeField, saveButton);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(24));
        root.add(label);
        root.add(Box.createVerticalStrut(6));
        root.add(placeField);
        root.add(Box.createVerticalStrut(24));
        root.add(saveButton);

        setContentPane(root);
    }

    /** "저장" 버튼 클릭 시 실행됩니다. 입력한 장소를 방의 최종 장소로 저장합니다. */
    private void handleSave() {
        String place = placeField.getText().trim();
        if (place.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "장소를 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        finalRepository.setFinalPlace(room.getCode(), place);
        dispose();
    }
}
