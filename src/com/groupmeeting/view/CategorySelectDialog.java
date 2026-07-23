package com.groupmeeting.view;

import com.groupmeeting.model.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 목업 디자인의 "여행 vs 약속 골라!" 화면을 구현한 모달 다이얼로그입니다.
 * 두 개의 카드(단체 약속 / 단체 여행) 중 하나를 클릭하면 해당 값을 결과로 반환합니다.
 */
public class CategorySelectDialog extends JDialog {

    private String selectedCategory = null; // 사용자가 선택한 카테고리 (선택 안 하고 닫으면 null)

    public CategorySelectDialog(Window owner) {
        super(owner, "카테고리 선택", ModalityType.APPLICATION_MODAL);
        initDialog();
        initComponents();
    }

    private void initDialog() {
        setSize(495, 880);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getContentPane().setBackground(Theme.BACKGROUND);
    }

    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("여행 vs 약속 골라!");
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.PRIMARY_GREEN_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(20));

        JPanel promiseCard = createCategoryCard(
                "\uD83D\uDCC5", "단체 약속", "#모임약속  #일정공유",
                new Color(0xE3, 0xF2, 0xFD), Room.CATEGORY_PROMISE
        );
        JPanel travelCard = createCategoryCard(
                "\uD83E\uDDF3", "단체 여행", "#해외여행  #국내여행  #일정계획",
                new Color(0xE8, 0xF5, 0xE9), Room.CATEGORY_TRAVEL
        );

        root.add(promiseCard);
        root.add(Box.createVerticalStrut(16));
        root.add(travelCard);

        setContentPane(root);
    }

    /** 카테고리 카드 하나(아이콘 + 제목 + 해시태그)를 생성하고, 클릭 시 선택되도록 처리합니다. */
    private JPanel createCategoryCard(String emoji, String title, String hashtags,
                                       Color backgroundColor, String categoryValue) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(backgroundColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GREEN, 1, true),
                BorderFactory.createEmptyBorder(20, 16, 20, 16)
        ));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 32));
        emojiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SUBTITLE);
        titleLabel.setForeground(Theme.TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagLabel = new JLabel(hashtags);
        tagLabel.setFont(Theme.FONT_NORMAL);
        tagLabel.setForeground(new Color(0x77, 0x77, 0x77));
        tagLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(emojiLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(tagLabel);

        // 카드를 클릭하면 해당 카테고리를 선택하고 다이얼로그를 닫습니다.
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCategory = categoryValue;
                dispose();
            }
        });

        return card;
    }

    /** 사용자가 선택한 카테고리를 반환합니다. 선택 없이 닫힌 경우 null을 반환합니다. */
    public String getSelectedCategory() {
        return selectedCategory;
    }
}
