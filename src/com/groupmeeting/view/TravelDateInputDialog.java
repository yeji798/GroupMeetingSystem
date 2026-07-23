/**
 * TravelDateInputDialog.java
 *
 * "단체 여행" 방에 참여했을 때(방 생성 직후 또는 방 조인 직후) 뜨는 화면입니다.
 * "단체 약속"의 ScheduleInputDialog와 구조가 비슷하지만, 시간 요소 없이 날짜만 다룹니다.
 *
 *   1. 시작 날짜/종료 날짜를 달력에서 고른 뒤 "저장"을 누르면 "날짜리스트"에 쌓인다.
 *      (여러 번 눌러서 여러 개의 여행 날짜 후보를 만들 수 있음)
 *   2. 같은 사람이 등록한 날짜끼리는 겹치면 안 되므로, 겹치는 날짜를 저장하려 하면 경고만 띄운다.
 *   3. 맨 아래 "제출" 버튼을 눌러야 날짜리스트의 내용이 CSV에 실제로 저장된다.
 *
 *   <필드>
 *   1. repository    : 여행 날짜 CSV를 읽고 쓰는 저장소 객체
 *   2. room           : 지금 여행 날짜를 제안하는 방
 *   3. memberId       : 지금 로그인해서 제안하는 사용자의 아이디
 *   4. savedEntries   : 화면에서 지금 쌓고 있는 날짜 목록 (아직 CSV에 저장되지 않은 "임시 목록")
 *
 *   <중요 메소드>
 *   1. handleSave()   : "저장" 버튼 -> 겹치는지 검사 후 날짜리스트에 새 항목 추가
 *   2. isOverlapping(): 새로 추가하려는 날짜 구간이 이미 목록에 있는 것과 겹치는지 검사
 *   3. handleSubmit() : "제출" 버튼 -> 날짜리스트 전체를 CSV에 저장
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.model.TravelDateEntry;
import com.groupmeeting.util.TravelDateRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TravelDateInputDialog extends JDialog {

    private final TravelDateRepository repository;
    private final Room room;
    private final String memberId;

    private final List<TravelDateEntry> savedEntries = new ArrayList<>();

    private CalendarPanel startCalendar;
    private CalendarPanel endCalendar;
    private JPanel entriesPanel;
    private JScrollPane entriesScrollPane; // entriesPanel을 감싸는 가로 스크롤 영역

    public TravelDateInputDialog(Window owner, TravelDateRepository repository, Room room, String memberId) {
        super(owner, "여행 날짜 선택", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.room = room;
        this.memberId = memberId;
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

    /** 화면 내부 컴포넌트(제목, 시작/종료 날짜 달력, 저장 버튼, 날짜리스트, 제출 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName());
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel startLabel = sectionLabel("여행 시작 날짜");
        startCalendar = new CalendarPanel();

        JLabel endLabel = sectionLabel("여행 종료 날짜");
        endCalendar = new CalendarPanel();

        JButton saveButton = new JButton("저장");
        Theme.styleSecondaryButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(120, 34));
        saveButton.addActionListener(e -> handleSave());

        JLabel listLabel = sectionLabel("날짜리스트");
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);

        JButton submitButton = new JButton("제출");
        Theme.styleButton(submitButton);
        submitButton.addActionListener(e -> handleSubmit());

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(startLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(startCalendar);
        root.add(Box.createVerticalStrut(16));
        root.add(endLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(endCalendar);
        root.add(Box.createVerticalStrut(8));
        root.add(saveButton);
        root.add(Box.createVerticalStrut(16));
        root.add(listLabel);
        root.add(Box.createVerticalStrut(6));
        renderEntries(); // 목록을 먼저 그린 뒤에 폭을 맞춰야 함 (비어있는 상태로 폭이 고정되는 것을 방지)
        // 날짜리스트 글자가 길어도(예: "N박 M일") 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        entriesScrollPane = Theme.wrapHorizontalScrollable(entriesPanel, Theme.STANDARD_CONTENT_WIDTH);
        root.add(entriesScrollPane);
        root.add(Box.createVerticalStrut(20));
        root.add(submitButton);

        Theme.alignAsCenteredColumn(startLabel, startCalendar, endLabel, endCalendar, listLabel, entriesScrollPane, submitButton);

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

    /** "저장" 버튼 클릭 시 실행됩니다. 날짜 검증 -> 겹침 검사 -> 문제없으면 날짜리스트에 추가합니다. */
    private void handleSave() {
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
        if (isOverlapping(start, end)) {
            showWarning("이미 등록한 날짜와 겹칩니다. 다른 날짜를 선택해주세요.");
            return;
        }

        savedEntries.add(new TravelDateEntry(room.getCode(), memberId, start.toString(), end.toString()));
        renderEntries();
    }

    /**
     * 새로 추가하려는 날짜 구간(newStart~newEnd)이 이미 목록에 있는 구간과 겹치는지 확인합니다.
     * 날짜 구간은 시작일과 종료일을 포함하므로, "새 시작 <= 기존 종료"이고 "기존 시작 <= 새 종료"이면
     * 겹치는 것으로 판단합니다. (예: 7/1~7/3 과 7/3~7/5 는 7/3이 겹치므로 겹침으로 처리)
     */
    private boolean isOverlapping(LocalDate newStart, LocalDate newEnd) {
        for (TravelDateEntry entry : savedEntries) {
            LocalDate existingStart = LocalDate.parse(entry.getStartDate());
            LocalDate existingEnd = LocalDate.parse(entry.getEndDate());
            boolean overlap = !newStart.isAfter(existingEnd) && !existingStart.isAfter(newEnd);
            if (overlap) {
                return true;
            }
        }
        return false;
    }

    /** 날짜리스트 영역을 현재 savedEntries 기준으로 다시 그립니다. */
    private void renderEntries() {
        entriesPanel.removeAll();

        if (savedEntries.isEmpty()) {
            JLabel emptyLabel = new JLabel("저장된 날짜가 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            entriesPanel.add(emptyLabel);
        } else {
            for (int i = 0; i < savedEntries.size(); i++) {
                entriesPanel.add(createEntryRow(i));
                entriesPanel.add(Box.createVerticalStrut(6));
            }
        }

        entriesPanel.revalidate();
        entriesPanel.repaint();

        // 이미 가로 스크롤 영역으로 감싼 뒤(=항목을 추가/삭제해서 다시 그리는 경우)라면,
        // 새로 바뀐 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (entriesScrollPane != null) {
            Theme.resyncScrollableHeight(entriesScrollPane, entriesPanel);
            revalidate();
            repaint();
        }
    }

    /** 날짜리스트 항목 한 줄(날짜 구간 텍스트 + "삭제" 버튼)을 만듭니다. */
    private JPanel createEntryRow(int index) {
        TravelDateEntry entry = savedEntries.get(index);
        String text = formatRange(entry);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

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

    /** "M월 d일 ~ M월 d일 (N박 M일)" 형태의 문자열로 날짜 구간을 표현합니다. */
    static String formatRange(TravelDateEntry entry) {
        LocalDate start = LocalDate.parse(entry.getStartDate());
        LocalDate end = LocalDate.parse(entry.getEndDate());
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1; // 시작일도 포함해서 며칠인지
        long nights = days - 1;
        return start.getMonthValue() + "월 " + start.getDayOfMonth() + "일 ~ "
                + end.getMonthValue() + "월 " + end.getDayOfMonth() + "일"
                + "  (" + nights + "박 " + days + "일)";
    }

    /** "제출" 버튼 클릭 시 실행됩니다. 날짜리스트 전체를 CSV에 저장합니다. */
    private void handleSubmit() {
        if (savedEntries.isEmpty()) {
            showWarning("저장된 날짜가 없습니다. 먼저 날짜를 저장해주세요.");
            return;
        }

        boolean saved = repository.appendEntries(savedEntries);
        if (saved) {
            JOptionPane.showMessageDialog(this,
                    "여행 날짜가 제출되었습니다!",
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
