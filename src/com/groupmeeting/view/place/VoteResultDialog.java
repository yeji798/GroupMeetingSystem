/**
 * VoteResultDialog.java
 *
 * "장소 투표" 화면의 "투표 결과 보기" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 장소별로 지금까지 몇 명이 투표했는지(득표 수)와, 누가 투표했는지(투표 명단)를 함께 보여줍니다.
 * 득표 수가 많은 장소가 위로 오도록 정렬해서, 지금 가장 유력한 장소를 한눈에 볼 수 있게 합니다.
 *
 * "실시간으로 볼 수 있게" 하기 위해, 이 창에는 "새로고침" 버튼이 있습니다.
 * -> 이 프로그램은 CSV 파일을 그때그때 읽는 방식이라 서버처럼 자동으로 화면이 갱신되지는 않으므로,
 *    누르면 최신 CSV 내용을 다시 읽어와 화면을 다시 그리는 방식으로 "실시간 확인"을 구현했습니다.
 *
 *   <필드>
 *   1. placeRepository     : 장소 후보 목록을 읽는 저장소 객체
 *   2. placeVoteRepository : 득표 수, 투표자 명단을 읽는 저장소 객체
 *   3. memberRepository    : 투표자 아이디를 닉네임으로 바꿔서 보여주기 위한 저장소 객체
 *   4. room                 : 지금 보고 있는 방 정보
 *
 *   <중요 메소드>
 *   1. renderResult() : 득표 수 기준으로 정렬한 뒤, 장소별 카드(득표 수 + 투표자 명단)를 화면에 그림
 */

package com.groupmeeting.view.place;

import com.groupmeeting.view.common.Theme;

import com.groupmeeting.model.Member;
import com.groupmeeting.model.Room;
import com.groupmeeting.repository.MemberRepository;
import com.groupmeeting.repository.PlaceRepository;
import com.groupmeeting.repository.PlaceVoteRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class VoteResultDialog extends JDialog {

    private final PlaceRepository placeRepository;
    private final PlaceVoteRepository placeVoteRepository;
    private final MemberRepository memberRepository;
    private final Room room;

    private JPanel resultPanel;

    public VoteResultDialog(Window owner, PlaceRepository placeRepository, PlaceVoteRepository placeVoteRepository,
                             MemberRepository memberRepository, Room room) {
        super(owner, "투표 결과 보기", ModalityType.APPLICATION_MODAL);
        this.placeRepository = placeRepository;
        this.placeVoteRepository = placeVoteRepository;
        this.memberRepository = memberRepository;
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

    /** 화면 내부 컴포넌트(제목, 결과 목록, 새로고침/닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("투표 결과");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton refreshButton = new JButton("새로고침");
        Theme.styleSecondaryButton(refreshButton);
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        refreshButton.addActionListener(e -> renderResult());
        // -> 누르면 CSV를 다시 읽어서 화면을 새로 그린다. (다른 참여자가 그 사이에 투표했을 수도 있으므로)

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        closeButton.addActionListener(e -> dispose());

        // 결과를 먼저 채운 뒤, 장소 이름이나 투표자 명단 글자가 길어도 잘리지 않도록
        // 가로 스크롤 영역으로 감쌉니다.
        renderResult();
        JScrollPane resultScroll = Theme.wrapHorizontalScrollable(resultPanel, Theme.STANDARD_CONTENT_WIDTH);
        Theme.alignAsCenteredColumn(resultScroll, refreshButton, closeButton);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(resultScroll);
        root.add(Box.createVerticalStrut(16));
        root.add(refreshButton);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /** 장소별 득표 수를 다시 계산해서, 득표 수가 많은 순서로 카드 목록을 새로 그립니다. */
    private void renderResult() {
        resultPanel.removeAll();

        List<String> places = placeRepository.getPlaces(room.getCode());
        Map<String, Integer> voteCounts = placeVoteRepository.getVoteCounts(room.getCode());
        Map<String, List<String>> votersByPlace = placeVoteRepository.getVotersByPlace(room.getCode());

        if (places.isEmpty()) {
            JLabel emptyLabel = new JLabel("등록된 장소가 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            resultPanel.add(emptyLabel);
            resultPanel.revalidate();
            resultPanel.repaint();
            return;
        }

        // 득표 수가 많은 장소가 위로 오도록 정렬 (없으면 0표로 취급)
        places.sort((a, b) -> {
            int countA = voteCounts.getOrDefault(a, 0);
            int countB = voteCounts.getOrDefault(b, 0);
            return countB - countA; // 내림차순
        });

        for (String place : places) {
            int count = voteCounts.getOrDefault(place, 0);
            List<String> voterIds = votersByPlace.getOrDefault(place, List.of());
            resultPanel.add(createResultCard(place, count, voterIds));
            resultPanel.add(Box.createVerticalStrut(10));
        }

        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /** 장소 하나에 대한 결과 카드(장소 이름 + 득표 수 + 투표자 닉네임 목록)를 만듭니다. */
    private JPanel createResultCard(String place, int count, List<String> voterIds) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headerLabel = new JLabel(place + "  ·  " + count + "표");
        headerLabel.setFont(Theme.FONT_SUBTITLE);
        headerLabel.setForeground(Theme.TEXT_DARK);
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(headerLabel);

        if (voterIds.isEmpty()) {
            JLabel noVoteLabel = new JLabel("아직 투표한 사람이 없습니다.");
            noVoteLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            noVoteLabel.setForeground(new Color(0x99, 0x99, 0x99));
            noVoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            noVoteLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            card.add(noVoteLabel);
        } else {
            // 아이디를 닉네임으로 바꿔서 "홍길동, 김철수" 처럼 한 줄로 이어붙인다.
            StringBuilder names = new StringBuilder();
            for (String voterId : voterIds) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                Member member = findMemberById(voterId);
                names.append(member != null ? member.getNickname() : voterId);
            }

            JLabel votersLabel = new JLabel(names.toString());
            votersLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            votersLabel.setForeground(new Color(0x77, 0x77, 0x77));
            votersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            votersLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            card.add(votersLabel);
        }

        return card;
    }

    /** 회원 아이디로 전체 회원 목록에서 일치하는 회원을 찾아 반환합니다. 없으면 null. */
    private Member findMemberById(String id) {
        for (Member m : memberRepository.loadAll()) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }
}
