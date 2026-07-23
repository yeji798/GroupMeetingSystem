/**
 * MyScheduleEditDialog.java
 *
 * 방 메인 화면의 "자신의 일정 수정" 버튼을 눌렀을 때 뜨는 화면입니다.
 * ScheduleInputDialog(방 생성 직후 처음 입력하는 화면)와 생김새는 비슷하지만,
 * "새로 입력"이 아니라 "이미 입력해둔 내용을 고치는" 용도라서 아래 부분이 다릅니다.
 *
 *   1. 창을 열면 예전에 저장해두었던 나의 시간 목록을 CSV에서 불러와 미리 보여준다.
 *   2. "장소 추천" 입력칸이 없다. (장소는 이 화면의 목적이 아니므로 제외)
 *   3. 시간리스트의 각 항목마다 "수정" / "삭제" 버튼이 있어서 개별적으로 고치거나 지울 수 있다.
 *   4. 같은 날짜에 시간이 겹치는 항목은 등록할 수 없다. (겹치면 경고만 띄우고 추가하지 않음)
 *   5. 맨 아래 "저장" 버튼을 눌러야 비로소 CSV 파일에 최종 반영된다. (그 전까지는 화면 안에서만 수정 중인 상태)
 *
 *   <필드>
 *   1. repository   : 가능 시간 CSV를 읽고 쓰는 저장소 객체
 *   2. room          : 지금 일정을 수정 중인 방
 *   3. memberId      : 지금 로그인해서 수정 중인 사용자의 아이디
 *   4. savedEntries  : 화면에서 지금 편집 중인 시간 목록 (아직 CSV에 저장되지 않은 "임시 작업본")
 *
 *   <생성자>
 *   : 창을 만들고, CSV에서 이 사람이 예전에 저장해둔 항목들을 불러와 시간리스트에 미리 채워 넣음
 *
 *   <중요 메소드>
 *   1. handleAddEntry()   : "추가" 버튼 -> 겹치는지 검사 후 시간리스트에 새 항목 추가
 *   2. handleEditEntry()  : 목록의 "수정" 버튼 -> 그 항목 값을 달력/시간선택에 다시 채워주고 목록에서는 제거
 *                            (사용자가 값을 고친 뒤 "추가"를 다시 누르면 고친 내용으로 다시 들어감)
 *   3. handleDeleteEntry(): 목록의 "삭제" 버튼 -> 그 항목을 목록에서 제거
 *   4. isOverlapping()    : 새로 추가하려는 시간이 같은 날짜의 기존 항목과 겹치는지 검사
 *   5. handleFinalSave()  : "저장" 버튼 -> 지금 목록 전체를 CSV에 최종 반영 (replaceForMember 사용)
 */

package com.groupmeeting.view.schedule;

import com.groupmeeting.view.common.CalendarPanel;
import com.groupmeeting.view.common.Theme;
import com.groupmeeting.view.common.TimePickerPanel;

