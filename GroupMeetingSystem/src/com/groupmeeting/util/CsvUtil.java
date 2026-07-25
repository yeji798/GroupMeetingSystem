package com.groupmeeting.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 간단한 CSV 인코딩/디코딩을 지원하는 유틸리티 클래스입니다.
 *
 * - 값에 콤마(,) 또는 큰따옴표(")가 포함된 경우 큰따옴표로 감싸서 처리합니다.
 * - 표준 CSV 규칙(RFC 4180)의 핵심 부분만 간단히 구현한 버전입니다.
 *   (대학 프로젝트 수준에서 회원 이름, 이메일 등 텍스트 데이터를 다루기에 충분합니다.)
 */
public class CsvUtil {

    /**
     * 하나의 값(value)을 CSV에 안전하게 기록할 수 있도록 이스케이프 처리합니다.
     * 콤마, 큰따옴표, 줄바꿈이 포함된 경우에만 큰따옴표로 감쌉니다.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // 큰따옴표는 두 번 반복(""))하여 이스케이프 처리
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return value;
    }

    /**
     * 여러 개의 컬럼 값을 받아서 하나의 CSV 라인 문자열로 합칩니다.
     */
    public static String toCsvLine(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escape(values[i]));
        }
        return sb.toString();
    }

    /**
     * CSV 한 줄을 파싱하여 각 컬럼 값의 리스트로 반환합니다.
     * 큰따옴표로 감싸진 콤마/줄바꿈도 올바르게 처리합니다.
     */
    public static List<String> parseLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // 다음 문자도 큰따옴표라면 이스케이프된 큰따옴표
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // 다음 큰따옴표는 건너뜀
                    } else {
                        inQuotes = false; // 인용 종료
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == ',') {
                    tokens.add(current.toString());
                    current.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    current.append(c);
                }
            }
        }
        tokens.add(current.toString());
        return tokens;
    }
}
