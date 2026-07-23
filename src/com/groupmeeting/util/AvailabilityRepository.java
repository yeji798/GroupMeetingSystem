package com.groupmeeting.util;

import com.groupmeeting.model.AvailabilityEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final String HEADER = "roomCode,memberId,date,startTime,endTime,place";

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
                if (tokens.size() >= 6) {
                    entries.add(new AvailabilityEntry(
                            tokens.get(0), tokens.get(1), tokens.get(2),
                            tokens.get(3), tokens.get(4), tokens.get(5)
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
        for (AvailabilityEntry entry : entries) {
            sb.append(toLine(entry)).append(System.lineSeparator());
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
        return CsvUtil.toCsvLine(
                entry.getRoomCode(), entry.getMemberId(), entry.getDate(),
                entry.getStartTime(), entry.getEndTime(), entry.getPlace()
        );
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
     * 특정 방에서 회원별로 가장 최근에 제출한 장소를 반환합니다. (회원 아이디 -> 장소)
     * 장소가 비어있는 항목은 제외합니다.
     */
    public Map<String, String> getLatestPlaceByMember(String roomCode) {
        Map<String, String> result = new LinkedHashMap<>();
        for (AvailabilityEntry entry : getForRoom(roomCode)) {
            if (entry.getPlace() != null && !entry.getPlace().isBlank()) {
                result.put(entry.getMemberId(), entry.getPlace());
            }
        }
        return result;
    }
}
