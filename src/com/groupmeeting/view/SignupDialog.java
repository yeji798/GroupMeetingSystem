package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * 회원가입을 위한 모달(Modal) 다이얼로그입니다.
 *
 * 입력 항목: 사용자 이름, 닉네임, 아이디, 비밀번호, 비밀번호 확인, 이메일
 * 가입 완료 시 MemberRepository를 통해 CSV 파일에 저장됩니다.
 */
public class SignupDialog extends JDialog {

    private final MemberRepository memberRepository;

    // 이메일 형식 검증용 정규식 (간단한 형식 체크)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    // 입력 필드들
    private JTextField nameField;
    private JTextField nicknameField;
    private JTextField idField;
    private JPasswordField pwField;
    private JPasswordField pwConfirmField;
    private JTextField emailField;

    // 회원가입에 성공했을 때, 방금 가입한 아이디를 저장해두는 필드
    // (LoginView에서 로그인 아이디 자동입력을 위해 사용)
    private String registeredId = null;

    public SignupDialog(JFrame owner, MemberRepository memberRepository) {
        super(owner, "회원가입", true); // true -> 모달 다이얼로그
        this.memberRepository = memberRepository;
        initDialog();
        initComponents();
    }

    /** 다이얼로그 자체의 크기, 위치 등을 설정합니다. */
    private void initDialog() {
        setSize(360, 640);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    /** 다이얼로그 내부 컴포넌트를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel titleLabel = new JLabel("회원가입");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(20));

        // 각 입력 항목을 "라벨 + 입력창" 형태로 만들어 순서대로 추가
        nameField = addLabeledField(root, "사용자 이름");
        nicknameField = addLabeledField(root, "닉네임");
        idField = addLabeledField(root, "아이디");
        pwField = addLabeledPasswordField(root, "비밀번호");
        pwConfirmField = addLabeledPasswordField(root, "비밀번호 확인");
        emailField = addLabeledField(root, "이메일");

        root.add(Box.createVerticalStrut(20));

        JButton submitButton = new JButton("가입하기");
        Theme.styleButton(submitButton);
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        submitButton.addActionListener(e -> handleSubmit());

        JButton cancelButton = new JButton("취소");
        Theme.styleSecondaryButton(cancelButton);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        cancelButton.addActionListener(e -> dispose());

        root.add(submitButton);
        root.add(Box.createVerticalStrut(8));
        root.add(cancelButton);

        setContentPane(root);
    }

    /** 라벨 + 일반 텍스트필드를 root 패널에 추가하고, 생성된 텍스트필드를 반환합니다. */
    private JTextField addLabeledField(JPanel root, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_NORMAL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField field = new JTextField();
        Theme.styleTextField(field);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(label);
        root.add(Box.createVerticalStrut(4));
        root.add(field);
        root.add(Box.createVerticalStrut(12));

        return field;
    }

    /** 라벨 + 비밀번호 입력필드를 root 패널에 추가하고, 생성된 필드를 반환합니다. */
    private JPasswordField addLabeledPasswordField(JPanel root, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_NORMAL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField field = new JPasswordField();
        Theme.styleTextField(field);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(label);
        root.add(Box.createVerticalStrut(4));
        root.add(field);
        root.add(Box.createVerticalStrut(12));

        return field;
    }

    /**
     * "가입하기" 버튼 클릭 시 실행되는 메서드입니다.
     * 입력값 검증 -> 아이디 중복 확인 -> CSV 파일에 저장 순서로 진행합니다.
     */
    private void handleSubmit() {
        String name = nameField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String id = idField.getText().trim();
        String password = new String(pwField.getPassword());
        String passwordConfirm = new String(pwConfirmField.getPassword());
        String email = emailField.getText().trim();

        // 1) 필수 입력값 검증
        if (name.isEmpty() || nickname.isEmpty() || id.isEmpty()
                || password.isEmpty() || email.isEmpty()) {
            showWarning("모든 항목을 빠짐없이 입력해주세요.");
            return;
        }

        // 2) 비밀번호 길이 검증 (간단한 정책: 4자 이상)
        if (password.length() < 4) {
            showWarning("비밀번호는 4자 이상 입력해주세요.");
            return;
        }

        // 3) 비밀번호 확인 일치 검증
        if (!password.equals(passwordConfirm)) {
            showWarning("비밀번호가 일치하지 않습니다.");
            pwField.setText("");
            pwConfirmField.setText("");
            return;
        }

        // 4) 이메일 형식 검증
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showWarning("이메일 형식이 올바르지 않습니다.");
            return;
        }

        // 5) 아이디 중복 확인 (CSV 파일 조회)
        if (memberRepository.isIdDuplicate(id)) {
            showWarning("이미 사용 중인 아이디입니다.");
            return;
        }

        // 6) 모든 검증 통과 -> 회원 정보 저장
        Member newMember = new Member(name, nickname, id, password, email);
        boolean saved = memberRepository.addMember(newMember);

        if (saved) {
            registeredId = id; // LoginView에서 참조할 수 있도록 저장
            JOptionPane.showMessageDialog(this,
                    nickname + "님, 회원가입이 완료되었습니다!\n로그인 화면에서 로그인해주세요.",
                    "회원가입 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            showWarning("회원가입 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /** 경고 메시지를 표시하는 공통 헬퍼 메서드입니다. */
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 회원가입이 성공적으로 완료된 경우 방금 가입한 아이디를 반환합니다.
     * 가입에 실패했거나 취소한 경우 null을 반환합니다.
     */
    public String getRegisteredId() {
        return registeredId;
    }
}
