/**
 * PlaceRepository.java
 *
 * 방마다 "모임 장소 후보" 목록을 CSV 파일로 저장하고 불러오는 저장소 클래스입니다.
 * (RoomRepository, MemberRepository와 완전히 같은 방식으로, DB 없이 파일 입출력만으로 저장합니다.)
 *
 * 파일 경로: data/places.csv
 * 컬럼 순서: roomCode, place
 *
 *   <중요 메소드>
 *   1. getPlaces(roomCode)          : 이 방에 등록된 장소 이름 목록을 가져옴
 *   2. addPlace(roomCode, place)    : 새 장소를 추가함 (이미 있는 이름이면 추가하지 않고 false 반환)
 *   3. deletePlace(roomCode, place) : 특정 장소를 목록에서 제거함
 *   4. seedIfEmpty(...)             : 이 방에 등록된 장소가 하나도 없을 때만, 넘겨받은 목록으로 초기 채움
 *                                     (예전에 "날짜/시간 입력" 화면에서 참여자들이 적어둔 장소 추천을
 *                                      이 새로운 "장소 후보 목록" 기능으로 자연스럽게 이어주기 위해 사용)
 */

package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlaceRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "places.csv";
    private static final String HEADER = "roomCode,place";

    public PlaceRepository() {
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
            System.err.println("장소 목록 파일 초기화 실패: " + e.getMessage());
        }
    }

    /**
     * CSV 한 줄(roomCode, place)을 표현하는 아주 작은 내부용 클래스입니다.
     * 이 클래스는 이 파일 밖에서는 쓰이지 않고, loadAll()/saveAll() 안에서만 사용됩니다.
     */
    private static class PlaceRow {
        final String roomCode;
        final String place;

        PlaceRow(String roomCode, String place) {
            this.roomCode = roomCode;
            this.place = place;
        }
    }

    /** CSV 파일에 저장된 모든 (방, 장소) 쌍을 읽어서 리스트로 반환합니다. */
    private List<PlaceRow> loadAll() {
        List<PlaceRow> rows = new ArrayList<>();
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
                if (tokens.size() >= 2) {
                    rows.add(new PlaceRow(tokens.get(0), tokens.get(1)));
                }
            }
        } catch (IOException e) {
            System.err.println("장소 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return rows;
    }

    /** 전체 목록을 CSV 파일에 덮어씁니다. */
    private void saveAll(List<PlaceRow> rows) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (PlaceRow row : rows) {
                writer.write(CsvUtil.toCsvLine(row.roomCode, row.place));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("장소 목록을 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 특정 방에 등록된 장소 이름 목록을 등록된 순서 그대로 반환합니다. */
    public List<String> getPlaces(String roomCode) {
        List<String> result = new ArrayList<>();
        for (PlaceRow row : loadAll()) {
            if (row.roomCode.equals(roomCode)) {
                result.add(row.place);
            }
        }
        return result;
    }

    /**
     * 새 장소를 이 방의 후보 목록에 추가합니다.
     * -> 이름이 비어있거나, 이미 등록된 이름(대소문자 구분 없이 동일)과 겹치면 추가하지 않고 false를 반환합니다.
     *    (요구사항: "같은 이름의 장소가 중복 입력되지 않도록 한다")
     *
     * @return 추가에 성공하면 true, 이름이 비었거나 중복이라 추가하지 못했으면 false
     */
    public boolean addPlace(String roomCode, String place) {
        if (place == null) {
            return false;
        }
        String trimmed = place.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // 이미 같은 이름(대소문자 무시)의 장소가 있는지 확인
        for (String existing : getPlaces(roomCode)) {
            if (existing.equalsIgnoreCase(trimmed)) {
                return false; // 중복이므로 추가하지 않음
            }
        }

        List<PlaceRow> rows = loadAll();
        rows.add(new PlaceRow(roomCode, trimmed));
        saveAll(rows);
        return true;
    }

    /** 특정 방에서 주어진 이름과 일치하는 장소를 목록에서 제거합니다. */
    public void deletePlace(String roomCode, String place) {
        List<PlaceRow> rows = loadAll();
        List<PlaceRow> kept = new ArrayList<>();
        for (PlaceRow row : rows) {
            boolean isTargetRow = row.roomCode.equals(roomCode) && row.place.equals(place);
            if (!isTargetRow) {
                kept.add(row);
            }
        }
        saveAll(kept);
    }

    /**
     * 이 방에 등록된 장소가 아직 하나도 없을 때만, 넘겨받은 initialPlaces 목록으로 미리 채워 넣습니다.
     * (이미 장소가 하나라도 있다면 아무 일도 하지 않습니다 - 사용자가 직접 정리한 목록을 덮어쓰지 않기 위함)
     */
    public void seedIfEmpty(String roomCode, Collection<String> initialPlaces) {
        if (!getPlaces(roomCode).isEmpty()) {
            return; // 이미 목록이 있으므로 건드리지 않음
        }
        for (String place : initialPlaces) {
            addPlace(roomCode, place); // addPlace가 중복/빈값 검사를 알아서 해줌
        }
    }
}
