/**
 * FinalDateEditDialog.java
 *
 * 방 메인 화면 맨 아래 "모임 최종 날짜"의 "수정" 버튼(방장 전용)을 눌렀을 때 뜨는 화면입니다.
 * 방 카테고리에 따라 화면이 조금 다릅니다.
 *  - "단체 약속" 방: 날짜 하나만 고르면 됩니다.
 *  - "단체 여행" 방: 여행 시작 날짜와 종료 날짜, 두 개를 고릅니다.
 *
 *   <필드>
 *   1. finalRepository : 최종 확정 정보를 저장하는 저장소 객체
 *   2. room              : 지금 최종 날짜를 정하려는 방 (카테고리에 따라 화면 구성이 달라짐)
 */

package com.groupmeeting.view.schedule;

import com.groupmeeting.view.common.CalendarPanel;
import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Room;
import com.groupmeeting.repository.RoomFinalRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FinalDateEditDialog extends JDialog {

    private final RoomFinalRepository finalRepository;
    private final Room room;
    private final boolean isTravel;

    private CalendarPanel dateCalendar;      // 단체 약속: 날짜 하나
    private CalendarPanel startCalendar;     // 단체 여행: 시작 날짜
    private CalendarPanel endCalendar;       // 단체 여행: 종료 날짜

    public FinalDateEditDialog(Window owner, RoomFinalRepository finalRepository, Room room) {
        super(owner, "모임 최종 날짜 수정", ModalityType.APPLICATION_MODAL);
        this.finalRepository = finalRepository;
        this.room = room;
        this.isTravel = Room.CATEGORY_TRAVEL.equals(room.getCategory());
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

    /** 화면 내부 컴포넌트(제목, 날짜 달력, 저장 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("모임 최종 날짜 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.addActionListener(e -> handleSave());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(18));

        if (isTravel) {
            JLabel startLabel = sectionLabel("여행 시작 날짜");
            startCalendar = new CalendarPanel();
            JLabel endLabel = sectionLabel("여행 종료 날짜");
            endCalendar = new CalendarPanel();

            root.add(startLabel);
            root.add(Box.createVerticalStrut(6));
            root.add(startCalendar);
            root.add(Box.createVerticalStrut(16));
            root.add(endLabel);
            root.add(Box.createVerticalStrut(6));
            root.add(endCalendar);
            root.add(Box.createVerticalStrut(20));
            root.add(saveButton);

            Theme.alignAsCenteredColumn(startLabel, startCalendar, endLabel, endCalendar, saveButton);
        } else {
            JLabel dateLabel = sectionLabel("모임 날짜");
            dateCalendar = new CalendarPanel();

            root.add(dateLabel);
            root.add(Box.createVerticalStrut(6));
            root.add(dateCalendar);
            root.add(Box.createVerticalStrut(20));
            root.add(saveButton);

            Theme.alignAsCenteredColumn(dateLabel, dateCalendar, saveButton);
        }

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_NORMAL);
        label.setForeground(Theme.TEXT_DARK);
        return label;
    }

    /** "저장" 버튼 클릭 시 실행됩니다. 카테고리에 맞는 형식으로 최종 날짜 문자열을 만들어 저장합니다. */
    private void handleSave() {
        if (isTravel) {
            LocalDate start = startCalendar.getSelectedDate();
            LocalDate end = endCalendar.getSelectedDate();

            if (start == null || end == null) {
                showWarning("여행 시작 날짜와 종료 날짜를 모두 선택해주세요.");
                return;
            }
            if (end.isBefore(start)) {
                showWarning("종료 날짜는 시작 날짜보다 빠를 수 없습니다.");
                return;
            }

            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            long nights = days - 1;
            String text = start.getMonthValue() + "월 " + start.getDayOfMonth() + "일 ~ "
                    + end.getMonthValue() + "월 " + end.getDayOfMonth() + "일"
                    + "  (" + nights + "박 " + days + "일)";

            finalRepository.setFinalDate(room.getCode(), text);
        } else {
            LocalDate date = dateCalendar.getSelectedDate();
            if (date == null) {
                showWarning("날짜를 선택해주세요.");
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
            finalRepository.setFinalDate(room.getCode(), date.format(formatter));
        }

        dispose();
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }
}
