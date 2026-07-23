/**
 * FinalTimeEditDialog.java
 *
 * 방 메인 화면 맨 아래 "모임 최종 시간"의 "수정" 버튼(방장 전용)을 눌렀을 때 뜨는 화면입니다.
 * (단체 약속 방 전용 - 단체 여행 방은 시간 개념이 없습니다)
 * 시작 시간과 끝 시간을 직접 골라서 방의 최종 시간으로 저장합니다.
 *
 *   <필드>
 *   1. finalRepository : 최종 확정 정보를 저장하는 저장소 객체
 *   2. room              : 지금 최종 시간을 정하려는 방
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.util.RoomFinalRepository;

import javax.swing.*;
import java.awt.*;

public class FinalTimeEditDialog extends JDialog {

    private final RoomFinalRepository finalRepository;
    private final Room room;

    private TimePickerPanel startTimePanel;
    private TimePickerPanel endTimePanel;

    public FinalTimeEditDialog(Window owner, RoomFinalRepository finalRepository, Room room) {
        super(owner, "모임 최종 시간 수정", ModalityType.APPLICATION_MODAL);
        this.finalRepository = finalRepository;
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

    /** 화면 내부 컴포넌트(제목, 시작/끝 시간 선택, 저장 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("모임 최종 시간 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        startTimePanel = new TimePickerPanel("시작");
        endTimePanel = new TimePickerPanel("끝");

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.addActionListener(e -> handleSave());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(20));
        root.add(startTimePanel);
        root.add(Box.createVerticalStrut(12));
        root.add(endTimePanel);
        root.add(Box.createVerticalStrut(20));
        root.add(saveButton);

        Theme.alignAsCenteredColumn(startTimePanel, endTimePanel, saveButton);

        setContentPane(root);
    }

    /** "저장" 버튼 클릭 시 실행됩니다. 끝 시간이 시작 시간보다 늦은지 확인한 뒤 저장합니다. */
    private void handleSave() {
        String start = startTimePanel.get24HourTime();
        String end = endTimePanel.get24HourTime();

        if (end.compareTo(start) <= 0) {
            JOptionPane.showMessageDialog(this,
                    "끝나는 시간은 시작 시간보다 늦어야 합니다.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        finalRepository.setFinalTime(room.getCode(), start, end);
        dispose();
    }
}
