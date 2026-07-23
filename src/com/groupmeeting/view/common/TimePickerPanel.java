package com.groupmeeting.view.common;

import javax.swing.*;
import java.awt.*;

/**
 * "오전/오후 선택 + 시(1~12)/분(0~59) 스크롤 선택"으로 시간 하나를 입력받는 재사용 컴포넌트입니다.
 * ScheduleInputDialog에서 시작 시간, 끝 시간 입력에 각각 하나씩 사용합니다.
 */
public class TimePickerPanel extends JPanel {

    private JToggleButton amButton;
    private JToggleButton pmButton;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;

    public TimePickerPanel(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_NORMAL);
        titleLabel.setForeground(Theme.TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 오전/오후 토글 버튼
        amButton = new JToggleButton("오전", true);
        pmButton = new JToggleButton("오후", false);
        styleToggle(amButton);
        styleToggle(pmButton);

        ButtonGroup group = new ButtonGroup();
        group.add(amButton);
        group.add(pmButton);

        JPanel ampmRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        ampmRow.setOpaque(false);
        ampmRow.add(amButton);
        ampmRow.add(pmButton);
        ampmRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 시 : 분 스피너
        hourSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 12, 1));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        styleSpinner(hourSpinner);
        styleSpinner(minuteSpinner);

        JLabel colon = new JLabel(":");
        colon.setFont(Theme.FONT_SUBTITLE);

        JPanel spinnerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        spinnerRow.setOpaque(false);
        spinnerRow.add(hourSpinner);
        spinnerRow.add(colon);
        spinnerRow.add(minuteSpinner);
        spinnerRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(ampmRow);
        add(Box.createVerticalStrut(4));
        add(spinnerRow);
    }

    private void styleToggle(JToggleButton button) {
        button.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBackground(Theme.PANEL_BACKGROUND);
        button.setBorder(BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(Theme.FONT_NORMAL);
        spinner.setPreferredSize(new Dimension(52, 30));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setEditable(false);
    }

    /**
     * 24시간제 시(hour24, 0~23)와 분(minute)을 받아서 오전/오후 버튼과 시:분 스피너에 반영합니다.
     * -> 기존에 저장해둔 시간을 "수정" 화면에 불러와 보여줄 때 사용합니다. (get24HourTime()의 반대 동작)
     */
    public void setTime24(int hour24, int minute) {
        boolean isAM = hour24 < 12;

        // 24시간제 -> 12시간제 변환: 0시는 오전 12시, 13시는 오후 1시가 되도록 계산
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }

        amButton.setSelected(isAM);
        pmButton.setSelected(!isAM);
        hourSpinner.setValue(hour12);
        minuteSpinner.setValue(minute);
    }

    /** 오전 12시=00시, 오후 12시=12시 규칙에 따라 24시간제 "HH:mm" 문자열로 변환합니다. */
    public String get24HourTime() {
        int hour12 = (Integer) hourSpinner.getValue();
        int minute = (Integer) minuteSpinner.getValue();
        boolean isAM = amButton.isSelected();

        int hour24;
        if (isAM) {
            hour24 = (hour12 == 12) ? 0 : hour12;
        } else {
            hour24 = (hour12 == 12) ? 12 : hour12 + 12;
        }

        return String.format("%02d:%02d", hour24, minute);
    }
}
