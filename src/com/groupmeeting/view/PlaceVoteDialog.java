/**
 * PlaceVoteDialog.java
 *
 * 방 메인 화면의 "장소 투표" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 지금까지 등록된 장소 후보 목록이 뜨고, 원하는 장소의 "투표" 버튼을 누르면 그 장소에 투표됩니다.
 * 이미 투표한 장소는 목록에서 "✓ 투표함" 으로 표시됩니다.
 * 마음이 바뀌면 아래의 "투표 취소" 버튼으로 내 투표를 지우고 다른 곳에 다시 투표할 수 있습니다.
 * "투표 결과 보기" 버튼을 누르면 지금까지의 득표 현황(VoteResultDialog)을 볼 수 있습니다.
 *
 *   <필드>
 *   1. placeRepository     : 장소 후보 목록을 읽는 저장소 객체
 *   2. placeVoteRepository : 투표 기록을 읽고 쓰는 저장소 객체
 *   3. memberRepository    : 투표 결과 화면에서 닉네임을 보여주기 위해 필요 (VoteResultDialog에 전달)
 *   4. room                 : 지금 보고 있는 방 정보
 *   5. memberId             : 지금 로그인한 사용자의 아이디 (누가 투표하는지 구분하기 위해 필요)
 *
 *   <생성자>
 *   : 창을 만들고, 등록된 장소 목록과 내 투표 현황을 불러와 화면에 표시함
 *
 *   <중요 메소드>
 *   1. handleVote(place)        : 특정 장소의 "투표" 버튼 -> 그 장소에 투표
 *   2. handleCancelVote()       : "투표 취소" 버튼 -> 내 투표 기록 삭제
 *   3. handleShowResult()       : "투표 결과 보기" 버튼 -> VoteResultDialog를 염
 *   4. renderPlaces()           : 지금 상태(등록된 장소, 내 투표)를 기준으로 목록을 다시 그림
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.util.MemberRepository;
import com.groupmeeting.util.PlaceRepository;
import com.groupmeeting.util.PlaceVoteRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PlaceVoteDialog extends JDialog {

    private final PlaceRepository placeRepository;
    private final PlaceVoteRepository placeVoteRepository;
    private final MemberRepository memberRepository;
    private final Room room;
    private final String memberId;

    private JPanel placesPanel; // 장소 + 투표 버튼 목록이 그려지는 영역

    public PlaceVoteDialog(Window owner, PlaceRepository placeRepository, PlaceVoteRepository placeVoteRepository,
                            MemberRepository memberRepository, Room room, String memberId) {
        super(owner, "장소 투표", ModalityType.APPLICATION_MODAL);
        this.placeRepository = placeRepository;
        this.placeVoteRepository = placeVoteRepository;
        this.memberRepository = memberRepository;
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

    /** 화면 내부 컴포넌트(제목, 장소 목록, 투표 취소/결과 보기/닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 장소 투표");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("원하는 장소의 [투표] 버튼을 눌러주세요.");
        subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        subLabel.setForeground(new Color(0x99, 0x99, 0x99));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        placesPanel = new JPanel();
        placesPanel.setLayout(new BoxLayout(placesPanel, BoxLayout.Y_AXIS));
        placesPanel.setOpaque(false);
        placesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelVoteButton = new JButton("투표 취소");
        Theme.styleSecondaryButton(cancelVoteButton);
        cancelVoteButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelVoteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cancelVoteButton.addActionListener(e -> handleCancelVote());

        JButton resultButton = new JButton("투표 결과 보기");
        Theme.styleSecondaryButton(resultButton);
        resultButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        resultButton.addActionListener(e -> handleShowResult());

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        closeButton.addActionListener(e -> dispose());

        // 장소 목록을 먼저 채운 뒤, 이름이 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        renderPlaces();
        JScrollPane placesScroll = Theme.wrapHorizontalScrollable(placesPanel, Theme.STANDARD_CONTENT_WIDTH);
        placesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(subLabel);
        root.add(Box.createVerticalStrut(16));
        root.add(placesScroll);
        root.add(Box.createVerticalStrut(16));
        root.add(cancelVoteButton);
        root.add(Box.createVerticalStrut(8));
        root.add(resultButton);
        root.add(Box.createVerticalStrut(10));
        root.add(closeButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    /**
     * 등록된 장소 목록을 다시 읽어와서, 각 장소를 한 줄씩("장소 이름" + "투표" 버튼 또는 "✓ 투표함" 표시)
     * 다시 그립니다. 지금 내가 투표한 장소는 초록색으로 강조해서 한눈에 보이게 합니다.
     */
    private void renderPlaces() {
        placesPanel.removeAll();

        List<String> places = placeRepository.getPlaces(room.getCode());
        String myVote = placeVoteRepository.getMyVote(room.getCode(), memberId);

        if (places.isEmpty()) {
            JLabel emptyLabel = new JLabel("등록된 장소가 없습니다. 먼저 '장소 확인'에서 장소를 추가해주세요.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            placesPanel.add(emptyLabel);
        } else {
            for (String place : places) {
                boolean isMyVote = place.equals(myVote);
                placesPanel.add(createPlaceRow(place, isMyVote));
                placesPanel.add(Box.createVerticalStrut(6));
            }
        }

        placesPanel.revalidate();
        placesPanel.repaint();
    }

    /** 장소 목록의 항목 한 줄(장소 이름 + 투표 버튼/투표 완료 표시)을 만듭니다. */
    private JPanel createPlaceRow(String place, boolean isMyVote) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        // -> 내가 투표한 장소는 배경을 초록색 계열로 강조해서 다른 장소와 구분되게 함
        row.setBackground(isMyVote ? Theme.ACCENT_GREEN : Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel placeLabel = new JLabel(place);
        placeLabel.setFont(Theme.FONT_NORMAL);
        placeLabel.setForeground(Theme.TEXT_DARK);

        if (isMyVote) {
            // 이미 이 장소에 투표한 상태 -> 버튼 대신 완료 표시만 보여줌
            JLabel doneLabel = new JLabel("✓ 투표함");
            doneLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
            doneLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
            row.add(placeLabel, BorderLayout.CENTER);
            row.add(doneLabel, BorderLayout.EAST);
        } else {
            JButton voteButton = new JButton("투표");
            Theme.styleSecondaryButton(voteButton);
            voteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            voteButton.addActionListener(e -> handleVote(place));
            row.add(placeLabel, BorderLayout.CENTER);
            row.add(voteButton, BorderLayout.EAST);
        }

        return row;
    }

    /** 특정 장소의 "투표" 버튼 클릭 시 실행됩니다. 기존 투표는 자동으로 취소되고 이 장소로 새로 등록됩니다. */
    private void handleVote(String place) {
        placeVoteRepository.castVote(room.getCode(), memberId, place);
        renderPlaces();
    }

    /** "투표 취소" 버튼 클릭 시 실행됩니다. 내가 투표한 기록이 있으면 지웁니다. */
    private void handleCancelVote() {
        String myVote = placeVoteRepository.getMyVote(room.getCode(), memberId);
        if (myVote == null) {
            JOptionPane.showMessageDialog(this,
                    "아직 투표한 장소가 없습니다.",
                    "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        placeVoteRepository.cancelVote(room.getCode(), memberId);
        renderPlaces();
    }

    /** "투표 결과 보기" 버튼 클릭 시 실행됩니다. 득표 현황 화면을 엽니다. */
    private void handleShowResult() {
        VoteResultDialog dialog =
                new VoteResultDialog(this, placeRepository, placeVoteRepository, memberRepository, room);
        dialog.setVisible(true);
        renderPlaces(); // 결과 화면을 닫고 돌아왔을 때도 혹시 모를 변경사항을 반영해 다시 그려줌
    }
}
