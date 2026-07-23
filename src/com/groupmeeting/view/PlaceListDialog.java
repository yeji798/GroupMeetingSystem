/**
 * PlaceListDialog.java
 *
 * 방 메인 화면의 "장소 확인" 버튼을 눌렀을 때 뜨는 화면입니다.
 * 지금까지 등록된 장소 후보들을 목록으로 보여주고, 각 장소 옆의 "삭제" 버튼으로 지울 수 있으며,
 * 화면 위쪽 입력칸에 새 장소 이름을 적고 "추가" 버튼을 누르면 목록에 새로 들어갑니다.
 * (같은 이름의 장소는 두 번 등록되지 않도록 PlaceRepository에서 걸러줍니다.)
 *
 *   <필드>
 *   1. placeRepository     : 장소 후보 목록 CSV를 읽고 쓰는 저장소 객체
 *   2. placeVoteRepository : 장소를 삭제할 때, 그 장소에 걸려있던 투표 기록도 같이 지우기 위해 필요
 *   3. room                 : 지금 보고 있는 방 정보
 *
 *   <생성자>
 *   : 창을 만들고, 이 방의 장소 목록을 불러와 화면에 표시함
 *
 *   <중요 메소드>
 *   1. handleAddPlace()    : "추가" 버튼 -> 입력한 이름을 장소 목록에 추가 (중복이면 경고)
 *   2. handleDeletePlace() : 각 항목의 "삭제" 버튼 -> 그 장소와, 그 장소에 대한 투표 기록을 함께 삭제
 *   3. renderPlaces()      : 지금 저장된 목록을 다시 읽어와 화면을 새로 그림
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Room;
import com.groupmeeting.util.PlaceRepository;
import com.groupmeeting.util.PlaceVoteRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PlaceListDialog extends JDialog {

    private final PlaceRepository placeRepository;
    private final PlaceVoteRepository placeVoteRepository;
    private final Room room;

    private JTextField newPlaceField;
    private JPanel placesPanel; // 장소 목록이 그려지는 영역
    private JScrollPane placesScrollPane; // placesPanel을 감싸는 가로 스크롤 영역

    public PlaceListDialog(Window owner, PlaceRepository placeRepository,
                            PlaceVoteRepository placeVoteRepository, Room room) {
        super(owner, "장소 확인", ModalityType.APPLICATION_MODAL);
        this.placeRepository = placeRepository;
        this.placeVoteRepository = placeVoteRepository;
        this.room = room;
        initDialog();
        initComponents();
    }

    /** 다이얼로그(창) 자체의 크기, 위치 등 기본 속성을 설정합니다. (다른 화면과 동일한 432x768 크기 유지) */
    private void initDialog() {
        setSize(432, 768);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부 컴포넌트(제목, 추가 입력칸, 장소 목록, 닫기 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(room.getName() + " · 장소 확인");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ---------- 새 장소 추가 영역 ----------
        JLabel addLabel = sectionLabel("새 장소 추가");

        newPlaceField = new JTextField();
        Theme.styleTextField(newPlaceField);
        newPlaceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        newPlaceField.setAlignmentX(Component.LEFT_ALIGNMENT);
        newPlaceField.addActionListener(e -> handleAddPlace()); // 입력창에서 Enter 키를 눌러도 추가되도록 함

        JButton addButton = new JButton("추가");
        Theme.styleSecondaryButton(addButton);
        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        addButton.addActionListener(e -> handleAddPlace());

        // ---------- 장소 목록 영역 ----------
        JLabel listLabel = sectionLabel("등록된 장소 목록");

        placesPanel = new JPanel();
        placesPanel.setLayout(new BoxLayout(placesPanel, BoxLayout.Y_AXIS));
        placesPanel.setOpaque(false);
        placesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton closeButton = new JButton("닫기");
        Theme.styleButton(closeButton);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        closeButton.addActionListener(e -> dispose());

        // 장소 목록을 먼저 채운 뒤, 이름이 길어도 잘리지 않도록 가로 스크롤 영역으로 감쌉니다.
        renderPlaces();
        placesScrollPane = Theme.wrapHorizontalScrollable(placesPanel, Theme.STANDARD_CONTENT_WIDTH);
        placesScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(18));
        root.add(addLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(newPlaceField);
        root.add(Box.createVerticalStrut(8));
        root.add(addButton);
        root.add(Box.createVerticalStrut(20));
        root.add(listLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(placesScrollPane);
        root.add(Box.createVerticalStrut(20));
        root.add(closeButton);

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

    /** "추가" 버튼(또는 Enter 키) 클릭 시 실행됩니다. 입력한 이름을 장소 목록에 추가합니다. */
    private void handleAddPlace() {
        String name = newPlaceField.getText();
        boolean added = placeRepository.addPlace(room.getCode(), name);

        if (!added) {
            // 이름이 비어있거나, 이미 등록된 이름과 겹치는 경우
            JOptionPane.showMessageDialog(this,
                    "장소 이름을 입력해주세요. (이미 등록된 이름은 다시 추가할 수 없습니다)",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        newPlaceField.setText("");
        renderPlaces();
    }

    /**
     * "삭제" 버튼 클릭 시 실행됩니다.
     * 이 장소를 후보 목록에서 지우고, 이 장소에 대한 투표 기록도 함께 지웁니다.
     * (장소가 사라졌는데 투표 기록만 남아있으면 결과 화면이 이상해지므로 반드시 같이 지워줍니다.)
     */
    private void handleDeletePlace(String place) {
        placeRepository.deletePlace(room.getCode(), place);
        placeVoteRepository.deleteVotesForPlace(room.getCode(), place);
        renderPlaces();
    }

    /** 지금 저장된 장소 목록을 다시 읽어와서 화면을 새로 그립니다. */
    private void renderPlaces() {
        placesPanel.removeAll();

        List<String> places = placeRepository.getPlaces(room.getCode());

        if (places.isEmpty()) {
            JLabel emptyLabel = new JLabel("아직 등록된 장소가 없습니다.");
            emptyLabel.setFont(Theme.FONT_NORMAL);
            emptyLabel.setForeground(new Color(0x99, 0x99, 0x99));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            placesPanel.add(emptyLabel);
        } else {
            for (String place : places) {
                placesPanel.add(createPlaceRow(place));
                placesPanel.add(Box.createVerticalStrut(6));
            }
        }

        placesPanel.revalidate();
        placesPanel.repaint();

        // 이미 가로 스크롤 영역으로 감싼 뒤(=화면이 처음 열리고 나서 장소를 추가/삭제한 경우)라면,
        // 새로 바뀐 목록 높이에 맞춰 스크롤 영역 크기도 다시 맞추고 창을 다시 배치합니다.
        // (안 그러면 새로 추가한 장소가 화면에 안 보일 수 있습니다)
        if (placesScrollPane != null) {
            Theme.resyncScrollableHeight(placesScrollPane, placesPanel);
            revalidate();
            repaint();
        }
    }

    /** 장소 목록의 항목 한 줄(장소 이름 + "삭제" 버튼)을 만듭니다. */
    private JPanel createPlaceRow(String place) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.PANEL_BACKGROUND);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel placeLabel = new JLabel(place);
        placeLabel.setFont(Theme.FONT_NORMAL);
        placeLabel.setForeground(Theme.TEXT_DARK);

        JButton deleteButton = new JButton("삭제");
        deleteButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        deleteButton.setForeground(new Color(0x99, 0x99, 0x99));
        deleteButton.setBorderPainted(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> handleDeletePlace(place));

        row.add(placeLabel, BorderLayout.CENTER);
        row.add(deleteButton, BorderLayout.EAST);
        return row;
    }
}
