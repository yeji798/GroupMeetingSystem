package com.groupmeeting.util;

import com.groupmeeting.model.Room;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 모임 방(Room) 정보를 CSV 파일로 저장하고 불러오는 저장소(Repository) 클래스입니다.
 * 회원(MemberRepository)과 동일한 방식으로, DB 없이 파일 입출력만으로 데이터를 영속화합니다.
 *
 * 파일 경로: data/rooms.csv
 * 컬럼 순서: code, name, category, ownerId, members(세미콜론(;)으로 구분)
 */
public class RoomRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "rooms.csv";
    private static final String HEADER = "code,name,category,ownerId,members";
    private static final String MEMBER_DELIMITER = ";";

    private final Random random = new Random();

    public RoomRepository() {
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
            System.err.println("모임 방 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 모임 방 정보를 읽어서 리스트로 반환합니다. */
    public List<Room> loadAll() {
        List<Room> rooms = new ArrayList<>();
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 라인 건너뛰기
                }
                if (line.isBlank()) {
                    continue;
                }

                List<String> tokens = CsvUtil.parseLine(line);
                if (tokens.size() >= 5) {
                    String code = tokens.get(0);
                    String name = tokens.get(1);
                    String category = tokens.get(2);
                    String ownerId = tokens.get(3);
                    String membersRaw = tokens.get(4);

                    List<String> memberIds = new ArrayList<>();
                    if (!membersRaw.isBlank()) {
                        for (String id : membersRaw.split(MEMBER_DELIMITER)) {
                            if (!id.isBlank()) {
                                memberIds.add(id.trim());
                            }
                        }
                    }

                    rooms.add(new Room(code, name, category, ownerId, memberIds));
                }
            }
        } catch (IOException e) {
            System.err.println("모임 방 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return rooms;
    }

    /** 방 목록 전체를 CSV 파일에 덮어씁니다. (참여자 변경, 정보 수정 등 전체가 바뀌는 경우 사용) */
    public void saveAll(List<Room> rooms) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Room r : rooms) {
                String membersJoined = String.join(MEMBER_DELIMITER, r.getMemberIds());
                String line = CsvUtil.toCsvLine(
                        r.getCode(), r.getName(), r.getCategory(), r.getOwnerId(), membersJoined
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("모임 방 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 새로운 방 하나를 생성하여 CSV 파일에 저장합니다. */
    public boolean addRoom(Room room) {
        List<Room> rooms = loadAll();
        rooms.add(room);
        saveAll(rooms);
        return true;
    }

    /** 주어진 방 코드가 이미 사용 중인지 확인합니다. (방 생성 시 코드 중복 확인용) */
    public boolean isCodeDuplicate(String code) {
        return findByCode(code) != null;
    }

    /** 방 코드로 방을 조회합니다. 없으면 null을 반환합니다. */
    public Room findByCode(String code) {
        for (Room r : loadAll()) {
            if (r.getCode().equals(code)) {
                return r;
            }
        }
        return null;
    }

    /** 특정 회원이 참여 중인 모든 방 목록을 반환합니다. (메인 화면의 "방 리스트"에 사용) */
    public List<Room> getRoomsForMember(String memberId) {
        List<Room> result = new ArrayList<>();
        for (Room r : loadAll()) {
            if (r.hasMember(memberId)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * 특정 회원을 주어진 코드의 방에 참여시킵니다.
     *
     * @return 성공 시 참여된 Room 객체, 실패 시 null
     *         (실패 사유는 room이 없거나, 이미 참여 중이거나, 정원이 가득 찬 경우)
     */
    public Room joinRoom(String code, String memberId) {
        List<Room> rooms = loadAll();
        Room target = null;

        for (Room r : rooms) {
            if (r.getCode().equals(code)) {
                target = r;
                break;
            }
        }

        if (target == null) {
            return null; // 존재하지 않는 방
        }
        if (target.hasMember(memberId)) {
            return target; // 이미 참여 중인 경우 그대로 반환
        }
        if (target.isFull()) {
            return null; // 정원 초과
        }

        target.getMemberIds().add(memberId);
        saveAll(rooms);
        return target;
    }

    /** 특정 회원을 해당 코드의 방에서 내보냅니다(방 나가기). */
    public void leaveRoom(String code, String memberId) {
        List<Room> rooms = loadAll();
        for (Room r : rooms) {
            if (r.getCode().equals(code)) {
                r.getMemberIds().remove(memberId);
                break;
            }
        }
        saveAll(rooms);
    }

    /**
     * 1000~9999 사이의 중복되지 않는 4자리 방 코드를 무작위로 생성합니다.
     * 방 생성 다이얼로그를 열 때 기본값으로 제안하는 용도로 사용합니다.
     */
    public String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            int number = 1000 + random.nextInt(9000); // 1000 ~ 9999
            code = String.valueOf(number);
            attempts++;
            // 무한 루프 방지를 위한 안전장치 (방이 아주 많아질 경우 대비)
            if (attempts > 200) {
                break;
            }
        } while (isCodeDuplicate(code));

        return code;
    }
}
