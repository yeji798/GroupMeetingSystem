/**
 * TravelDateRepository.java
 *
 * "단체 여행" 방에서 참여자가 제안한 여행 날짜(시작일~종료일)들을 CSV 파일로 저장하고
 * 불러오는 저장소 클래스입니다. 한 사람이 여러 개의 날짜 구간을 제안할 수 있도록
 * (예: "7/1~7/3"과 "7/10~7/12" 둘 다) AvailabilityRepository와 같은 방식으로 만들었습니다.
 *
 * 파일 경로: data/travel_dates.csv
 * 컬럼 순서: roomCode, memberId, startDate, endDate
 *
 *   <중요 메소드>
 *   1. getForRoom(roomCode)                       : 이 방에 제출된 모든 여행 날짜를 가져옴
 *   2. getForRoomAndMember(roomCode, memberId)     : 특정 회원이 제출한 여행 날짜만 가져옴
 *   3. appendEntries(entries)                       : 새 날짜 구간들을 CSV 맨 끝에 추가 (처음 제출할 때 사용)
 *   4. replaceForMember(roomCode, memberId, ...)    : 특정 회원의 예전 기록을 지우고 새 목록으로 교체
 *                                                      ("자신의 일정 수정" 화면의 최종 저장에서 사용)
 */

package com.groupmeeting.repository;

import com.groupmeeting.util.CsvUtil;

import com.groupmeeting.model.TravelDateEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TravelDateRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + File.separator + "travel_dates.csv";
    private static final String HEADER = "roomCode,memberId,startDate,endDate";

    public TravelDateRepository() {
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
            System.err.println("여행 날짜 파일 초기화 실패: " + e.getMessage());
        }
    }

    /** CSV 파일에 저장된 모든 여행 날짜 항목을 읽어서 리스트로 반환합니다. */
    public List<TravelDateEntry> loadAll() {
        List<TravelDateEntry> entries = new ArrayList<>();
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
                if (tokens.size() >= 4) {
                    entries.add(new TravelDateEntry(tokens.get(0), tokens.get(1), tokens.get(2), tokens.get(3)));
                }
            }
        } catch (IOException e) {
            System.err.println("여행 날짜 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }

        return entries;
    }

    /** 전체 목록을 CSV 파일에 덮어씁니다. */
    private void saveAll(List<TravelDateEntry> entries) {
        Path filePath = Paths.get(FILE_PATH);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (TravelDateEntry entry : entries) {
                writer.write(CsvUtil.toCsvLine(
                        entry.getRoomCode(), entry.getMemberId(), entry.getStartDate(), entry.getEndDate()
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("여행 날짜 정보를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 특정 방에 제출된 모든 여행 날짜 항목을 반환합니다. */
    public List<TravelDateEntry> getForRoom(String roomCode) {
        List<TravelDateEntry> result = new ArrayList<>();
        for (TravelDateEntry entry : loadAll()) {
            if (entry.getRoomCode().equals(roomCode)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** 특정 방(roomCode)에서, 특정 회원(memberId) 한 명이 제출한 여행 날짜 항목만 골라 반환합니다. */
    public List<TravelDateEntry> getForRoomAndMember(String roomCode, String memberId) {
        List<TravelDateEntry> result = new ArrayList<>();
        for (TravelDateEntry entry : getForRoom(roomCode)) {
            if (entry.getMemberId().equals(memberId)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 새로운 여행 날짜 항목들을 CSV 파일 맨 끝에 한 번에 추가합니다.
     * (TravelDateInputDialog의 "제출" 버튼에서 사용)
     */
    public boolean appendEntries(List<TravelDateEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return true;
        }

        Path filePath = Paths.get(FILE_PATH);
        StringBuilder sb = new StringBuilder();
        for (TravelDateEntry entry : entries) {
            sb.append(CsvUtil.toCsvLine(
                    entry.getRoomCode(), entry.getMemberId(), entry.getStartDate(), entry.getEndDate()
            )).append(System.lineSeparator());
        }

        try {
            Files.writeString(
                    filePath, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            System.err.println("여행 날짜 정보 추가 중 오류가 발생했습니다: " + e.getMessage());
            return false;
        }
    }

    /**
     * 특정 회원(memberId)이 특정 방(roomCode)에 대해 예전에 저장해두었던 여행 날짜 항목들을 전부
     * 지우고, 새로 넘겨받은 newEntries 목록으로 통째로 교체합니다.
     * ("자신의 일정 수정" 화면의 최종 "저장" 버튼에서 사용 - AvailabilityRepository.replaceForMember와 동일한 방식)
     */
    public boolean replaceForMember(String roomCode, String memberId, List<TravelDateEntry> newEntries) {
        List<TravelDateEntry> all = loadAll();

        List<TravelDateEntry> kept = new ArrayList<>();
        for (TravelDateEntry entry : all) {
            boolean isThisMemberAndRoom = entry.getRoomCode().equals(roomCode) && entry.getMemberId().equals(memberId);
            if (!isThisMemberAndRoom) {
                kept.add(entry);
            }
        }

        kept.addAll(newEntries);
        saveAll(kept);
        return true;
    }
}
