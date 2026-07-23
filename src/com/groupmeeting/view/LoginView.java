package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;

/**
 * 프로그램 실행 시 가장 먼저 보여지는 메인(로그인) 화면입니다.
 *
 * 구성 요소:
 *  - 중앙 상단: "단체 모임 관리 시스템" 타이틀
 *  - 아이디 입력칸, 비밀번호 입력칸
 *  - "로그인" 버튼
 *  - "회원가입" 버튼 (하단, 회원가입 다이얼로그 오픈)
 */
public class LoginView extends JFrame {

    // 회원 정보 CSV 파일을 다루는 저장소 객체
    private final MemberRepository memberRepository = new MemberRepository();

    // 아이디/비밀번호 입력 필드 (버튼 클릭 이벤트에서 값을 읽어야 하므로 필드로 선언)
    private JTextField idField;
    private JPasswordField pwField;

    public LoginView() {
        initFrame();
        initComponents();
    }

    /** JFrame 자체의 기본 속성(제목, 크기, 닫기 동작 등)을 설정합니다. */
    private void initFrame() {
        setTitle("단체 모임 관리 시스템 - 로그인");
        setSize(360, 640);
        setLocationRelativeTo(null); // 화면 중앙에 표시
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 화면 내부의 컴포넌트(라벨, 입력창, 버튼 등)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // ---------- 아이콘 + 타이틀 (목업 디자인: 사람 아이콘 + 2줄 타이틀) ----------
        JLabel iconLabel = new JLabel("\uD83D\uDC65", SwingConstants.CENTER); // 👥
        iconLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 44));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("<html><div style='text-align:center;'>단체 모임<br>관리 시스템</div></html>",
                SwingConstants.CENTER);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ---------- 아이디 입력 ----------
        JLabel idLabel = new JLabel("아이디");
        idLabel.setFont(Theme.FONT_NORMAL);
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        idField = new JTextField();
        Theme.styleTextField(idField);
        idField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---------- 비밀번호 입력 ----------
        JLabel pwLabel = new JLabel("비밀번호");
        pwLabel.setFont(Theme.FONT_NORMAL);
        pwLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        pwField = new JPasswordField();
        Theme.styleTextField(pwField);
        pwField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pwField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---------- 로그인 버튼 ----------
        JButton loginButton = new JButton("로그인");
        Theme.styleButton(loginButton);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.addActionListener(e -> handleLogin());

        // ---------- 회원가입 링크 (목업 디자인: 버튼이 아닌 밑줄 텍스트 링크 형태) ----------
        JButton signupButton = new JButton("<html><u>회원가입</u></html>");
        signupButton.setFont(Theme.FONT_NORMAL);
        signupButton.setForeground(Theme.TEXT_DARK);
        signupButton.setContentAreaFilled(false);
        signupButton.setBorderPainted(false);
        signupButton.setFocusPainted(false);
        signupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signupButton.addActionListener(e -> handleOpenSignup());

        // 엔터 키로도 로그인 되도록 처리 (사용성 개선)
        getRootPane().setDefaultButton(loginButton);

        // ---------- 컴포넌트 조립 ----------
        root.add(iconLabel);
        root.add(Box.createVerticalStrut(10));
        root.add(titleLabel);
        root.add(Box.createVerticalStrut(36));
        root.add(idLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(idField);
        root.add(Box.createVerticalStrut(16));
        root.add(pwLabel);
        root.add(Box.createVerticalStrut(6));
        root.add(pwField);
        root.add(Box.createVerticalStrut(28));
        root.add(loginButton);
        root.add(Box.createVerticalStrut(10));
        root.add(signupButton);

        setContentPane(root);
    }

    /**
     * 로그인 버튼 클릭 시 실행됩니다.
     * 입력값 검증 -> CSV 파일 기반 인증 -> 성공 시 메인 화면으로 전환합니다.
     */
    private void handleLogin() {
        String id = idField.getText().trim();
        String password = new String(pwField.getPassword());

        if (id.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 모두 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Member member = memberRepository.authenticate(id, password);

        if (member == null) {
            JOptionPane.showMessageDialog(this, "아이디 또는 비밀번호가 일치하지 않습니다.",
                    "로그인 실패", JOptionPane.ERROR_MESSAGE);
            pwField.setText("");
            return;
        }

        // 로그인 성공 -> 메인 화면(MainView)으로 전환
        JOptionPane.showMessageDialog(this,
                member.getNickname() + "님, 환영합니다!",
                "로그인 성공", JOptionPane.INFORMATION_MESSAGE);

        MainView mainView = new MainView(member);
        mainView.setVisible(true);
        this.dispose(); // 로그인 창은 닫음
    }

    /** 회원가입 버튼 클릭 시 회원가입 다이얼로그를 모달로 띄웁니다. */
    private void handleOpenSignup() {
        SignupDialog signupDialog = new SignupDialog(this, memberRepository);
        signupDialog.setVisible(true);

        // 회원가입이 성공적으로 완료되었다면, 편의를 위해 방금 가입한 아이디를 자동 입력해줍니다.
        String newlyRegisteredId = signupDialog.getRegisteredId();
        if (newlyRegisteredId != null) {
            idField.setText(newlyRegisteredId);
            pwField.requestFocus();
        }
    }
}
