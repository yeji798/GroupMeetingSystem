package com.groupmeeting.util;

import java.util.List;
import java.util.Random;

public class PlaceRecommender {
    
    /**
     * 참여자들이 입력한 장소 리스트 중 하나를 랜덤으로 추천합니다.
     * 
     * @param places 후보 장소 리스트
     * @return 랜덤으로 선정된 장소 이름
     */
    public static String recommendRandomPlace(List<String> places) {
        // 후보 장소가 없을 경우의 예외 처리
        if (places == null || places.isEmpty()) {
            return "등록된 후보 장소가 없습니다.";
        }
        
        // 순수 자바의 Random 클래스 활용
        Random random = new Random();
        int randomIndex = random.nextInt(places.size());
        
        return places.get(randomIndex);
    }
}