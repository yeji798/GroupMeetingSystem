package com.groupmeeting.view.common;

import javax.swing.*;
import java.awt.*;

/**
 * 프로그램 전체에서 사용하는 "눈이 편한 초록색 테마" 색상 및 폰트 상수를 모아둔 클래스입니다.
 * 화면(View) 클래스들은 이 클래스의 상수와 헬퍼 메서드를 사용하여 통일된 디자인을 유지합니다.
 */
public class Theme {

    // ---------------- 색상 ----------------
    public static final Color BACKGROUND = new Color(0xF1, 0xF8, 0xF4);      // 아주 옅은 민트 배경
    public static final Color PANEL_BACKGROUND = new Color(0xFF, 0xFF, 0xFF); // 카드/입력 패널 배경(흰색)
    public static final Color PRIMARY_GREEN = new Color(0x4C, 0xAF, 0x50);    // 메인 버튼 색상
    public static final Color PRIMARY_GREEN_DARK = new Color(0x38, 0x8E, 0x3C); // 버튼 hover/강조
    public static final Color ACCENT_GREEN = new Color(0xA5, 0xD6, 0xA7);     // 보조 강조 색상(연한 초록)
    public static final Color TEXT_DARK = new Color(0x2E, 0x3D, 0x2F);        // 기본 텍스트 색
    public static final Color BORDER_GREEN = new Color(0x81, 0xC7, 0x84);     // 테두리 색상

    // ---------------- 폰트 ----------------
    public static final Font FONT_TITLE = new Font("맑은 고딕", Font.BOLD, 26);
    public static final Font FONT_SUBTITLE = new Font("맑은 고딕", Font.BOLD, 16);
    public static final Font FONT_NORMAL = new Font("맑은 고딕", Font.PLAIN, 14);
    public static final Font FONT_BUTTON = new Font("맑은 고딕", Font.BOLD, 14);

