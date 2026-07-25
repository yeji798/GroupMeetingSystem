package com.groupmeeting.view;

import com.groupmeeting.util.PlaceRecommender;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PlaceRecommendDialog extends JDialog {

    public PlaceRecommendDialog(JFrame parentFrame, List<String> placeList) {
        super(parentFrame, "모임 장소 랜덤 추천", true);
        
        setSize(350, 350);
        setLocationRelativeTo(parentFrame);
        setLayout(new BorderLayout(10, 10));

        // 상단 안내문
        JLabel titleLabel = new JLabel("현재 취합된 장소 후보입니다.", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 중앙: 전달받은 장소 리스트(placeList)를 화면에 표시
        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (placeList == null || placeList.isEmpty()) {
            listModel.addElement("등록된 후보 장소가 없습니다.");
        } else {
            for (String place : placeList) {
                // 빈 값이나 널값이 아닌 실제 장소만 목록에 추가
                if(place != null && !place.trim().isEmpty()) {
                    listModel.addElement("📍 " + place);
                }
            }
        }

        JList<String> placeListUI = new JList<>(listModel);
        placeListUI.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(placeListUI);
        scrollPane.setBorder(BorderFactory.createTitledBorder("후보 장소 목록"));
        add(scrollPane, BorderLayout.CENTER);

        // 하단: 추첨 버튼
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 15, 10));

        JButton drawButton = new JButton("🎲 랜덤 장소 추첨하기 🎲");
        drawButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        // 🎨 Theme 색상 적용 (팀원의 Theme.java 변수명에 맞춰 주석 해제)
        // drawButton.setBackground(Theme.PRIMARY_COLOR); 
        // drawButton.setForeground(Color.WHITE);
        
        bottomPanel.add(drawButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // 랜덤 추첨 버튼 클릭 이벤트
        drawButton.addActionListener(e -> {
            if (placeList == null || placeList.isEmpty()) {
                JOptionPane.showMessageDialog(PlaceRecommendDialog.this, 
                        "추첨할 장소 후보가 없습니다!", "경고", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // PlaceRecommender 로직을 통해 1개 뽑기
            String result = PlaceRecommender.recommendRandomPlace(placeList);
            
            // 결과창 띄우기
            JOptionPane.showMessageDialog(PlaceRecommendDialog.this, 
                    "🎉 당첨된 모임 장소는\n[" + result + "] 입니다! 🎉", 
                    "추첨 결과", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}