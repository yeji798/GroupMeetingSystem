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
     * 목록 스크롤 영역이 가질 수 있는 최대 세로 높이(px)입니다. 목록이 이보다 짧으면
     * 내용물 높이 그대로 보여주고, 이보다 길어지면 이 높이로 고정된 채 안에서 세로
     * 스크롤이 생깁니다. (그 아래에 있는 "닫기"/"저장" 같은 버튼이 화면 밖으로 밀려나
     * 잘려 보이지 않도록, 목록 쪽의 높이를 여기서 미리 못박아 두는 것입니다)
     */
    private static final int LIST_MAX_HEIGHT = 280;

    /**
     * 이름이나 날짜처럼 길이가 들쭉날쭉한 텍스트가 들어가는 목록 패널(listPanel)을
     * "가로+세로 스크롤이 가능한" 영역으로 감쌉니다.
     *
     * -> 목록 안의 글자가 화면 폭보다 길면 옆으로 스크롤해서 볼 수 있고, 목록 자체가
     *    LIST_MAX_HEIGHT보다 길어지면(항목이 많아지면) 그 높이에서 고정되고 안에서
     *    세로 스크롤이 생겨서 전부 볼 수 있습니다. 이렇게 목록의 세로 높이를 여기서
     *    미리 상한선으로 묶어두기 때문에, 바깥의 "닫기"/"저장" 버튼은 항상 목록 바로
     *    아래 제자리에 남아있고 화면 밖으로 밀려나 잘리지 않습니다.
     */
    public static JScrollPane wrapHorizontalScrollable(JComponent listPanel, int visibleWidth) {
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);
        // 뷰포트의 "블릿 스크롤" 최적화(화면 일부만 복사해서 스크롤 처리)를 쓰면, 목록
        // 내용이 바뀐 직후 화면이 곧바로 다시 그려지지 않고 예전 모습이 남아있는 경우가
        // 있습니다. 매번 전체를 새로 그리도록 강제해서 "추가했는데 창을 나갔다 들어와야
        // 보인다" 같은 문제를 원천적으로 막습니다.
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scrollPane.setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        applyBoundedHeight(scrollPane, listPanel, visibleWidth);

        return scrollPane;
    }

    /**
     * wrapHorizontalScrollable()로 감싸둔 스크롤 영역의 세로 높이를, 목록 패널(listPanel)의
     * "지금" 필요한 높이(LIST_MAX_HEIGHT로 상한)에 맞춰 다시 계산해줍니다.
     *
     * -> 목록에 항목을 추가/삭제해서 화면이 이미 열려있는 도중에 내용의 세로 길이가 바뀌는
     *    경우(예: "추가" 버튼으로 새 항목을 넣을 때) 항상 이 메서드를 호출해서 다시 맞춰줘야
     *    합니다. LIST_MAX_HEIGHT를 넘는 만큼은 스크롤 영역 안에서 세로 스크롤로 볼 수 있습니다.
     */
    public static void resyncScrollableHeight(JScrollPane scrollPane, JComponent listPanel) {
        int width = scrollPane.getPreferredSize().width; // 가로 폭은 그대로 유지
        applyBoundedHeight(scrollPane, listPanel, width);

        // 크기가 바뀐 스크롤 영역 자신부터 확실하게 다시 배치를 요청합니다. (scrollPane은
        // JComponent이므로 revalidate()가 항상 정상적으로 동작합니다 - 반면 다이얼로그
        // 자신의 revalidate()는 최상위 창이라 parent가 없어 아무 효과가 없을 수 있습니다)
        scrollPane.revalidate();
        scrollPane.repaint();

        // 여기서 한 번 더, 이 스크롤 영역이 속한 창(다이얼로그) 전체를 "지금 당장" 강제로
        // 다시 배치·그리도록 합니다. validate()는 revalidate()와 달리 나중으로 미뤄지지
        // 않고 즉시 동기적으로 레이아웃을 다시 계산하므로, "추가" 버튼을 눌렀는데 화면에는
        // 반영되지 않고 창을 나갔다 들어와야 보이는 문제를 확실하게 막아줍니다.
        Window window = SwingUtilities.getWindowAncestor(scrollPane);
        if (window != null) {
            window.validate();
            window.repaint();
        }
    }

    /**
     * 목록 패널(listPanel)의 "지금" 진짜 필요한 높이를 다시 측정해서, LIST_MAX_HEIGHT를
     * 넘지 않는 선에서 스크롤 영역의 높이를 고정합니다.
     *
     * -> listPanel은 이 메서드가 끝날 때 항상 setPreferredSize()로 크기가 고정된 상태로
     *    남는데, 만약 다음 번 호출 때 그 고정된 값을 그대로 다시 읽으면 그 사이에 추가/삭제된
     *    자식 컴포넌트가 전혀 반영되지 않은 "옛날 크기"를 계속 재사용하게 되는 문제가 있습니다
     *    (한 번 setPreferredSize()를 호출하면 그 뒤로는 getPreferredSize()가 실제 자식
     *    구성과 상관없이 그 고정값만 돌려주기 때문입니다). 그래서 측정하기 직전에 먼저
     *    setPreferredSize(null)로 고정을 풀어, BoxLayout이 지금 자식들을 기준으로 새로
     *    계산한 "진짜" 높이를 읽은 뒤에 다시 고정합니다.
     */
    private static void applyBoundedHeight(JScrollPane scrollPane, JComponent listPanel, int width) {
        listPanel.setPreferredSize(null);
        Dimension trueContentSize = listPanel.getPreferredSize();
        int contentWidth = Math.max(width, trueContentSize.width);
        int contentHeight = trueContentSize.height;
        listPanel.setPreferredSize(new Dimension(contentWidth, contentHeight));

        int boundedHeight = Math.min(contentHeight, LIST_MAX_HEIGHT);
        Dimension size = new Dimension(width, boundedHeight);
        scrollPane.setPreferredSize(size);
        scrollPane.setMaximumSize(size);
    }
}
