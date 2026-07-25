package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import com.groupmeeting.model.Member;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 회원 정보를 CSV 파일로 저장하고 불러오는 저장소(Repository) 클래스입니다.
 * DB 없이 파일 입출력만으로 회원 데이터를 영속화합니다.
 *
 * 파일 경로: data/members.csv
 * 컬럼 순서: name, nickname, id, password, email
 */
public class MemberRepository {

    // data 폴더와 회원 정보 CSV 파일 경로
    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "members.csv";

    // CSV 파일의 헤더(첫 줄)
    private static final String HEADER = "name,nickname,id,password,email";

    /**
     * 생성자에서 data 폴더 및 members.csv 파일이 없으면 새로 생성합니다.
     * 프로그램을 처음 실행했을 때도 오류 없이 동작하도록 하기 위함입니다.
     */
    public MemberRepository() {
        ensureFileExists();
    }

    /** data 디렉터리와 CSV 파일이 존재하는지 확인하고, 없으면 헤더만 있는 파일을 생성합니다. */
    private void ensureFileExists() {
        try {
            Path dirPath = Paths.get(DATA_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = Paths.get(FILE_PATH);
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // 파일 시스템 접근에 실패한 경우, 콘솔에 오류를 출력합니다.
            // (실제 서비스라면 로깅 프레임워크를 사용하는 것이 좋습니다.)
            System.err.println("회원 정보 파일 초기화 실패: " + e.getMessage());
        }
    }

    /**
     * CSV 파일에 저장된 모든 회원 정보를 읽어서 리스트로 반환합니다.
     */
    public List<Member> loadAll() {
        List<Member> members = new ArrayList<>();
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 첫 줄(헤더)은 데이터로 취급하지 않고 건너뜁니다.
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue; // 빈 줄은 무시
                }

                List<String> tokens = CsvUtil.parseLine(line);
                if (tokens.size() >= 5) {
                    Member member = new Member(
                            tokens.get(0), // name
                            tokens.get(1), // nickname
                            tokens.get(2), // id
                            tokens.get(3), // password
                            tokens.get(4)  // email
                    );
                    members.add(member);
                }
            }
        } catch (IOException e) {
            System.err.println("회원 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return members;
    }

    /**
     * 회원 목록 전체를 CSV 파일에 덮어씁니다. (헤더 포함)
     * 회원 정보 수정, 탈퇴 등 전체 목록이 바뀌는 경우에 사용합니다.
     */
    public void saveAll(List<Member> members) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Member m : members) {
                String line = CsvUtil.toCsvLine(
                        m.getName(), m.getNickname(), m.getId(), m.getPassword(), m.getEmail()
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("회원 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 새로운 회원 한 명을 CSV 파일 맨 끝에 추가합니다.
     * (파일 전체를 다시 쓰지 않고 한 줄만 append 하여 효율적으로 처리합니다.)
     *
     * @return 저장에 성공하면 true, 실패하면 false
     */
    public boolean addMember(Member member) {
        Path filePath = Paths.get(FILE_PATH);
        String line = CsvUtil.toCsvLine(
                member.getName(), member.getNickname(), member.getId(),
                member.getPassword(), member.getEmail()
        );

        try {
            Files.writeString(
                    filePath,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            System.err.println("회원 정보 추가 중 오류가 발생했습니다: " + e.getMessage());
            return false;
        }
    }

    /**
     * 회원 정보를 수정합니다. 아이디(id)가 같은 회원을 찾아서 그 회원의 정보를 통째로 교체합니다.
     * ("마이페이지 - 회원 정보 수정" 화면에서 사용)
     *
     * @return 수정에 성공하면 true, 아이디를 찾지 못했다면 false
     */
    public boolean updateMember(Member updated) {
        List<Member> members = loadAll();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId().equals(updated.getId())) {
                members.set(i, updated);
                saveAll(members);
                return true;
            }
        }
        return false;
    }

    /**
     * 주어진 아이디가 이미 사용 중인지 확인합니다. (회원가입 시 중복 확인용)
     */
    public boolean isIdDuplicate(String id) {
        for (Member m : loadAll()) {
            if (m.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 아이디/비밀번호가 일치하는 회원을 찾아 반환합니다.
     * 로그인 시 사용하며, 일치하는 회원이 없으면 null을 반환합니다.
     */
    public Member authenticate(String id, String password) {
        for (Member m : loadAll()) {
            if (m.getId().equals(id) && m.getPassword().equals(password)) {
                return m;
            }
        }
        return null;
    }
}
