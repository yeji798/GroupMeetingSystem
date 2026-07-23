/**
 * PlaceVoteRepository.java
 *
 * "장소 투표" 기능에서, 누가 어떤 장소에 투표했는지를 CSV 파일로 저장하고 불러오는 저장소 클래스입니다.
 *
 * 파일 경로: data/place_votes.csv
 * 컬럼 순서: roomCode, place, memberId
 *
 * 규칙: 한 사람은 한 방에서 동시에 한 곳에만 투표할 수 있습니다.
 *       -> castVote()를 호출하면, 그 사람의 기존 투표(있다면)를 먼저 지우고 새 투표를 추가합니다.
 *
 *   <중요 메소드>
 *   1. getMyVote(roomCode, memberId)   : 이 사람이 지금 투표한 장소 이름 (투표 안 했으면 null)
 *   2. castVote(roomCode, memberId, place) : 투표하기 (기존 투표는 자동으로 취소되고 새로 반영됨)
 *   3. cancelVote(roomCode, memberId)  : 투표 취소하기
 *   4. getVoteCounts(roomCode)         : 장소별 득표 수 (장소 이름 -> 득표 수)
 *   5. getVotersByMember(roomCode)     : 장소별로 투표한 사람들의 아이디 목록 (투표 명단 확인용)
 *   6. deleteVotesForPlace(...)        : 특정 장소가 삭제될 때, 그 장소에 대한 투표 기록도 함께 지움
 */

package com.groupmeeting.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlaceVoteRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "place_votes.csv";
    private static final String HEADER = "roomCode,place,memberId";

    public PlaceVoteRepository() {
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
            System.err.println("투표 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 한 줄(roomCode, place, memberId)을 표현하는 내부 전용 클래스입니다. */
    private static class VoteRow {
        final String roomCode;
        final String place;
        final String memberId;

        VoteRow(String roomCode, String place, String memberId) {
            this.roomCode = roomCode;
            this.place = place;
            this.memberId = memberId;
        }
    }

    /** CSV 파일에 저장된 모든 투표 기록을 읽어서 리스트로 반환합니다. */
    private List<VoteRow> loadAll() {
        List<VoteRow> rows = new ArrayList<>();
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
                    rows.add(new VoteRow(tokens.get(0), tokens.get(1), tokens.get(2)));
                }
            }
        } catch (IOException e) {
            System.err.println("투표 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return rows;
    }

    /** 전체 투표 기록을 CSV 파일에 덮어씁니다. */
    private void saveAll(List<VoteRow> rows) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (VoteRow row : rows) {
                writer.write(CsvUtil.toCsvLine(row.roomCode, row.place, row.memberId));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("투표 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 이 회원이 이 방에서 지금 투표한 장소 이름을 반환합니다. 투표한 적이 없으면 null을 반환합니다. */
    public String getMyVote(String roomCode, String memberId) {
        for (VoteRow row : loadAll()) {
            if (row.roomCode.equals(roomCode) && row.memberId.equals(memberId)) {
                return row.place;
            }
        }
        return null;
    }

    /**
     * 투표합니다. 한 사람이 동시에 여러 곳에 투표할 수 없으므로,
     * 이 회원이 이 방에서 예전에 투표한 기록이 있다면 먼저 지우고, 새로운 투표를 추가합니다.
     */
    public void castVote(String roomCode, String memberId, String place) {
        List<VoteRow> rows = loadAll();

        // 1) 이 회원의 이 방에 대한 기존 투표 기록을 제거한다.
        List<VoteRow> withoutMyOldVote = new ArrayList<>();
        for (VoteRow row : rows) {
            boolean isMyOldVote = row.roomCode.equals(roomCode) && row.memberId.equals(memberId);
            if (!isMyOldVote) {
                withoutMyOldVote.add(row);
            }
        }

        // 2) 새 투표를 추가한다.
        withoutMyOldVote.add(new VoteRow(roomCode, place, memberId));
        saveAll(withoutMyOldVote);
    }

    /** 이 회원이 이 방에 대해 가지고 있는 투표 기록을 지웁니다. (다시 투표할 수 있도록 "투표 취소") */
    public void cancelVote(String roomCode, String memberId) {
        List<VoteRow> rows = loadAll();
        List<VoteRow> kept = new ArrayList<>();
        for (VoteRow row : rows) {
            boolean isMyVote = row.roomCode.equals(roomCode) && row.memberId.equals(memberId);
            if (!isMyVote) {
                kept.add(row);
            }
        }
        saveAll(kept);
    }

    /** 이 방의 장소별 득표 수를 반환합니다. (장소 이름 -> 득표 수) */
    public Map<String, Integer> getVoteCounts(String roomCode) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (VoteRow row : loadAll()) {
            if (row.roomCode.equals(roomCode)) {
                counts.merge(row.place, 1, Integer::sum);
                // -> merge: place가 처음 나오면 1로 시작, 이미 있으면 기존 값 + 1
            }
        }
        return counts;
    }

    /** 이 방의 장소별로, 투표한 회원들의 아이디 목록을 반환합니다. (장소 이름 -> 투표자 아이디 목록) */
    public Map<String, List<String>> getVotersByPlace(String roomCode) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (VoteRow row : loadAll()) {
            if (row.roomCode.equals(roomCode)) {
                result.computeIfAbsent(row.place, k -> new ArrayList<>()).add(row.memberId);
            }
        }
        return result;
    }

    /** 특정 장소가 후보 목록에서 삭제될 때 함께 호출하여, 그 장소에 대한 투표 기록도 모두 지웁니다. */
    public void deleteVotesForPlace(String roomCode, String place) {
        List<VoteRow> rows = loadAll();
        List<VoteRow> kept = new ArrayList<>();
        for (VoteRow row : rows) {
            boolean isTargetPlace = row.roomCode.equals(roomCode) && row.place.equals(place);
            if (!isTargetPlace) {
                kept.add(row);
            }
        }
        saveAll(kept);
    }
}