import com.groupmeeting.model.AvailabilityEntry;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.AvailabilityRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MyScheduleEditDialog extends JDialog {

    private final AvailabilityRepository repository;
    private final Room room;
    private final String memberId;

    // 화면에서 지금 편집 중인 시간 목록. "저장" 버튼을 눌러야 이 내용이 CSV에 실제로 반영된다.
    private final List<AvailabilityEntry> savedEntries = new ArrayList<>();

    private CalendarPanel calendarPanel;
    private TimePickerPanel startTimePanel;
    private TimePickerPanel endTimePanel;
    private JPanel entriesPanel;
    private JScrollPane entriesScrollPane; // entriesPanel을 감싸는 가로 스크롤 영역

    public MyScheduleEditDialog(Window owner, AvailabilityRepository repository, Room room, String memberId) {
        super(owner, "자신의 일정 수정", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.room = room;
        this.memberId = memberId;
        // 예전에 저장해둔 내용을 화면을 만들기 "전에" 미리 불러옵니다. 그래야 initComponents()
        // 안에서 시간리스트의 실제 크기에 맞춰 가로 스크롤 영역의 높이를 정확히 계산할 수 있습니다.
        // (화면을 먼저 만들고 나중에 데이터를 채우면, 스크롤 영역 높이가 빈 목록 기준으로
        //  고정되어버려서 나중에 채운 내용이 잘려 보이는 문제가 생깁니다)
        savedEntries.addAll(repository.getForRoomAndMember(room.getCode(), memberId));
        initDialog();
        initComponents();
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768); // 다른 화면들과 동일한 창 크기(9:16 비율)
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(달력, 시간 선택, 시간리스트, 버튼 등)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 자신의 일정 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dateLabel = sectionLabel("날짜 선택");
        calendarPanel = new CalendarPanel();

        JLabel timeLabel = sectionLabel("시간 선택");

        startTimePanel = new TimePickerPanel("시작");
        endTimePanel = new TimePickerPanel("끝");

        // 시작 시간과 끝 시간을 좌우로 나란히 배치 (기존 ScheduleInputDialog와 동일한 방식)
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 8, 0));
        timeRow.setOpaque(false);
        timeRow.add(startTimePanel);
        timeRow.add(endTimePanel);

        // "추가" 버튼: 지금 선택된 날짜/시간을 시간리스트에 새 항목으로 넣는다. (이 시점엔 아직 CSV에 저장 안 됨)
        // -> 다른 줄들과 폭을 맞추지 않고, 작은 크기로 화면 가운데에 독립적으로 배치합니다.
        JButton addButton = new JButton("추가");
        Theme.styleSecondaryButton(addButton);
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(120, 34));
        addButton.addActionListener(e -> handleAddEntry());

        JLabel listLabel = sectionLabel("시간리스트");
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        renderEntries(); // savedEntries는 생성자에서 이미 채워져 있으므로, 여기서 바로 실제 내용을 그림

        // "저장" 버튼: 지금 시간리스트에 있는 내용을 CSV에 최종 반영한다. (이 화면의 진짜 "저장" 동작)
        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.addActionListener(e -> handleFinalSave());

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
        root.add(addButton);
        root.add(Box.createVerticalStrut(16));
        root.add(listLabel);
        root.add(Box.createVerticalStrut(6));
        entriesScrollPane = Theme.wrapHorizontalScrollable(entriesPanel, Theme.STANDARD_CONTENT_WIDTH);
        root.add(entriesScrollPane);
        root.add(Box.createVerticalStrut(20));
        root.add(saveButton);

        // 달력/시간선택/시간리스트/저장 버튼의 가로 폭을 통일해서 화면 가운데로 나란히 정렬합니다.
        // ("추가" 버튼은 일부러 작게 유지하기 위해 이 정렬 대상에서 제외했습니다. 시간리스트는
        //  글자가 길어도 잘리지 않도록 가로 스크롤 영역(entriesScroll)으로 감싼 뒤 정렬합니다)
        Theme.alignAsCenteredColumn(dateLabel, calendarPanel, timeLabel, timeRow, listLabel, entriesScrollPane, saveButton);

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
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * "추가" 버튼 클릭 시 실행됩니다.
     * 날짜/시간 값 검증 -> 기존 항목과 겹치는지 검사 -> 문제없으면 시간리스트에 새 항목을 추가합니다.
     */
    private void handleAddEntry() {
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

        String dateStr = date.toString();
        if (isOverlapping(dateStr, start, end)) {
            // -> 요구사항: 시간리스트에는 겹치는 내용이 있으면 안 되므로, 겹치면 추가하지 않고 경고만 띄운다.
            showWarning("이미 등록한 시간대와 겹칩니다. 다른 시간을 선택해주세요.");
            return;
        }

        savedEntries.add(new AvailabilityEntry(room.getCode(), memberId, dateStr, start, end));
        renderEntries();
    }

    /**
     * 새로 추가하려는 시간(newDate, newStart, newEnd)이 이미 목록에 있는 항목과 겹치는지 확인합니다.
     * 겹침 판정: 같은 날짜이면서, "새 시작 < 기존 끝" 이고 "기존 시작 < 새 끝" 이면 겹치는 것으로 본다.
     * (HH:mm 형식은 앞자리가 0으로 채워진 문자열이라 문자열 비교로도 시간 순서 비교가 정확히 됩니다.)
     */
    private boolean isOverlapping(String newDate, String newStart, String newEnd) {
        for (AvailabilityEntry entry : savedEntries) {
            if (!entry.getDate().equals(newDate)) {
                continue; // 날짜가 다르면애초에 겹칠 수 없음
            }
            boolean overlap = newStart.compareTo(entry.getEndTime()) < 0
                    && entry.getStartTime().compareTo(newEnd) < 0;
            if (overlap) {
                return true;
            }
        }
        return false;
    }

    /** 시간리스트 영역을 현재 savedEntries 기준으로 다시 그립니다. */
    private void renderEntries() {
        entriesPanel.removeAll();

        if (savedEntries.isEmpty()) {
            JLabel emptyLabel = new JLabel("등록된 시간이 없습니다.");
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

        // 이미 가로 스크롤 영역으로 감싼 뒤(=항목을 추가/수정/삭제해서 다시 그리는 경우)라면,
        // 새로 바뀐 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (entriesScrollPane != null) {
            Theme.resyncScrollableHeight(entriesScrollPane, entriesPanel);
            revalidate();
            repaint();
        }
    }

    /** 시간리스트의 항목 한 줄(날짜/시간 텍스트 + "수정" 버튼 + "삭제" 버튼)을 만듭니다. */
    private JPanel createEntryRow(int index) {
        AvailabilityEntry entry = savedEntries.get(index);
        LocalDate date = LocalDate.parse(entry.getDate());
        String text = date.getMonthValue() + "/" + date.getDayOfMonth()
                + "   " + entry.getStartTime() + " ~ " + entry.getEndTime();

        JPanel row = new JPanel(new BorderLayout(6, 0));
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

        // "수정" / "삭제" 버튼을 한 줄에 나란히 배치
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonGroup.setOpaque(false);

        JButton editButton = new JButton("수정");
        editButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        editButton.setForeground(Theme.PRIMARY_GREEN_DARK);
        editButton.setBorderPainted(false);
        editButton.setContentAreaFilled(false);
        editButton.setFocusPainted(false);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> handleEditEntry(index));

        JButton deleteButton = new JButton("삭제");
        deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        deleteButton.setForeground(new Color(0x99, 0x99, 0x99));
        deleteButton.setBorderPainted(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeleteEntry(index));

        buttonGroup.add(editButton);
        buttonGroup.add(deleteButton);

        row.add(textLabel, BorderLayout.CENTER);
        row.add(buttonGroup, BorderLayout.EAST);
        return row;
    }

    /**
     * 목록의 "수정" 버튼 클릭 시 실행됩니다.
     * -> 이 항목의 날짜/시간 값을 위쪽 달력과 시간 선택 영역에 다시 채워 넣고,
     *    목록에서는 일단 제거합니다. (사용자가 값을 원하는 대로 고친 뒤 "추가"를 다시 누르면
     *    고친 내용이 새 항목으로 다시 들어가는 방식 - 겹침 검사도 자연스럽게 그대로 적용됩니다.)
     */
    private void handleEditEntry(int index) {
        AvailabilityEntry entry = savedEntries.get(index);

        calendarPanel.selectDate(LocalDate.parse(entry.getDate()));
        setPickerTime(startTimePanel, entry.getStartTime());
        setPickerTime(endTimePanel, entry.getEndTime());

        savedEntries.remove(index);
        renderEntries();
    }

    /** "HH:mm" 형식의 문자열을 시/분으로 나누어 TimePickerPanel에 반영하는 보조 메서드입니다. */
    private void setPickerTime(TimePickerPanel picker, String hhmm) {
        String[] parts = hhmm.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        picker.setTime24(hour, minute);
    }

    /** 목록의 "삭제" 버튼 클릭 시 실행됩니다. 해당 항목을 목록에서 제거합니다. */
    private void handleDeleteEntry(int index) {
        savedEntries.remove(index);
        renderEntries();
    }

    /**
     * "저장" 버튼 클릭 시 실행됩니다.
     * 지금 화면의 시간리스트(savedEntries) 전체로 CSV에 저장된 이 사람의 기존 기록을 덮어씁니다.
     */
    private void handleFinalSave() {
        boolean saved = repository.replaceForMember(room.getCode(), memberId, savedEntries);
        if (saved) {
            JOptionPane.showMessageDialog(this,
                    "일정이 저장되었습니다!",
                    "저장 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            showWarning("저장 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }
}
