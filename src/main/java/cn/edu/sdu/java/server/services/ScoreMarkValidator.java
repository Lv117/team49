package cn.edu.sdu.java.server.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

final class ScoreMarkValidator {
    static final String INVALID_MSG = "成绩必须在0–100之间，请重新输入";
    private static final Pattern MARK_PATTERN = Pattern.compile("^(100(\\.0)?|\\d{1,2}(\\.\\d)?)$");
    private static final BigDecimal MAX = new BigDecimal("100");

    private ScoreMarkValidator() {
    }

    static BigDecimal parseOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (!MARK_PATTERN.matcher(s).matches()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(MAX) > 0) {
                return null;
            }
            if (v.scale() > 1) {
                return null;
            }
            return v.setScale(1, RoundingMode.UNNECESSARY);
        } catch (Exception e) {
            return null;
        }
    }
}

