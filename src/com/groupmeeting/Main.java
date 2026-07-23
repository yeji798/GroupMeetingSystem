/** 
 * Main.java
 */

package com.groupmeeting;

import com.groupmeeting.view.LoginView;

import javax.swing.*;

/**
 * 프로그램의 시작점(entry point)입니다.
 * Swing GUI는 항상 이벤트 디스패치 스레드(EDT)에서 실행되어야 하므로
 * SwingUtilities.invokeLater를 사용하여 안전하게 화면을 띄웁니다.
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // 운영체제의 기본 Look & Feel을 적용하여 자연스러운 UI 만듦 (*Look & Feel : GUI의 디자인과 모양)
            // - UIManager.getSystemLookAndFeelClassName() : 현재 운영체제에 맞는 Look & Feel의 클래스 이름을 가져옴
        } catch (Exception e) {
            // Look & Feel 적용에 실패해도 프로그램 실행에는 문제가 없으므로 로그만 남깁니다.
            System.err.println("시스템 Look & Feel 적용 실패: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });
    }
}