    /**
     * 버튼에 초록 테마 스타일(배경색, 글자색, 폰트, 테두리 없음 등)을 일괄 적용합니다.
     */
    public static void styleButton(JButton button) {
        button.setBackground(PRIMARY_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(FONT_BUTTON);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * 보조(덜 강조되는) 버튼 스타일. 배경은 흰색, 글자와 테두리는 초록색으로 처리합니다.
     */
    public static void styleSecondaryButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(PRIMARY_GREEN_DARK);
        button.setFont(FONT_BUTTON);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 1, true));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * 텍스트 입력 필드에 공통 스타일(테두리, 폰트)을 적용합니다.
     */
    public static void styleTextField(JTextField field) {
        field.setFont(FONT_NORMAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    /**
     * BoxLayout(Y_AXIS)로 세로 배치된 화면에서, 넘겨받은 컴포넌트들의 가로 폭을 전부
     * 똑같이(그 중 가장 넓은 컴포넌트의 폭으로) 맞추고 화면 가운데로 정렬합니다.
     *
     * -> BoxLayout 안에서 어떤 줄은 왼쪽 정렬, 어떤 줄은 화면 전체 폭으로 늘어나는 식으로
     *    제각각이면 화면이 한쪽으로 쏠려 보이는 문제가 생깁니다. 이 메서드로 여러 줄의
     *    폭을 통일하고 다같이 가운데 정렬하면 그 문제를 확실하게 막을 수 있습니다.
     */
    public static void alignAsCenteredColumn(JComponent... components) {
        int maxWidth = 0;
        for (JComponent c : components) {
            maxWidth = Math.max(maxWidth, c.getPreferredSize().width);
        }
        for (JComponent c : components) {
            int height = c.getPreferredSize().height;
            Dimension fixedSize = new Dimension(maxWidth, height);
            c.setPreferredSize(fixedSize);
            c.setMaximumSize(fixedSize);
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
    }

    /**
     * 대부분의 화면(다이얼로그)에서 좌우 여백을 뺀 뒤 실제로 내용이 들어갈 수 있는
     * 표준 가로 폭입니다. (창 폭 432px - 좌우 여백 20px씩 - 세로 스크롤바 자리)
     * 목록(리스트) 화면의 가로 스크롤 영역 폭을 정할 때 기준으로 사용합니다.
     */
    public static final int STANDARD_CONTENT_WIDTH = 370;

    /**
     * BoxLayout의 세로 열에서 입력 요소의 폭을 표준 폭으로 통일하고 가운데에 둡니다.
     * JLabel의 글자는 왼쪽 정렬을 유지하면서, 라벨의 영역 자체만 중앙에 배치됩니다.
     */
    public static void centerAtStandardWidth(JComponent... components) {
        for (JComponent component : components) {
            int height = component.getPreferredSize().height;
            Dimension size = new Dimension(STANDARD_CONTENT_WIDTH, height);
            component.setPreferredSize(size);
            component.setMaximumSize(size);
            component.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
    }

    /**
     * 이름이나 날짜처럼 길이가 들쭉날쭉한 텍스트가 들어가는 목록 패널(listPanel)을
     * "가로 스크롤이 가능한" 영역으로 감쌉니다.
     *
     * -> 목록 안의 글자가 화면 폭보다 길면 예전에는 그냥 잘려서 안 보였는데, 이 메서드로 감싸면
     *    옆으로 스크롤해서 잘린 내용을 전부 볼 수 있게 됩니다. 세로 스크롤은 이 화면을 감싸고
     *    있는 바깥 스크롤 영역이 이미 담당하고 있으므로, 여기서는 가로 스크롤만 켭니다.
     *    (목록의 세로 높이는 내용물 그대로 사용하고, 가로 폭만 visibleWidth로 고정합니다)
     */
    public static JScrollPane wrapHorizontalScrollable(JComponent listPanel, int visibleWidth) {
        Dimension panelPreferredSize = listPanel.getPreferredSize();
        listPanel.setPreferredSize(new Dimension(
                Math.max(visibleWidth, panelPreferredSize.width),
                panelPreferredSize.height
        ));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 목록 패널이 지금까지 쌓은 내용물의 "원래 필요한 세로 높이"는 그대로 유지하고,
        // 가로 폭만 visibleWidth로 고정합니다. (내용이 더 넓으면 안에서 가로 스크롤이 생김)
        int neededHeight = listPanel.getPreferredSize().height;
        Dimension fixedSize = new Dimension(visibleWidth, neededHeight);
        scrollPane.setPreferredSize(fixedSize);
        scrollPane.setMaximumSize(fixedSize);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        return scrollPane;
    }

    /**
     * wrapHorizontalScrollable()로 감싸둔 스크롤 영역의 세로 높이를, 목록 패널(listPanel)의
     * "지금" 필요한 높이에 맞춰 다시 계산해줍니다.
     *
     * -> 목록에 항목을 추가/삭제해서 화면이 이미 열려있는 도중에 내용의 세로 길이가 바뀌는
     *    경우(예: "추가" 버튼으로 새 항목을 넣을 때), 스크롤 영역의 높이가 처음 만들었을 때
     *    값으로 고정된 채로 있으면 새로 추가된 내용이 화면에 안 보일 수 있습니다.
     *    항목을 추가/삭제한 뒤에는 항상 이 메서드를 호출해서 높이를 새로 맞춰줘야 합니다.
     */
    public static void resyncScrollableHeight(JScrollPane scrollPane, JComponent listPanel) {
        int width = scrollPane.getPreferredSize().width; // 가로 폭은 그대로 유지
        int height = listPanel.getPreferredSize().height;
        Dimension panelSize = listPanel.getPreferredSize();
        listPanel.setPreferredSize(new Dimension(
                Math.max(width, panelSize.width),
                height
        ));
        Dimension size = new Dimension(width, height);
        scrollPane.setPreferredSize(size);
        scrollPane.setMaximumSize(size);
    }
}
