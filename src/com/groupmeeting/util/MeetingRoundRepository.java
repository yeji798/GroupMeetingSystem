/**
 * MeetingRoundRepository.java
 *
 * "차수별 인원 조사" 기능에서 사용하는 모임 차수 목록(예: "1차 모임", "2차 모임", "뒤풀이")을
 * CSV 파일로 저장하고 불러오는 저장소 클래스입니다.
 *
 * 파일 경로: data/meeting_rounds.csv
 * 컬럼 순서: roomCode, id, name
 *
 *   <중요 메소드>
 *   1. getForRoom(roomCode)      : 이 방에 만들어진 모임 차수 목록을 가져옴
 *   2. addRound(roomCode, name)  : 새 모임 차수를 추가함 ("모임 차수 추가" 버튼에서 사용)
 */

package com.groupmeeting.util;

import com.groupmeeting.model.MeetingRound;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MeetingRoundRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "meeting_rounds.csv";
    private static final String HEADER = "roomCode,id,name";

    public MeetingRoundRepository() {
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
            System.err.println("모임 차수 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 모임 차수를 읽어서 리스트로 반환합니다. */
    public List<MeetingRound> loadAll() {
        List<MeetingRound> rounds = new ArrayList<>();
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
                if (tokens.size() >= 3) {
                    rounds.add(new MeetingRound(tokens.get(0), tokens.get(1), tokens.get(2)));
                }
            }
        } catch (IOException e) {
            System.err.println("모임 차수 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return rounds;
    }

    /** 특정 방에 만들어진 모임 차수 목록만 골라서 반환합니다. */
    public List<MeetingRound> getForRoom(String roomCode) {
        List<MeetingRound> result = new ArrayList<>();
        for (MeetingRound round : loadAll()) {
            if (round.getRoomCode().equals(roomCode)) {
                result.add(round);
            }
        }
        return result;
    }

    /**
     * 새로운 모임 차수를 이 방에 추가합니다. (고유 아이디는 자동으로 생성됩니다)
     * -> "모임 차수 추가" 버튼에서 사용하며, 추가되는 즉시 모든 참여자의 차수 목록에 나타납니다.
     */
    public void addRound(String roomCode, String name) {
        Path filePath = Paths.get(FILE_PATH);
        String id = UUID.randomUUID().toString();
        String line = CsvUtil.toCsvLine(roomCode, id, name);

        try {
            Files.writeString(
                    filePath, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("모임 차수 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
