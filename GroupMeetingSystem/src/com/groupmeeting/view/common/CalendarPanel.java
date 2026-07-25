package com.groupmeeting.view.common;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 날짜 하나를 클릭으로 선택할 수 있는 간단한 월간 달력 컴포넌트입니다.
 * ScheduleInputDialog의 "날짜 선택" 영역에서 사용합니다.
 *
 * 동작 방식:
 *  - 상단의 ‹ / › 버튼으로 이전/다음 달로 이동
 *  - 날짜 버튼을 클릭하면 해당 날짜가 선택되어 초록색으로 강조 표시됨
 */
public class CalendarPanel extends JPanel {

    private static final String[] WEEKDAY_NAMES = {"일", "월", "화", "수", "목", "금", "토"};

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;

    private JLabel monthLabel;
    private JPanel daysPanel;

    public CalendarPanel() {
        this.currentYearMonth = YearMonth.now();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.PANEL_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        add(buildHeader());
        add(Box.createVerticalStrut(6));
        add(buildWeekdayHeader());

        daysPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        daysPanel.setOpaque(false);
        add(daysPanel);

        renderDays();
    }

    /** 이전달 버튼 + 월 라벨 + 다음달 버튼 */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton prevButton = createNavButton("‹"); // ‹
        prevButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            renderDays();
        });

        JButton nextButton = createNavButton("›"); // ›
        nextButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            renderDays();
        });

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(Theme.FONT_SUBTITLE);
        monthLabel.setForeground(Theme.TEXT_DARK);

        header.add(prevButton, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextButton, BorderLayout.EAST);
        return header;
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        button.setForeground(Theme.PRIMARY_GREEN_DARK);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** 요일(일~토) 헤더 행 */
    private JPanel buildWeekdayHeader() {
        JPanel row = new JPanel(new GridLayout(1, 7));
        row.setOpaque(false);
        for (String name : WEEKDAY_NAMES) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            label.setForeground(new Color(0x99, 0x99, 0x99));
            row.add(label);
        }
        return row;
    }

    /** 현재 currentYearMonth 기준으로 날짜 그리드를 다시 그립니다. */
    private void renderDays() {
        daysPanel.removeAll();
        monthLabel.setText(currentYearMonth.getYear() + "년 " + currentYearMonth.getMonthValue() + "월");

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        // ISO 기준 월=1 ~ 일=7 이므로, 일요일이 첫 칸이 되도록 7로 나눈 나머지만큼 빈 칸을 채움
        int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < leadingBlanks; i++) {
            daysPanel.add(new JLabel(""));
        }

        int lengthOfMonth = currentYearMonth.lengthOfMonth();
        for (int day = 1; day <= lengthOfMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            daysPanel.add(createDayButton(date));
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    private JButton createDayButton(LocalDate date) {
        JButton dayButton = new JButton(String.valueOf(date.getDayOfMonth()));
        dayButton.setFont(Theme.FONT_NORMAL);
        dayButton.setFocusPainted(false);
        dayButton.setBorderPainted(false);
        dayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        boolean selected = date.equals(selectedDate);
        dayButton.setOpaque(selected);
        dayButton.setContentAreaFilled(selected);
        dayButton.setBackground(selected ? Theme.PRIMARY_GREEN : Theme.PANEL_BACKGROUND);
        dayButton.setForeground(selected ? Color.WHITE : Theme.TEXT_DARK);

        dayButton.addActionListener(e -> {
            selectedDate = date;
            renderDays();
        });

        return dayButton;
    }

    /** 사용자가 선택한 날짜를 반환합니다. 선택하지 않았으면 null을 반환합니다. */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * 특정 날짜를 화면에 미리 선택된 상태로 표시합니다. (수정 화면에서 기존 값을 불러올 때 사용)
     * -> 달력을 그 날짜가 속한 달로 이동시키고, 해당 날짜를 선택된 것으로 표시합니다.
     */
    public void selectDate(LocalDate date) {
        if (date == null) return;
        this.currentYearMonth = YearMonth.from(date); // 그 날짜가 속한 연/월로 달력을 이동
        this.selectedDate = date;                     // 선택 상태로 표시
        renderDays();                                  // 화면을 다시 그려서 반영
    }
}
