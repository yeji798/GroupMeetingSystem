package com.groupmeeting.model;

/**
 * 회원 정보를 담는 모델(VO) 클래스입니다.
 * CSV 파일 한 줄이 Member 객체 하나에 대응됩니다.
 *
 * CSV 컬럼 순서: name, nickname, id, password, email
 */
public class Member {

    private String name;      // 사용자 이름 (실명)
    private String nickname;  // 닉네임 (화면에 표시되는 이름)
    private String id;        // 로그인 아이디
    private String password;  // 로그인 비밀번호
    private String email;     // 이메일 주소

    public Member(String name, String nickname, String id, String password, String email) {
        this.name = name;
        this.nickname = nickname;
        this.id = id;
        this.password = password;
        this.email = email;
    }

    // ---------------- Getter ----------------
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getId() { return id; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    // ---------------- Setter ----------------
    // 추후 "회원 정보 수정" 기능 구현 시 사용할 수 있도록 setter도 함께 제공합니다.
    public void setName(String name) { this.name = name; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "Member{" +
                "name='" + name + '\'' +
                ", nickname='" + nickname + '\'' +
                ", id='" + id + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
