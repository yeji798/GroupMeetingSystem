/**
 * TravelScheduleEditDialog.java
 *
 * 방 메인 화면의 "자신의 일정 수정" 버튼을 눌렀을 때 뜨는 화면입니다. (단체 여행 방 전용)
 * MyScheduleEditDialog(단체 약속용)와 구조가 같지만, 시간 없이 날짜(시작~종료)만 다룹니다.
 *
 *   1. 창을 열면 예전에 저장해두었던 나의 여행 날짜 목록을 CSV에서 불러와 미리 보여준다.
 *   2. 목록의 각 항목마다 "수정"/"삭제" 버튼이 있어서 개별적으로 고치거나 지울 수 있다.
 *   3. 같은 날짜에(정확히는 날짜 구간이) 겹치는 항목은 등록할 수 없다.
 *   4. 맨 아래 "저장" 버튼을 눌러야 비로소 CSV 파일에 최종 반영된다.
 *
 *   <필드>
 *   1. repository   : 여행 날짜 CSV를 읽고 쓰는 저장소 객체
 *   2. room          : 지금 일정을 수정 중인 방
 *   3. memberId      : 지금 로그인해서 수정 중인 사용자의 아이디
 *   4. savedEntries  : 화면에서 지금 편집 중인 날짜 목록 (아직 CSV에 저장되지 않은 "임시 작업본")
 *
 *   <중요 메소드>
 *   1. handleAddEntry()    : "추가" 버튼 -> 겹치는지 검사 후 목록에 새 항목 추가
 *   2. handleEditEntry()   : 목록의 "수정" 버튼 -> 그 항목 값을 달력에 다시 채워주고 목록에서는 제거
 *   3. handleDeleteEntry() : 목록의 "삭제" 버튼 -> 그 항목을 목록에서 제거
 *   4. handleFinalSave()   : "저장" 버튼 -> 지금 목록 전체를 CSV에 최종 반영
 */

package com.groupmeeting.view.travel;

import com.groupmeeting.view.common.CalendarPanel;
import com.groupmeeting.view.common.Theme;
import com.groupmeeting.view.schedule.MyScheduleEditDialog;

import com.groupmeeting.model.Room;
import com.groupmeeting.model.TravelDateEntry;
import com.groupmeeting.repository.TravelDateRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TravelScheduleEditDialog extends JDialog {

    private final TravelDateRepository repository;
    private final Room room;
    private final String memberId;

    private final List<TravelDateEntry> savedEntries = new ArrayList<>();

    private CalendarPanel startCalendar;
    private CalendarPanel endCalendar;
    private JPanel entriesPanel;
    private JScrollPane entriesScrollPane; // entriesPanel을 감싸는 가로 스크롤 영역

    public TravelScheduleEditDialog(Window owner, TravelDateRepository repository, Room room, String memberId) {
        super(owner, "자신의 일정 수정", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.room = room;
        this.memberId = memberId;
        // 예전에 저장해둔 내용을 화면을 만들기 "전에" 미리 불러옵니다. (initComponents() 안에서
        // 날짜리스트의 실제 크기에 맞춰 가로 스크롤 영역의 높이를 정확히 계산하기 위함입니다)
        savedEntries.addAll(repository.getForRoomAndMember(room.getCode(), memberId));
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

    /** 화면 내부 컴포넌트(달력, 추가 버튼, 날짜리스트, 저장 버튼)를 배치합니다. */
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

        JLabel startLabel = sectionLabel("여행 시작 날짜");
        startCalendar = new CalendarPanel();

        JLabel endLabel = sectionLabel("여행 종료 날짜");
        endCalendar = new CalendarPanel();

        JButton addButton = new JButton("추가");
        Theme.styleSecondaryButton(addButton);
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(120, 34));
        addButton.addActionListener(e -> handleAddEntry());

        JLabel listLabel = sectionLabel("날짜리스트");
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        renderEntries(); // savedEntries는 생성자에서 이미 채워져 있으므로, 여기서 바로 실제 내용을 그림

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.addActionListener(e -> handleFinalSave());

        // 날짜리스트 글자가 길어도(예: "N박 M일") 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        entriesScrollPane = Theme.wrapHorizontalScrollable(entriesPanel, Theme.STANDARD_CONTENT_WIDTH);

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
        root.add(addButton);
        root.add(Box.createVerticalStrut(16));
        root.add(listLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(entriesScrollPane);
        root.add(Box.createVerticalStrut(20));
        root.add(saveButton);

        Theme.alignAsCenteredColumn(startLabel, startCalendar, endLabel, endCalendar, listLabel, entriesScrollPane, saveButton);

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


    /** "추가" 버튼 클릭 시 실행됩니다. 날짜 검증 -> 겹침 검사 -> 문제없으면 목록에 새 항목을 추가합니다. */
    private void handleAddEntry() {
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

    /** 새로 추가하려는 날짜 구간이 이미 목록에 있는 구간과 겹치는지 확인합니다. (양끝 포함) */
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
            JLabel emptyLabel = new JLabel("등록된 날짜가 없습니다.");
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

        // 이미 가로 스크롤 영역으로 감싼 뒤(=항목을 추가/수정/삭제해서 다시 그리는 경우)라면,
        // 새로 바뀐 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        if (entriesScrollPane != null) {
            Theme.resyncScrollableHeight(entriesScrollPane, entriesPanel);
            revalidate();
            repaint();
        }
    }

    /** 날짜리스트의 항목 한 줄(날짜 구간 텍스트 + "수정" 버튼 + "삭제" 버튼)을 만듭니다. */
    private JPanel createEntryRow(int index) {
        TravelDateEntry entry = savedEntries.get(index);
        String text = TravelDateInputDialog.formatRange(entry);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(Theme.FONT_NORMAL);
        textLabel.setForeground(Theme.TEXT_DARK);

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
     * -> 이 항목의 시작/종료 날짜를 위쪽 달력에 다시 채워 넣고, 목록에서는 일단 제거합니다.
     *    (사용자가 값을 고친 뒤 "추가"를 다시 누르면 고친 내용이 새 항목으로 다시 들어갑니다)
     */
    private void handleEditEntry(int index) {
        TravelDateEntry entry = savedEntries.get(index);

        startCalendar.selectDate(LocalDate.parse(entry.getStartDate()));
        endCalendar.selectDate(LocalDate.parse(entry.getEndDate()));

        savedEntries.remove(index);
        renderEntries();
    }

    /** 목록의 "삭제" 버튼 클릭 시 실행됩니다. 해당 항목을 목록에서 제거합니다. */
    private void handleDeleteEntry(int index) {
        savedEntries.remove(index);
        renderEntries();
    }

    /** "저장" 버튼 클릭 시 실행됩니다. 지금 목록 전체로 CSV에 저장된 이 사람의 기존 기록을 덮어씁니다. */
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
