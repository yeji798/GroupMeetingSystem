/**
 * ProfileEditDialog.java
 *
 * "마이페이지"에서 뜨는 회원 정보 수정 화면입니다. 이름/닉네임/이메일을 고칠 수 있고,
 * 비밀번호는 바꾸고 싶을 때만 입력하면 됩니다(빈 칸으로 두면 기존 비밀번호가 그대로 유지됨).
 * 아이디(로그인 아이디)는 고칠 수 없는 값이라 읽기 전용으로만 보여줍니다.
 *
 *   <필드>
 *   1. memberRepository : 회원 정보 CSV를 읽고 쓰는 저장소 객체
 *   2. loginMember       : 지금 로그인해서 정보를 수정하려는 사용자 (수정 성공 시 이 객체를 직접 고쳐서
 *                           반환하므로, 이 다이얼로그를 연 다른 화면들도 자동으로 최신 정보를 보게 됨)
 *
 *   <중요 메소드>
 *   1. handleSave() : "저장" 버튼 -> 입력값 검증 -> loginMember 필드를 직접 수정 -> CSV에 반영
 */

package com.groupmeeting.view;

import com.groupmeeting.model.Member;
import com.groupmeeting.util.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public class ProfileEditDialog extends JDialog {

    private final MemberRepository memberRepository;
    private final Member loginMember;

    // 이메일 형식 검증용 정규식 (SignupDialog와 동일한 간단한 형식 체크)
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private JTextField nameField;
    private JTextField nicknameField;
    private JTextField emailField;
    private JPasswordField newPasswordField;
    private JPasswordField newPasswordConfirmField;

    public ProfileEditDialog(Window owner, MemberRepository memberRepository, Member loginMember) {
        super(owner, "마이페이지", ModalityType.APPLICATION_MODAL);
        this.memberRepository = memberRepository;
        this.loginMember = loginMember;
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

    /** 화면 내부 컴포넌트(아이디 표시, 이름/닉네임/이메일/비밀번호 입력칸, 저장 버튼)를 배치합니다. */
    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("회원 정보 수정");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLabel = sectionLabel("아이디");
        JLabel idValueLabel = new JLabel(loginMember.getId());
        idValueLabel.setFont(Theme.FONT_NORMAL);
        idValueLabel.setForeground(new Color(0x99, 0x99, 0x99));
        idValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        idValueLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(20));
        root.add(idLabel);
        root.add(Box.createVerticalStrut(4));
        root.add(idValueLabel);
        root.add(Box.createVerticalStrut(14));

        nameField = addLabeledField(root, "이름", loginMember.getName());
        nicknameField = addLabeledField(root, "닉네임", loginMember.getNickname());
        emailField = addLabeledField(root, "이메일", loginMember.getEmail());

        JLabel passwordHintLabel = new JLabel("비밀번호를 바꾸고 싶을 때만 입력하세요. (그대로 두면 유지됩니다)");
        passwordHintLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        passwordHintLabel.setForeground(new Color(0x99, 0x99, 0x99));
        passwordHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(passwordHintLabel);
        root.add(Box.createVerticalStrut(10));

        newPasswordField = addLabeledPasswordField(root, "새 비밀번호");
        newPasswordConfirmField = addLabeledPasswordField(root, "새 비밀번호 확인");

        JButton saveButton = new JButton("저장");
        Theme.styleButton(saveButton);
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        saveButton.addActionListener(e -> handleSave());

        JButton cancelButton = new JButton("취소");
        Theme.styleSecondaryButton(cancelButton);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        cancelButton.addActionListener(e -> dispose());

        root.add(Box.createVerticalStrut(10));
        root.add(saveButton);
        root.add(Box.createVerticalStrut(8));
        root.add(cancelButton);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        setContentPane(scrollPane);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_NORMAL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** 라벨 + 일반 텍스트필드를 root에 추가하고, initialValue로 미리 채운 뒤 그 필드를 반환합니다. */
    private JTextField addLabeledField(JPanel root, String labelText, String initialValue) {
        JLabel label = sectionLabel(labelText);

        JTextField field = new JTextField(initialValue);
        Theme.styleTextField(field);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(label);
        root.add(Box.createVerticalStrut(4));
        root.add(field);
        root.add(Box.createVerticalStrut(12));

        return field;
    }

    /** 라벨 + 비밀번호 입력필드를 root에 추가하고 그 필드를 반환합니다. (항상 빈 값으로 시작) */
    private JPasswordField addLabeledPasswordField(JPanel root, String labelText) {
        JLabel label = sectionLabel(labelText);

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
     * "저장" 버튼 클릭 시 실행됩니다.
     * 입력값을 검증한 뒤, loginMember 객체의 필드를 직접 수정하고 CSV에 반영합니다.
     */
    private void handleSave() {
        String name = nameField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String newPassword = new String(newPasswordField.getPassword());
        String newPasswordConfirm = new String(newPasswordConfirmField.getPassword());

        if (name.isEmpty() || nickname.isEmpty() || email.isEmpty()) {
            showWarning("이름, 닉네임, 이메일은 비워둘 수 없습니다.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showWarning("이메일 형식이 올바르지 않습니다.");
            return;
        }

        // 비밀번호는 둘 다 입력했을 때만 바꾼다. (하나만 입력한 경우는 실수로 보고 안내)
        boolean wantsPasswordChange = !newPassword.isEmpty() || !newPasswordConfirm.isEmpty();
        if (wantsPasswordChange) {
            if (newPassword.length() < 4) {
                showWarning("새 비밀번호는 4자 이상 입력해주세요.");
                return;
            }
            if (!newPassword.equals(newPasswordConfirm)) {
                showWarning("새 비밀번호가 서로 일치하지 않습니다.");
                return;
            }
        }

        // loginMember는 이 프로그램 여러 화면이 함께 참조하는 같은 객체이므로, 여기서 값을
        // 직접 바꿔주면 다른 화면(방 메인 화면 등)에도 최신 정보가 자동으로 반영됩니다.
        loginMember.setName(name);
        loginMember.setNickname(nickname);
        loginMember.setEmail(email);
        if (wantsPasswordChange) {
            loginMember.setPassword(newPassword);
        }

        boolean saved = memberRepository.updateMember(loginMember);
        if (saved) {
            JOptionPane.showMessageDialog(this,
                    "회원 정보가 수정되었습니다.",
                    "수정 완료", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            showWarning("회원 정보 수정 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }
}
