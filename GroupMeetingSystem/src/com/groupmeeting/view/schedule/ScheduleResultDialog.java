/**
 * ScheduleResultDialog.java
 *
 * "날짜 및 시간 산출" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 방에 있는 참여자들이 지금까지 CSV(availability.csv)에 제출해둔 "나는 이 날짜에 이 시간에 가능해요"
 * 정보를 모두 불러온 다음, "모든 참여자가 동시에 가능한 시간대(겹치는 시간)"를 계산해서
 * 그 중 시간이 가장 긴 순서로 1순위 / 2순위 / 3순위를 보여줍니다.
 *
 *   <필드>
 *   1. repository       : 가능 시간 CSV를 읽고 쓰는 저장소 객체 (AvailabilityRepository)
 *   2. memberRepository : 회원 닉네임을 조회하기 위한 저장소 객체 (MemberRepository)
 *   3. room              : 지금 보고 있는 방 정보
 *
 *   <생성자>
 *   : 창을 생성하고, 곧바로 겹치는 시간대를 계산해서 화면에 순위별로 표시함
 *
 *   <중요 메소드>
 *   1. computeTopCommonSlots()
 *      - CSV에서 이 방의 모든 가능 시간을 불러와 회원별로 묶고(groupByMember)
 *      - 날짜별로 모든 회원의 가능 시간을 교집합(intersect) 계산
 *      - 겹치는 시간이 긴 순서로 정렬해서 상위 3개만 반환
 *   2. intersectIntervalLists() : 두 회원의 "가능 시간 구간 목록" 사이의 겹치는 부분(교집합)을 계산
 *   3. handleShowParticipantSchedules() : "참여자 일정 보기" 버튼 -> ParticipantScheduleDialog를 염
 */

package com.groupmeeting.view.schedule;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.AvailabilityEntry;
import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.AvailabilityRepository;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.RoomFinalRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

public class ScheduleResultDialog extends JDialog {

    private final AvailabilityRepository repository;
    private final MemberRepository memberRepository;
    private final RoomFinalRepository finalRepository;
    private final Room room;
    private final Member loginMember;

    // 화면에 순위 카드를 그려 넣을 패널 (계산 결과에 따라 매번 다시 그림)
    private JPanel resultPanel;

