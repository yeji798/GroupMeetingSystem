package com.groupmeeting.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 단체 모임 방 정보를 담는 모델(VO) 클래스입니다.
 * CSV 파일(rooms.csv) 한 줄이 Room 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: code, name, category, ownerId, members(세미콜론으로 구분된 회원 아이디 목록)
 */
public class Room {

    /** 카테고리 상수: 단체 약속 */
    public static final String CATEGORY_PROMISE = "단체 약속";
    /** 카테고리 상수: 단체 여행 */
    public static final String CATEGORY_TRAVEL = "단체 여행";

    private String code;          // 4자리 방 코드 (1000~9999)
    private String name;          // 방 이름
    private String category;      // 카테고리 (단체 약속 / 단체 여행)
    private String ownerId;       // 방장(생성자)의 로그인 아이디
    private List<String> memberIds; // 현재 방에 참여 중인 회원들의 아이디 목록

    public Room(String code, String name, String category, String ownerId, List<String> memberIds) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.ownerId = ownerId;
        this.memberIds = (memberIds != null) ? memberIds : new ArrayList<>();
    }

    // ---------------- Getter ----------------
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getOwnerId() { return ownerId; }
    public List<String> getMemberIds() { return memberIds; }

    // ---------------- Setter ----------------
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }

    /** 현재 참여 인원 수를 반환합니다. */
    public int getMemberCount() {
        return memberIds.size();
    }

    /** 특정 회원이 이미 이 방에 참여 중인지 확인합니다. */
    public boolean hasMember(String memberId) {
        return memberIds.contains(memberId);
    }

    @Override
    public String toString() {
        return "Room{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", members=" + memberIds.size() +
                '}';
    }
}
