/**
 * RoundParticipantRepository.java
 *
 * "차수별 인원 조사" 기능에서, 누가 어떤 모임 차수에 "참여"를 확정했는지를 CSV 파일로
 * 저장하고 불러오는 저장소 클래스입니다.
 *
 * 파일 경로: data/round_participants.csv
 * 컬럼 순서: roomCode, roundId, memberId
 *
 * 규칙: "참여" 버튼은 한 번 누르면 다시 되돌릴 수 없습니다(요구사항). 그래서 이 저장소에는
 *       "참여를 취소"하는 기능이 없고, 오직 추가(참여 확정)만 있습니다.
 *
 *   <중요 메소드>
 *   1. getParticipantIds(roomCode, roundId) : 이 차수에 참여를 확정한 회원 아이디 목록
 *   2. isParticipating(...)                  : 특정 회원이 이 차수에 참여 확정했는지 여부
 *   3. addParticipant(...)                    : "참여" 버튼 -> 참여자로 등록 (이미 등록되어 있으면 무시)
 */

package com.groupmeeting.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class RoundParticipantRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "round_participants.csv";
    private static final String HEADER = "roomCode,roundId,memberId";

    public RoundParticipantRepository() {
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
            System.err.println("차수 참여자 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 한 줄(roomCode, roundId, memberId)을 표현하는 내부 전용 클래스입니다. */
    private static class ParticipantRow {
        final String roomCode;
        final String roundId;
        final String memberId;

        ParticipantRow(String roomCode, String roundId, String memberId) {
            this.roomCode = roomCode;
            this.roundId = roundId;
            this.memberId = memberId;
        }
    }

    /** CSV 파일에 저장된 모든 참여 기록을 읽어서 리스트로 반환합니다. */
    private List<ParticipantRow> loadAll() {
        List<ParticipantRow> rows = new ArrayList<>();
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
                    rows.add(new ParticipantRow(tokens.get(0), tokens.get(1), tokens.get(2)));
                }
            }
        } catch (IOException e) {
            System.err.println("차수 참여자 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return rows;
    }

    /** 특정 차수(roundId)에 참여를 확정한 회원들의 아이디 목록을 반환합니다. */
    public List<String> getParticipantIds(String roundId) {
        List<String> result = new ArrayList<>();
        for (ParticipantRow row : loadAll()) {
            if (row.roundId.equals(roundId)) {
                result.add(row.memberId);
            }
        }
        return result;
    }

    /** 특정 회원이 특정 차수에 참여를 이미 확정했는지 확인합니다. */
    public boolean isParticipating(String roundId, String memberId) {
        return getParticipantIds(roundId).contains(memberId);
    }

    /**
     * 이 회원을 해당 차수의 참여자로 등록합니다. (CSV 맨 끝에 한 줄 추가)
     * 이미 참여 등록이 되어 있다면 중복으로 추가하지 않습니다.
     */
    public void addParticipant(String roomCode, String roundId, String memberId) {
        if (isParticipating(roundId, memberId)) {
            return; // 이미 참여 확정된 경우 아무 것도 하지 않음
        }

        Path filePath = Paths.get(FILE_PATH);
        String line = CsvUtil.toCsvLine(roomCode, roundId, memberId);

        try {
            Files.writeString(
                    filePath, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("차수 참여자 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