    public ScheduleResultDialog(Window owner, AvailabilityRepository repository, MemberRepository memberRepository,
                                 RoomFinalRepository finalRepository, Room room, Member loginMember) {
        super(owner, "날짜 및 시간 산출", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.finalRepository = finalRepository;
        this.room = room;
        this.loginMember = loginMember;
        initDialog();
        initComponents();
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. */
    private void initDialog() {
        setSize(432, 768);                     // 다른 화면들과 동일한 창 크기(9:16 비율) 사용
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부의 컴포넌트(제목, 순위 카드, 버튼 등)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS)); // 위에서 아래로 세로 배치
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 날짜 및 시간 산출");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        boolean iAmOwner = loginMember.getId().equals(room.getOwnerId());
        JLabel subLabel = new JLabel("<html><div style='text-align:center;'>참여자들이 입력한 시간 중<br>모두 가능한 시간이 가장 긴 순서입니다."
                + (iAmOwner ? "<br>(방장은 클릭해서 최종 날짜/시간으로 확정할 수 있어요)" : "") + "</div></html>");
        subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        subLabel.setForeground(new Color(0x99, 0x99, 0x99));
        subLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 결과(1~3순위)가 들어갈 자리. renderResult()에서 내용을 채워 넣습니다.
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton participantButton = new JButton("참여자 일정 보기");
        Theme.styleSecondaryButton(participantButton);
        participantButton.addActionListener(e -> handleShowParticipantSchedules());
        // -> 클릭하면 참여자별 일정을 보여주는 ParticipantScheduleDialog를 연다.

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        renderResult(iAmOwner); // 창이 열리자마자 바로 계산해서 보여준다.

        // 순위 카드 안의 날짜/시간 글자가 길어도 잘리지 않도록, resultPanel을 가로 스크롤
        // 영역으로 감쌉니다. (renderResult()로 내용을 다 채운 뒤에 감싸야 높이가 정확합니다)
        JScrollPane resultScroll = Theme.wrapHorizontalScrollable(resultPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(subLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(resultScroll);
        root.add(Box.createVerticalStrut(20));
        root.add(participantButton);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        // resultPanel(순위 카드들)과 두 버튼의 가로 폭을 통일해서 화면 가운데로 나란히 정렬합니다.
        Theme.alignAsCenteredColumn(resultScroll, participantButton, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** computeTopCommonSlots()로 계산한 결과를 1순위/2순위/3순위 카드로 그려 넣습니다. */
    private void renderResult(boolean iAmOwner) {
        resultPanel.removeAll();

        List<RankedSlot> topSlots = computeTopCommonSlots();

        if (topSlots.isEmpty()) {
            // 아직 겹치는 시간이 없는 경우 (참여자가 한 명도 입력 안 했거나, 겹치는 날짜/시간이 없는 경우)
            JLabel emptyLabel = new JLabel("<html><div style='text-align:center;'>아직 모두가 겹치는 시간이 없습니다.<br>참여자들이 시간을 더 입력하면 다시 계산됩니다.</div></html>");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            resultPanel.add(emptyLabel);
        } else {
            String[] rankLabels = {"1순위", "2순위", "3순위"};
            for (int i = 0; i < topSlots.size(); i++) {
                resultPanel.add(createRankCard(rankLabels[i], topSlots.get(i), iAmOwner));
                resultPanel.add(Box.createVerticalStrut(10));
            }
        }

        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /**
     * 순위 하나(예: "1순위 · 7월 3일(금) 14:00~16:30 · 2시간 30분")를 카드 형태로 만듭니다.
     * 방장이 보고 있다면(iAmOwner) 카드를 클릭해서 이 날짜/시간을 최종으로 확정할 수 있습니다.
     */
    private JPanel createRankCard(String rankLabel, RankedSlot slot, boolean iAmOwner) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel rankTitle = new JLabel(rankLabel);
        rankTitle.setFont(Theme.FONT_SUBTITLE);
        rankTitle.setForeground(Theme.PRIMARY_GREEN_DARK);
        rankTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
        String dateText = slot.date.format(dateFormat);
        String dateTimeText = dateText + "  " + slot.start + " ~ " + slot.end;
        JLabel dateTimeLabel = new JLabel(dateTimeText);
        dateTimeLabel.setFont(Theme.FONT_NORMAL);
        dateTimeLabel.setForeground(Theme.TEXT_DARK);
        dateTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel durationLabel = new JLabel("모두 가능 · " + formatDuration(slot.minutes()));
        durationLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        durationLabel.setForeground(new Color(0x77, 0x77, 0x77));
        durationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(rankTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(dateTimeLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(durationLabel);

        if (iAmOwner) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel hintLabel = new JLabel("클릭하면 최종 날짜/시간으로 확정됩니다");
            hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
            hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(2));
            card.add(hintLabel);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    handleFinalize(dateText, slot);
                }
            });
        }

        return card;
    }

    /** 순위 카드 클릭 시(방장 전용) 실행됩니다. 확인 후 이 날짜/시간을 방의 최종 날짜/시간으로 저장합니다. */
    private void handleFinalize(String dateText, RankedSlot slot) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "이 날짜/시간(" + dateText + " " + slot.start + " ~ " + slot.end + ")을 모임의 최종 날짜/시간으로 지정하시겠습니까?",
                "최종 날짜/시간 확정", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            finalRepository.setFinalDate(room.getCode(), dateText);
            finalRepository.setFinalTime(room.getCode(), slot.start.toString(), slot.end.toString());
            JOptionPane.showMessageDialog(this,
                    "모임의 최종 날짜/시간으로 확정되었습니다!",
                    "확정 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    /** 분 단위 숫자를 "2시간 30분" 같은 읽기 쉬운 문자열로 바꿉니다. */
    private String formatDuration(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "분";
        }
        if (minutes == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + minutes + "분";
    }

    // =====================================================================
    // 겹치는 시간 계산 알고리즘
    // =====================================================================

    /**
     * 이 방에 제출된 모든 가능 시간을 바탕으로, "모든 참여자가 동시에 가능한 시간대"를 계산하여
     * 겹치는 시간이 긴 순서(내림차순)로 정렬한 뒤, 상위 3개(1~3순위)만 반환합니다.
     *
     * 계산 방식:
     *  1) 전체 데이터를 회원별로 묶는다. (아직 한 번도 입력하지 않은 회원은 계산에서 제외한다.
     *     -> 그 회원이 언제 가능한지 정보가 전혀 없으므로, "모두 가능"을 판단할 수 없기 때문)
     *  2) 데이터가 존재하는 날짜들을 하나씩 훑으면서, 그 날짜에 "모든 회원"이 각자 최소 1개 이상의
     *     가능 시간을 입력해두었는지 확인한다. 한 명이라도 그 날짜에 아무것도 입력하지 않았다면
     *     그 날짜는 "모두 가능"이 될 수 없으므로 건너뛴다.
     *  3) 남은 날짜에 대해서는 회원들의 가능 시간 구간들을 순서대로 교집합(intersect) 내서
     *     실제로 전원이 겹치는 시간 구간들을 뽑아낸다.
     *  4) 뽑아낸 모든 구간을 길이(분) 기준 내림차순으로 정렬하고 상위 3개만 반환한다.
     */
    private List<RankedSlot> computeTopCommonSlots() {
        List<AvailabilityEntry> allEntries = repository.getForRoom(room.getCode());

        // 1) 회원별로 묶기 (memberId -> 그 회원이 입력한 항목들)
        Map<String, List<AvailabilityEntry>> byMember = groupByMember(allEntries);
        if (byMember.isEmpty()) {
            return new ArrayList<>(); // 아무도 입력하지 않았다면 계산할 것이 없음
        }

        // 2) 데이터에 등장하는 날짜들을 오름차순으로 모은다. (TreeSet -> 문자열 "yyyy-MM-dd"라 정렬도 날짜순과 동일)
        Set<String> dates = new TreeSet<>();
        for (AvailabilityEntry entry : allEntries) {
            dates.add(entry.getDate());
        }

        List<RankedSlot> candidates = new ArrayList<>();

        for (String dateStr : dates) {
            // 이 날짜에 대해, 회원별 가능 시간 구간 목록들을 모은다.
            List<List<TimeInterval>> perMemberIntervals = new ArrayList<>();
            boolean everyoneHasThisDate = true;

            for (List<AvailabilityEntry> memberEntries : byMember.values()) {
                List<TimeInterval> intervalsOnDate = extractIntervalsForDate(memberEntries, dateStr);
                if (intervalsOnDate.isEmpty()) {
                    // 이 회원은 이 날짜에 입력한 것이 없음 -> 이 날짜는 "모두 가능"이 될 수 없음
                    everyoneHasThisDate = false;
                    break;
                }
                perMemberIntervals.add(intervalsOnDate);
            }

            if (!everyoneHasThisDate) {
                continue; // 다음 날짜로 넘어감
            }

            // 3) 회원들의 구간 목록을 차례로 교집합 내서, 전원이 겹치는 구간만 남긴다.
            List<TimeInterval> common = perMemberIntervals.get(0);
            for (int i = 1; i < perMemberIntervals.size() && !common.isEmpty(); i++) {
                common = intersectIntervalLists(common, perMemberIntervals.get(i));
            }

            LocalDate date = LocalDate.parse(dateStr);
            for (TimeInterval slot : common) {
                candidates.add(new RankedSlot(date, slot.start, slot.end));
            }
        }

        // 4) 겹치는 시간이 긴 순서로 정렬 후 상위 3개만 반환
        candidates.sort((a, b) -> Long.compare(b.minutes(), a.minutes()));
        if (candidates.size() > 3) {
            return candidates.subList(0, 3);
        }
        return candidates;
    }

    /** 가능 시간 목록을 회원 아이디(memberId) 기준으로 묶어서 Map으로 반환합니다. */
    private Map<String, List<AvailabilityEntry>> groupByMember(List<AvailabilityEntry> entries) {
        Map<String, List<AvailabilityEntry>> map = new LinkedHashMap<>();
        for (AvailabilityEntry entry : entries) {
            // computeIfAbsent: memberId에 해당하는 리스트가 없으면 새로 만들고, 있으면 기존 것을 그대로 사용
            map.computeIfAbsent(entry.getMemberId(), k -> new ArrayList<>()).add(entry);
        }
        return map;
    }

    /** 한 회원이 입력한 항목들 중에서, 특정 날짜(dateStr)에 해당하는 것만 골라 시간 구간 목록으로 변환합니다. */
    private List<TimeInterval> extractIntervalsForDate(List<AvailabilityEntry> memberEntries, String dateStr) {
        List<TimeInterval> result = new ArrayList<>();
        for (AvailabilityEntry entry : memberEntries) {
            if (entry.getDate().equals(dateStr)) {
                LocalTime start = LocalTime.parse(entry.getStartTime());
                LocalTime end = LocalTime.parse(entry.getEndTime());
                result.add(new TimeInterval(start, end));
            }
        }
        result.sort(Comparator.comparing(iv -> iv.start)); // 시작 시간 순으로 정렬
        return result;
    }

    /**
     * 두 개의 시간 구간 목록(a, b) 사이에서 서로 겹치는 부분(교집합)만 뽑아냅니다.
     * 예) a=[09:00~12:00], b=[10:00~13:00] -> 결과=[10:00~12:00]
     */
    private List<TimeInterval> intersectIntervalLists(List<TimeInterval> a, List<TimeInterval> b) {
        List<TimeInterval> result = new ArrayList<>();
        for (TimeInterval intervalA : a) {
            for (TimeInterval intervalB : b) {
                // 겹치는 구간의 시작 = 두 시작 시간 중 더 늦은 시간
                LocalTime start = intervalA.start.isAfter(intervalB.start) ? intervalA.start : intervalB.start;
                // 겹치는 구간의 끝 = 두 끝 시간 중 더 이른 시간
                LocalTime end = intervalA.end.isBefore(intervalB.end) ? intervalA.end : intervalB.end;
                if (start.isBefore(end)) {
                    result.add(new TimeInterval(start, end));
                }
            }
        }
        return result;
    }

    /** "참여자 일정 보기" 버튼: 참여자별 가능 시간 목록을 보여주는 화면을 엽니다. */
    private void handleShowParticipantSchedules() {
        ParticipantScheduleDialog dialog =
                new ParticipantScheduleDialog(this, repository, memberRepository, room);
        dialog.setVisible(true);
    }

    // =====================================================================
    // 계산에만 사용하는 간단한 보조 클래스들
    // =====================================================================

    /** 하루 안에서의 "시작~끝" 시간 구간 하나를 나타내는 값 객체입니다. (계산용, 화면에는 직접 쓰이지 않음) */
    private static class TimeInterval {
        final LocalTime start;
        final LocalTime end;

        TimeInterval(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }

    /** 화면에 순위로 보여줄 "날짜 + 시작~끝 시간" 결과 한 건을 나타내는 값 객체입니다. */
    private static class RankedSlot {
        final LocalDate date;
        final LocalTime start;
        final LocalTime end;

        RankedSlot(LocalDate date, LocalTime start, LocalTime end) {
            this.date = date;
            this.start = start;
            this.end = end;
        }

        /** 이 시간 구간이 몇 분짜리인지 계산합니다. (정렬에 사용) */
        long minutes() {
            return java.time.Duration.between(start, end).toMinutes();
        }
    }
}
