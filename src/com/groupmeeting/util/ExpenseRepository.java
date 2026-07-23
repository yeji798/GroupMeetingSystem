/**
 * ExpenseRepository.java
 *
 * 방의 지출(비용) 내역을 CSV 파일로 저장하고 불러오는 저장소 클래스입니다.
 * Room/Member와 동일한 방식으로, DB 없이 파일 입출력만으로 영속화합니다.
 *
 * 파일 경로: data/expenses.csv
 * 컬럼 순서: id, roomCode, payerId, amount, reason, note, roundId
 *
 *   <중요 메소드>
 *   1. getForRoom(roomCode)   : 이 방에 등록된 지출 내역 전체를 가져옴
 *   2. addExpense(expense)    : 새 지출 내역 추가 ("추가" 버튼)
 *   3. updateExpense(expense) : 기존 지출 내역 수정 ("수정" 버튼, id가 같은 항목을 찾아서 덮어씀)
 *   4. deleteExpense(...)     : 지출 내역 삭제 ("삭제" 버튼)
 */

package com.groupmeeting.util;

import com.groupmeeting.model.Expense;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "expenses.csv";
    private static final String HEADER = "id,roomCode,payerId,amount,reason,note,roundId";

    public ExpenseRepository() {
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
            System.err.println("지출 내역 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 지출 내역을 읽어서 리스트로 반환합니다. */
    public List<Expense> loadAll() {
        List<Expense> expenses = new ArrayList<>();
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
                    String id = tokens.get(0);
                    String roomCode = tokens.get(1);
                    String payerId = tokens.get(2);
                    long amount = Long.parseLong(tokens.get(3));
                    String reason = tokens.get(4);
                    String note = tokens.get(5);
                    // roundId 칸은 나중에 추가된 컬럼이라, 예전 방식으로 저장된 줄(6개 칸)에는
                    // 없을 수도 있습니다. 없으면 "일반 지출"이라는 뜻으로 빈 문자열을 사용합니다.
                    String roundId = tokens.size() >= 7 ? tokens.get(6) : "";
                    expenses.add(new Expense(id, roomCode, payerId, amount, reason, note, roundId));
                }
            }
        } catch (IOException e) {
            System.err.println("지출 내역을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return expenses;
    }

    /** 전체 지출 내역을 CSV 파일에 덮어씁니다. */
    private void saveAll(List<Expense> expenses) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Expense expense : expenses) {
                writer.write(toLine(expense));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("지출 내역을 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String toLine(Expense expense) {
        return CsvUtil.toCsvLine(
                expense.getId(), expense.getRoomCode(), expense.getPayerId(),
                String.valueOf(expense.getAmount()), expense.getReason(), expense.getNote(), expense.getRoundId()
        );
    }

    /** 특정 방에 등록된 지출 내역만 골라서 반환합니다. */
    public List<Expense> getForRoom(String roomCode) {
        List<Expense> result = new ArrayList<>();
        for (Expense expense : loadAll()) {
            if (expense.getRoomCode().equals(roomCode)) {
                result.add(expense);
            }
        }
        return result;
    }

    /** 새 지출 내역 하나를 추가합니다. */
    public void addExpense(Expense expense) {
        List<Expense> all = loadAll();
        all.add(expense);
        saveAll(all);
    }

    /** 기존 지출 내역을 수정합니다. id가 같은 항목을 찾아서 통째로 교체합니다. */
    public void updateExpense(Expense updated) {
        List<Expense> all = loadAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(updated.getId())) {
                all.set(i, updated);
                break;
            }
        }
        saveAll(all);
    }

    /** 지출 내역을 하나 삭제합니다. */
    public void deleteExpense(String expenseId) {
        List<Expense> all = loadAll();
        List<Expense> kept = new ArrayList<>();
        for (Expense expense : all) {
            if (!expense.getId().equals(expenseId)) {
                kept.add(expense);
            }
        }
        saveAll(kept);
    }
}
