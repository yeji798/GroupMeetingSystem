/**
 * TravelScheduleResultDialog.java
 *
 * "날짜 산출" 버튼(단체 여행 방 전용)을 눌렀을 때 뜨는 화면입니다.
 * 참여자들이 제출한 여행 날짜(시작~종료)들을 모두 불러온 다음, "모든 참여자가 겹치는 날짜 구간"을
 * 계산해서 가장 빠른 날짜(이른 시작일)부터 순서대로 1순위~5순위까지 보여줍니다.
 * (단체 약속의 ScheduleResultDialog와 달리, 겹치는 기간이 긴지 짧은지는 따지지 않고
 *  "언제부터 시작하는지"만 기준으로 순위를 매깁니다 - 요구사항에 따름)
 *
 * 방장이 이 화면을 보고 있다면, 순위 카드를 클릭해서 그 날짜를 "모임 최종 날짜"로 바로
 * 확정할 수 있습니다. (한 번 확정하면 방 메인 화면 아래쪽에 표시됩니다)
 *
 *   <필드>
 *   1. repository       : 여행 날짜 CSV를 읽는 저장소 객체
 *   2. finalRepository   : 최종 확정 정보를 저장하는 저장소 객체
 *   3. room               : 지금 보고 있는 방
 *   4. loginMember        : 지금 로그인한 사용자 (방장인지 확인해서 확정 가능 여부를 결정)
 *
 *   <중요 메소드>
 *   1. computeTopRanges() : 모든 참여자가 겹치는 날짜 구간을 계산해 이른 날짜 순으로 상위 5개 반환
 *   2. intersectRanges()  : 두 날짜 구간 목록 사이의 겹치는 부분(교집합)을 계산
 *   3. handleFinalize()   : 순위 카드 클릭(방장 전용) -> 확인 후 이 날짜를 방의 최종 날짜로 저장
 */

package com.groupmeeting.view.travel;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.model.TravelDateEntry;
import com.groupmeeting.repository.RoomFinalRepository;
import com.groupmeeting.repository.TravelDateRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;

public class TravelScheduleResultDialog extends JDialog {

    private final TravelDateRepository repository;
    private final RoomFinalRepository finalRepository;
    private final Room room;
    private final Member loginMember;

    private JPanel resultPanel;

    public TravelScheduleResultDialog(Window owner, TravelDateRepository repository, RoomFinalRepository finalRepository,
                                       Room room, Member loginMember) {
        super(owner, "날짜 산출", ModalityType.APPLICATION_MODAL);
        this.repository = repository;
        this.finalRepository = finalRepository;
        this.room = room;
        this.loginMember = loginMember;
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

    /** 화면 내부 컴포넌트(제목, 순위 카드, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 날짜 산출");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        boolean iAmOwner = loginMember.getId().equals(room.getOwnerId());
        JLabel subLabel = new JLabel("<html><div style='text-align:center;'>참여자들이 입력한 날짜 중<br>가장 빠르게 모두 겹치는 순서입니다."
                + (iAmOwner ? "<br>(방장은 클릭해서 최종 날짜로 확정할 수 있어요)" : "") + "</div></html>");
        subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        subLabel.setForeground(new Color(0x99, 0x99, 0x99));
        subLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        renderResult(iAmOwner);
        // 순위 카드 안의 날짜 구간 글자가 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        JScrollPane resultScroll = Theme.wrapHorizontalScrollable(resultPanel, Theme.STANDARD_CONTENT_WIDTH);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(subLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(resultScroll);
        root.add(Box.createVerticalStrut(20));
        root.add(closeButton);

        Theme.alignAsCenteredColumn(resultScroll, closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** computeTopRanges()로 계산한 결과를 1순위~5순위 카드로 그려 넣습니다. */
    private void renderResult(boolean iAmOwner) {
        resultPanel.removeAll();

        List<DateRange> topRanges = computeTopRanges();

        if (topRanges.isEmpty()) {
            JLabel emptyLabel = new JLabel("<html><div style='text-align:center;'>아직 모두가 겹치는 날짜가 없습니다.<br>참여자들이 날짜를 더 입력하면 다시 계산됩니다.</div></html>");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            resultPanel.add(emptyLabel);
        } else {
            String[] rankLabels = {"1순위", "2순위", "3순위", "4순위", "5순위"};
            for (int i = 0; i < topRanges.size(); i++) {
                resultPanel.add(createRankCard(rankLabels[i], topRanges.get(i), iAmOwner));
                resultPanel.add(Box.createVerticalStrut(10));
            }
        }

        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /** 순위 하나를 카드 형태로 만듭니다. 방장이라면 클릭해서 최종 날짜로 확정할 수 있습니다. */
    private JPanel createRankCard(String rankLabel, DateRange range, boolean iAmOwner) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel rankTitle = new JLabel(rankLabel);
        rankTitle.setFont(Theme.FONT_SUBTITLE);
        rankTitle.setForeground(Theme.PRIMARY_GREEN_DARK);

        String rangeText = formatRange(range.start, range.end);
        JLabel rangeLabel = new JLabel(rangeText);
        rangeLabel.setFont(Theme.FONT_NORMAL);
        rangeLabel.setForeground(Theme.TEXT_DARK);

        card.add(rankTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(rangeLabel);

        if (iAmOwner) {
            // 방장이면 카드를 클릭할 수 있게 만들고, 클릭하면 확인 후 최종 날짜로 저장합니다.
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel hintLabel = new JLabel("클릭하면 최종 날짜로 확정됩니다");
            hintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            hintLabel.setForeground(new Color(0x99, 0x99, 0x99));
            card.add(Box.createVerticalStrut(2));
            card.add(hintLabel);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    handleFinalize(rangeText);
                }
            });
        }

