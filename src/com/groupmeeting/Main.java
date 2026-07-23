/** 
 * Main.java
 * 
 * 1. 자연스러운 UI를 위해 Look & Feel 가져옴 
 *      --> 부가적인 요소이므로 오류날 경우를 대비해 오류처리함
 * 2. Swing을 GUI 전용 스레드인 EDT에서만 생성.수정
 * 3. view.LoginView.java를 통해 로그인 뷰 생성 & 화면에 표시
 * 
 * 
 * 
 * 서연아ㅏ 안녕
 */


package com.groupmeeting;
import com.groupmeeting.view.LoginView;
import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // 운영체제의 기본 Look & Feel을 적용하여 자연스러운 UI 만듦 (*Look & Feel : GUI의 디자인과 모양)
            // - UIManager.getSystemLookAndFeelClassName() : 현재 운영체제에 맞는 Look & Feel의 클래스 이름을 가져옴
        } catch (Exception e) {
            System.err.println("시스템 Look & Feel 적용 실패: " + e.getMessage());
            // Look & Feel 적용에 실패해도 프로그램 실행에는 문제가 없으므로!! 
        }


        // Swing은 GUI를 전용 스레드인 EDT(Event Dispatch Thread) 에서만 안전하게 생성·수정해야 한다.
        SwingUtilities.invokeLater(() -> { //GUI 담당 스레드에게 코드 실행하라고 명령 (GUI는 EDT 한 곳에서만 관리)
            LoginView loginView = new LoginView(); // view.LoginView.java를 통해 로그인 뷰 생성 
            loginView.setVisible(true); // 화면에 표시
        });
    }
}
