/**
 * SettlementRepository.java
 *
 * "정산" 버튼을 눌러서 계산된 결과("누가 누구에게 얼마를 줘야 한다")를 CSV 파일로
 * 저장하고 불러오는 저장소 클래스입니다.
 *
 * 파일 경로: data/settlements.csv
 * 컬럼 순서: roomCode, fromMemberId, toMemberId, amount, confirmed
 *
 * 이 프로젝트에서는 방마다 "가장 최근에 계산한 정산 결과" 한 세트만 보관합니다.
 * -> 정산 버튼을 다시 누르면(예: 다른 지출 내역을 선택해서), 그 방의 예전 정산 결과는 전부 지워지고
 *    새로 계산한 결과로 통째로 교체됩니다. (replaceForRoom 메소드)
 *
 *   <중요 메소드>
 *   1. getForRoom(roomCode)                         : 이 방의 최신 정산 결과 전체를 가져옴
 *   2. replaceForRoom(roomCode, items)               : 정산을 새로 계산해서 통째로 교체
 *   3. setConfirmed(roomCode, from, to, confirmed)   : 특정 항목의 "입금 확인" 체크 상태를 저장
 */

package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import com.groupmeeting.model.SettlementItem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class SettlementRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "settlements.csv";
    private static final String HEADER = "roomCode,fromMemberId,toMemberId,amount,confirmed";

    public SettlementRepository() {
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
            System.err.println("정산 결과 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 정산 결과를 읽어서 리스트로 반환합니다. */
    public List<SettlementItem> loadAll() {
        List<SettlementItem> items = new ArrayList<>();
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
                    String roomCode = tokens.get(0);
                    String from = tokens.get(1);
                    String to = tokens.get(2);
                    long amount = Long.parseLong(tokens.get(3));
                    boolean confirmed = Boolean.parseBoolean(tokens.get(4));
                    items.add(new SettlementItem(roomCode, from, to, amount, confirmed));
                }
            }
        } catch (IOException e) {
            System.err.println("정산 결과를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return items;
    }

    /** 전체 정산 결과를 CSV 파일에 덮어씁니다. */
    private void saveAll(List<SettlementItem> items) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (SettlementItem item : items) {
                writer.write(CsvUtil.toCsvLine(
                        item.getRoomCode(), item.getFromMemberId(), item.getToMemberId(),
                        String.valueOf(item.getAmount()), String.valueOf(item.isConfirmed())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("정산 결과를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 특정 방의 정산 결과만 골라서 반환합니다. */
    public List<SettlementItem> getForRoom(String roomCode) {
        List<SettlementItem> result = new ArrayList<>();
        for (SettlementItem item : loadAll()) {
            if (item.getRoomCode().equals(roomCode)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 이 방의 예전 정산 결과를 모두 지우고, 새로 계산한 결과(newItems)로 통째로 교체합니다.
     * ("정산" 버튼을 누를 때마다 새로 계산해서 이전 결과를 덮어씀 - 확인 체크 상태도 전부 초기화됨)
     */
    public void replaceForRoom(String roomCode, List<SettlementItem> newItems) {
        List<SettlementItem> all = loadAll();

        List<SettlementItem> kept = new ArrayList<>();
        for (SettlementItem item : all) {
            if (!item.getRoomCode().equals(roomCode)) {
                kept.add(item);
            }
        }

        kept.addAll(newItems);
        saveAll(kept);
    }

    /**
     * 특정 정산 항목(누가 -> 누구에게)의 "입금 확인" 체크 상태를 바꿉니다.
     * (한 쌍(from, to)은 한 정산 라운드에 한 번만 계산되도록 SettlementCalculator가 보장하므로,
     *  roomCode + from + to 조합으로 항목을 정확히 하나 찾을 수 있습니다.)
     */
    public void setConfirmed(String roomCode, String fromMemberId, String toMemberId, boolean confirmed) {
        List<SettlementItem> all = loadAll();
        for (SettlementItem item : all) {
            boolean isTarget = item.getRoomCode().equals(roomCode)
                    && item.getFromMemberId().equals(fromMemberId)
                    && item.getToMemberId().equals(toMemberId);
            if (isTarget) {
                item.setConfirmed(confirmed);
                break;
            }
        }
        saveAll(all);
    }
}
