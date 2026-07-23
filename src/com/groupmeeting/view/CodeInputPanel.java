package com.groupmeeting.view;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;

/**
 * 목업 디자인의 "4자리 숫자 코드 입력 박스"를 구현한 재사용 컴포넌트입니다.
 * 방 생성 화면(코드 지정)과 방 참여 화면(코드 입력) 양쪽에서 공통으로 사용합니다.
 *
 * 동작 방식:
 *  - 4개의 칸에 숫자 1자리씩만 입력 가능
 *  - 숫자를 입력하면 자동으로 다음 칸으로 포커스 이동
 *  - 백스페이스 입력 시, 현재 칸이 비어있으면 이전 칸으로 포커스 이동 후 삭제
 */
public class CodeInputPanel extends JPanel {

    private static final int BOX_COUNT = 4;
    private final JTextField[] boxes = new JTextField[BOX_COUNT];

    public CodeInputPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        setOpaque(false);

        for (int i = 0; i < BOX_COUNT; i++) {
            JTextField box = createDigitBox();
            boxes[i] = box;
            add(box);
        }

        // 각 칸 사이의 포커스 이동(자동 다음칸 이동 / 백스페이스 시 이전칸 이동) 연결
        for (int i = 0; i < BOX_COUNT; i++) {
            final int index = i;
            boxes[i].getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    if (boxes[index].getText().length() >= 1 && index < BOX_COUNT - 1) {
                        boxes[index + 1].requestFocus();
                        boxes[index + 1].selectAll();
                    }
                }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
            });

            // 빈 칸에서 백스페이스를 누르면 이전 칸으로 이동
            boxes[i].addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                            && boxes[index].getText().isEmpty() && index > 0) {
                        boxes[index - 1].requestFocus();
                        boxes[index - 1].selectAll();
                    }
                }
            });
        }
    }

    /** 숫자 한 글자만 입력 가능한 텍스트필드를 생성합니다. */
    private JTextField createDigitBox() {
        JTextField field = new JTextField();
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        field.setPreferredSize(new Dimension(44, 44));
        field.setBorder(BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true));

        // 숫자 1자리만 입력되도록 DocumentFilter로 제한합니다.
        ((PlainDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null) return;
                String digitsOnly = string.replaceAll("[^0-9]", "");
                if (digitsOnly.isEmpty()) return;

                // 이미 글자가 있으면 새로 들어오는 글자로 덮어씀 (칸당 1글자 유지)
                if (fb.getDocument().getLength() > 0) {
                    fb.remove(0, fb.getDocument().getLength());
                }
                fb.insertString(0, digitsOnly.substring(0, 1), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                insertString(fb, offset, text, attrs);
            }
        });

        return field;
    }

    /** 4개 칸에 입력된 숫자를 하나로 합쳐서 반환합니다. (예: "1234") */
    public String getCode() {
        StringBuilder sb = new StringBuilder();
        for (JTextField box : boxes) {
            sb.append(box.getText());
        }
        return sb.toString();
    }

    /** 4자리 코드 문자열을 받아 각 칸에 한 글자씩 채워 넣습니다. */
    public void setCode(String code) {
        if (code == null) return;
        for (int i = 0; i < BOX_COUNT; i++) {
            boxes[i].setText(i < code.length() ? String.valueOf(code.charAt(i)) : "");
        }
    }

    /** 4자리가 모두 채워졌는지 확인합니다. */
    public boolean isComplete() {
        return getCode().length() == BOX_COUNT;
    }

    /** 첫 번째 칸에 포커스를 줍니다. (다이얼로그가 열릴 때 사용) */
    public void focusFirstBox() {
        boxes[0].requestFocus();
    }
}