        return card;
    }

    /** 순위 카드 클릭 시(방장 전용) 실행됩니다. 확인 후 이 날짜를 방의 최종 날짜로 저장합니다. */
    private void handleFinalize(String rangeText) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "이 날짜(" + rangeText + ")를 모임의 최종 날짜로 지정하시겠습니까?",
                "최종 날짜 확정", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            finalRepository.setFinalDate(room.getCode(), rangeText);
            JOptionPane.showMessageDialog(this,
                    "모임의 최종 날짜로 확정되었습니다!",
                    "확정 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    /** "M월 d일 ~ M월 d일 (N박 M일)" 형태의 문자열로 날짜 구간을 표현합니다. */
    private String formatRange(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long nights = days - 1;
        return start.getMonthValue() + "월 " + start.getDayOfMonth() + "일 ~ "
                + end.getMonthValue() + "월 " + end.getDayOfMonth() + "일"
                + "  (" + nights + "박 " + days + "일)";
    }

    // =====================================================================
    // 겹치는 날짜 계산 알고리즘
    // =====================================================================

    /**
     * 이 방에 제출된 모든 여행 날짜를 바탕으로, "모든 참여자가 겹치는 날짜 구간"을 계산하여
     * 시작 날짜가 이른 순서로 정렬한 뒤, 상위 5개(1~5순위)만 반환합니다.
     *
     * 계산 방식: ScheduleResultDialog의 시간 겹침 계산과 원리는 같지만, "하루 안의 몇 시부터
     * 몇 시까지"가 아니라 "여러 날에 걸친 날짜 구간"을 다룬다는 점이 다릅니다.
     *  1) 아직 한 번도 입력하지 않은 회원은 계산에서 제외합니다. (그 사람이 언제 가능한지 모르므로)
     *  2) 회원별 날짜 구간 목록들을 순서대로 교집합(intersect) 내서, 전원이 겹치는 구간만 남깁니다.
     *  3) 시작 날짜가 이른 순서로 정렬해서 상위 5개만 반환합니다.
     */
    private List<DateRange> computeTopRanges() {
        List<TravelDateEntry> allEntries = repository.getForRoom(room.getCode());

        Map<String, List<DateRange>> byMember = new LinkedHashMap<>();
        for (TravelDateEntry entry : allEntries) {
            DateRange range = new DateRange(LocalDate.parse(entry.getStartDate()), LocalDate.parse(entry.getEndDate()));
            byMember.computeIfAbsent(entry.getMemberId(), k -> new ArrayList<>()).add(range);
        }

        if (byMember.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<DateRange>> allMemberRanges = new ArrayList<>(byMember.values());

        // 회원들의 날짜 구간 목록을 차례로 교집합 내서, 전원이 겹치는 구간만 남긴다.
        List<DateRange> common = allMemberRanges.get(0);
        for (int i = 1; i < allMemberRanges.size() && !common.isEmpty(); i++) {
            common = intersectRanges(common, allMemberRanges.get(i));
        }

        // 시작 날짜가 이른 순서로 정렬
        common.sort(Comparator.comparing(r -> r.start));

        if (common.size() > 5) {
            return common.subList(0, 5);
        }
        return common;
    }

    /** 두 날짜 구간 목록(a, b) 사이에서 서로 겹치는 부분(교집합)만 뽑아냅니다. */
    private List<DateRange> intersectRanges(List<DateRange> a, List<DateRange> b) {
        List<DateRange> result = new ArrayList<>();
        for (DateRange rangeA : a) {
            for (DateRange rangeB : b) {
                LocalDate start = rangeA.start.isAfter(rangeB.start) ? rangeA.start : rangeB.start;
                LocalDate end = rangeA.end.isBefore(rangeB.end) ? rangeA.end : rangeB.end;
                if (!start.isAfter(end)) {
                    result.add(new DateRange(start, end));
                }
            }
        }
        return result;
    }

    /** 계산에만 사용하는 "시작 날짜 ~ 종료 날짜" 값 객체입니다. */
    private static class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
