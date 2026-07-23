package com.groupmeeting.view;

import com.groupmeeting.model.AvailabilityEntry;
import com.groupmeeting.model.Room;
import com.groupmeeting.util.AvailabilityRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * "단체 약속" 방에서 참여자가 자신의 가능 날짜/시간과 추천 장소를 입력하는 모달 다이얼로그입니다.
 *
 * 흐름: 달력에서 날짜 선택 -> 시작/끝 시간 선택 -> "저장" (여러 번 반복 가능, 시간리스트에 누적)
 *       -> 장소 추천 입력 -> "제출" -> 누적된 항목들을 availability.csv에 한 번에 저장
 */
public class ScheduleInputDialog extends JDialog {

    private final AvailabilityRepository repository;
    private final Room room;
    private final String memberId;

    private final List<AvailabilityEntry> savedEntries = new ArrayList<>();

    private CalendarPanel calendarPanel;
    private TimePickerPanel startTimePanel;
    private TimePickerPanel endTimePanel;
    private JTextField placeField;
    private JPanel entriesPanel;

    public ScheduleInputDialog(Window owner, AvailabilityRepository repository, Room room, String memberId) {
        super(owner, "날짜 및 시간 선택", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.room = room;
        this.memberId = memberId;
        initDialog();
        initComponents();
    }

    private void initDialog() {
        setSize(360, 640);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName());
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dateLabel = sectionLabel("날짜 선택");
        calendarPanel = new CalendarPanel();
        calendarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLabel = sectionLabel("시간 선택");

        startTimePanel = new TimePickerPanel("시작");
        endTimePanel = new TimePickerPanel("끝");

        JPanel timeRow = new JPanel(new GridLayout(1, 2, 8, 0));
        timeRow.setOpaque(false);
        timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeRow.add(startTimePanel);
        timeRow.add(endTimePanel);

        JButton saveButton = new JButton("저장");
        Theme.styleSecondaryButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(120, 34));
        saveButton.addActionListener(e -> handleSave());

        JLabel placeLabel = sectionLabel("장소 추천");
        placeField = new JTextField();
        Theme.styleTextField(placeField);
        placeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        placeField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel listLabel = sectionLabel("시간리스트");
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        entriesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton submitButton = new JButton("제출");
        Theme.styleButton(submitButton);
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        submitButton.addActionListener(e -> handleSubmit());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(dateLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(calendarPanel);
        root.add(Box.createVerticalStrut(16));
        root.add(timeLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(timeRow);
        root.add(Box.createVerticalStrut(8));
        root.add(saveButton);
        root.add(Box.createVerticalStrut(16));
        root.add(placeLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(placeField);
        root.add(Box.createVerticalStrut(16));
        root.add(listLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(entriesPanel);
        root.add(Box.createVerticalStrut(20));
        root.add(submitButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);

        setContentPane(scrollPane);

        renderEntries();
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_NORMAL);
        label.setForeground(Theme.TEXT_DARK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** "저장" 버튼 클릭: 날짜/시간 유효성 확인 후 시간리스트에 항목을 추가합니다. */
    private void handleSave() {
        LocalDate date = calendarPanel.getSelectedDate();
        if (date == null) {
            showWarning("날짜를 선택해주세요.");
            return;
        }

        String start = startTimePanel.get24HourTime();
        String end = endTimePanel.get24HourTime();
        if (end.compareTo(start) <= 0) {
            showWarning("끝나는 시간은 시작 시간보다 늦어야 합니다.");
            return;
        }

        savedEntries.add(new AvailabilityEntry(room.getCode(), memberId, date.toString(), start, end, ""));
        renderEntries();
    }

    /** 시간리스트 영역을 현재 savedEntries 기준으로 다시 그립니다. */
    private void renderEntries() {
        entriesPanel.removeAll();

        if (savedEntries.isEmpty()) {
            JLabel emptyLabel = new JLabel("저장된 시간이 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            entriesPanel.add(emptyLabel);
        } else {
            for (int i = 0; i < savedEntries.size(); i++) {
                entriesPanel.add(createEntryRow(i));
                entriesPanel.add(Box.createVerticalStrut(6));
            }
        }

        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

    private JPanel createEntryRow(int index) {
        AvailabilityEntry entry = savedEntries.get(index);
        LocalDate date = LocalDate.parse(entry.getDate());
        String text = date.getMonthValue() + "/" + date.getDayOfMonth()
                + "   " + entry.getStartTime() + " ~ " + entry.getEndTime();

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(Theme.FONT_NORMAL);
        textLabel.setForeground(Theme.TEXT_DARK);

        JButton deleteButton = new JButton("삭제");
        deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        deleteButton.setForeground(new Color(0x99, 0x99, 0x99));
        deleteButton.setBorderPainted(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> {
            savedEntries.remove(index);
            renderEntries();
        });

        row.add(textLabel, BorderLayout.CENTER);
        row.add(deleteButton, BorderLayout.EAST);
        return row;
    }

    /** "제출" 버튼 클릭: 시간리스트에 장소를 채워 CSV 파일에 저장합니다. */
    private void handleSubmit() {
        if (savedEntries.isEmpty()) {
            showWarning("저장된 시간이 없습니다. 먼저 날짜와 시간을 저장해주세요.");
            return;
        }

        String place = placeField.getText().trim();
        for (AvailabilityEntry entry : savedEntries) {
            entry.setPlace(place);
        }

        boolean saved = repository.appendEntries(savedEntries);
        if (saved) {
            JOptionPane.showMessageDialog(this,
                    "가능한 날짜/시간과 장소가 제출되었습니다!",
                    "제출 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            showWarning("제출 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }
}
