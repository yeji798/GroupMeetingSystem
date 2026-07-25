/**
 * RoomFinalRepository.java
 *
 * 방장이 최종 확정한 "모임 최종 날짜/시간/장소"를 CSV 파일로 저장하고 불러오는 저장소 클래스입니다.
 * 방 하나당 한 줄만 존재하며, 날짜/시간/장소는 각각 따로따로 확정하거나 수정할 수 있습니다.
 *
 * 파일 경로: data/room_final.csv
 * 컬럼 순서: roomCode, finalDate, finalStartTime, finalEndTime, finalPlace
 *
 *   <중요 메소드>
 *   1. getForRoom(roomCode)   : 이 방의 최종 확정 정보를 가져옴 (없으면 빈 값으로 채워진 객체 반환)
 *   2. setFinalDate(...)      : 최종 날짜만 갱신 (다른 항목은 그대로 유지)
 *   3. setFinalTime(...)      : 최종 시작~끝 시간만 갱신
 *   4. setFinalPlace(...)     : 최종 장소만 갱신
 */

package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import com.groupmeeting.model.RoomFinalDecision;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class RoomFinalRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "room_final.csv";
    private static final String HEADER = "roomCode,finalDate,finalStartTime,finalEndTime,finalPlace";

    public RoomFinalRepository() {
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
            System.err.println("모임 최종 확정 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 방의 최종 확정 정보를 읽어서 리스트로 반환합니다. */
    private List<RoomFinalDecision> loadAll() {
        List<RoomFinalDecision> list = new ArrayList<>();
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                List<String> tokens = CsvUtil.parseLine(line);
                if (tokens.size() >= 5) {
                    list.add(new RoomFinalDecision(tokens.get(0), tokens.get(1), tokens.get(2), tokens.get(3), tokens.get(4)));
                }
            }
        } catch (IOException e) {
            System.err.println("모임 최종 확정 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return list;
    }

    /** 전체 목록을 CSV 파일에 덮어씁니다. */
    private void saveAll(List<RoomFinalDecision> list) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (RoomFinalDecision d : list) {
                writer.write(CsvUtil.toCsvLine(
                        d.getRoomCode(), d.getFinalDate(), d.getFinalStartTime(), d.getFinalEndTime(), d.getFinalPlace()
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("모임 최종 확정 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 이 방의 최종 확정 정보를 반환합니다. 아직 저장된 것이 없다면 빈 값으로 채워진 객체를 반환합니다. */
    public RoomFinalDecision getForRoom(String roomCode) {
        for (RoomFinalDecision d : loadAll()) {
            if (d.getRoomCode().equals(roomCode)) {
                return d;
            }
        }
        return RoomFinalDecision.empty(roomCode);
    }

    /** 이 방의 최종 확정 정보를 찾아서 수정하고 저장합니다. 없으면 새로 만들어서 저장합니다. */
    private void updateOrCreate(String roomCode, java.util.function.Consumer<RoomFinalDecision> updater) {
        List<RoomFinalDecision> all = loadAll();

        RoomFinalDecision target = null;
        for (RoomFinalDecision d : all) {
            if (d.getRoomCode().equals(roomCode)) {
                target = d;
                break;
            }
        }

        if (target == null) {
            target = RoomFinalDecision.empty(roomCode);
            all.add(target);
        }

        updater.accept(target);
        saveAll(all);
    }

    /** 최종 날짜만 갱신합니다. (단체 약속: 특정 하루 / 단체 여행: "M월 d일 ~ M월 d일" 같은 구간 문자열) */
    public void setFinalDate(String roomCode, String finalDate) {
        updateOrCreate(roomCode, d -> d.setFinalDate(finalDate));
    }

    /** 최종 시작~끝 시간을 갱신합니다. (단체 약속 전용) */
    public void setFinalTime(String roomCode, String startTime, String endTime) {
        updateOrCreate(roomCode, d -> {
            d.setFinalStartTime(startTime);
            d.setFinalEndTime(endTime);
        });
    }

    /** 최종 장소만 갱신합니다. */
    public void setFinalPlace(String roomCode, String finalPlace) {
        updateOrCreate(roomCode, d -> d.setFinalPlace(finalPlace));
    }
}
