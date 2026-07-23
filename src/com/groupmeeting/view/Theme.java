package com.groupmeeting.view;

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
}
