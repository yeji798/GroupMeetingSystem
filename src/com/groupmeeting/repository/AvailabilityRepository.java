package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import com.groupmeeting.model.AvailabilityEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * "단체 약속" 방의 참여자별 가능 날짜/시간/장소 정보를 CSV 파일로 저장하고 불러오는
 * 저장소(Repository) 클래스입니다. Room/Member와 동일한 방식으로 DB 없이 파일 입출력만으로 영속화합니다.
 *
 * 파일 경로: data/availability.csv
 * 컬럼 순서: roomCode, memberId, date, startTime, endTime, place
 */
public class AvailabilityRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "availability.csv";
    private static final String HEADER = "roomCode,memberId,date,startTime,endTime";

    public AvailabilityRepository() {
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
            System.err.println("가능 시간 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 가능 시간 정보를 읽어서 리스트로 반환합니다. */
    public List<AvailabilityEntry> loadAll() {
        List<AvailabilityEntry> entries = new ArrayList<>();
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
                // 이전 6열 형식의 place 값은 무시하여 기존 파일도 읽을 수 있습니다.
                if (tokens.size() >= 5) {
                    entries.add(new AvailabilityEntry(
                            tokens.get(0), tokens.get(1), tokens.get(2),
                            tokens.get(3), tokens.get(4)
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("가능 시간 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return entries;
    }

    /** 전체 목록을 CSV 파일에 덮어씁니다. */
    public void saveAll(List<AvailabilityEntry> entries) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (AvailabilityEntry entry : entries) {
                writer.write(toLine(entry));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("가능 시간 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 새로운 가능 시간 항목들을 CSV 파일 맨 끝에 한 번에 추가합니다.
     * (ScheduleInputDialog의 "제출" 버튼에서 사용)
     */
    public boolean appendEntries(List<AvailabilityEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return true;
        }

        Path filePath = Paths.get(FILE_PATH);
        StringBuilder sb = new StringBuilder();
        boolean legacySchema = usesLegacySchema(filePath);
        for (AvailabilityEntry entry : entries) {
            sb.append(toLine(entry, legacySchema)).append(System.lineSeparator());
        }

        try {
            Files.writeString(
                    filePath, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            System.err.println("가능 시간 정보 추가 중 오류가 발생했습니다: " + e.getMessage());
            return false;
        }
    }

    private String toLine(AvailabilityEntry entry) {
        return toLine(entry, false);
    }

    private String toLine(AvailabilityEntry entry, boolean legacySchema) {
        if (legacySchema) {
            return CsvUtil.toCsvLine(
                    entry.getRoomCode(), entry.getMemberId(), entry.getDate(),
                    entry.getStartTime(), entry.getEndTime(), ""
            );
        }
        return CsvUtil.toCsvLine(
                entry.getRoomCode(), entry.getMemberId(), entry.getDate(),
                entry.getStartTime(), entry.getEndTime()
        );
    }

    private boolean usesLegacySchema(Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            return header != null && CsvUtil.parseLine(header).size() >= 6;
        } catch (IOException e) {
            return false;
        }
    }

    /** 특정 방에 제출된 모든 가능 시간 항목을 반환합니다. */
    public List<AvailabilityEntry> getForRoom(String roomCode) {
        List<AvailabilityEntry> result = new ArrayList<>();
        for (AvailabilityEntry entry : loadAll()) {
            if (entry.getRoomCode().equals(roomCode)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 이전 6열 CSV에 남아 있는 장소 값을 마이그레이션할 때만 사용합니다.
     * 신규 장소는 PlaceRepository에 직접 저장됩니다.
     */
    public List<String> getLegacyPlaceSuggestions(String roomCode) {
        List<String> places = new ArrayList<>();
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> tokens = CsvUtil.parseLine(line);
                if (tokens.size() >= 6
                        && tokens.get(0).equals(roomCode)
                        && !tokens.get(5).isBlank()) {
                    places.add(tokens.get(5));
                }
            }
        } catch (IOException e) {
            System.err.println("이전 장소 데이터를 읽지 못했습니다: " + e.getMessage());
        }
        return places;
    }

    /**
     * 특정 방에서 회원별로 가장 최근에 제출한 장소를 반환합니다. (회원 아이디 -> 장소)
     * 장소가 비어있는 항목은 제외합니다.
     */
    /**
     * 특정 방(roomCode)에서, 특정 회원(memberId) 한 명이 지금까지 제출한 가능 시간 항목만 골라서 반환합니다.
     * -> "자신의 일정 수정" 화면(MyScheduleEditDialog)을 열 때, 예전에 저장해둔 내용을 화면에 미리 채워 넣는 용도로 사용합니다.
     */
    public List<AvailabilityEntry> getForRoomAndMember(String roomCode, String memberId) {
        List<AvailabilityEntry> result = new ArrayList<>();
        for (AvailabilityEntry entry : getForRoom(roomCode)) {
            if (entry.getMemberId().equals(memberId)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 특정 회원(memberId)이 특정 방(roomCode)에 대해 예전에 저장해두었던 항목들을 전부 지우고,
     * 새로 넘겨받은 newEntries 목록으로 통째로 교체합니다. ("자신의 일정 수정" 화면의 최종 "저장" 버튼에서 사용)
     *
     * -> appendEntries()는 무조건 뒤에 이어붙이기만 하지만(추가), 이 메서드는 "그 사람의 예전 기록은
     *    지우고 지금 화면에 있는 목록으로 덮어쓰기" 하는 것이 다른 점입니다.
     */
    public boolean replaceForMember(String roomCode, String memberId, List<AvailabilityEntry> newEntries) {
        List<AvailabilityEntry> all = loadAll(); // 1) CSV에 저장된 전체 데이터를 불러온다.

        // 2) 전체 데이터 중에서, "이 방 + 이 회원"에 해당하지 않는 것들만 남긴다.
        //    (즉, 이 회원의 예전 기록과 다른 방/다른 회원의 기록을 구분해서, 다른 회원 기록은 그대로 보존한다.)
        List<AvailabilityEntry> kept = new ArrayList<>();
        for (AvailabilityEntry entry : all) {
            boolean isThisMemberAndRoom = entry.getRoomCode().equals(roomCode) && entry.getMemberId().equals(memberId);
            if (!isThisMemberAndRoom) {
                kept.add(entry);
            }
        }

        // 3) 남겨둔 다른 기록들 뒤에, 화면에서 새로 정리한 목록을 이어 붙인다.
        kept.addAll(newEntries);

        // 4) 파일 전체를 새 목록으로 덮어쓴다.
        saveAll(kept);
        return true;
    }
}
