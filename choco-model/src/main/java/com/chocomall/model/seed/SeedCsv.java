package com.chocomall.model.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;

/** Reads the CSV files in {@code classpath:/seed} that were generated from the shop guide workbook. */
final class SeedCsv {

    private SeedCsv() {
    }

    static List<Map<String, String>> read(String file) {
        try (InputStream in = new ClassPathResource("seed/" + file).getInputStream();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(record.toMap());
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read seed file " + file, e);
        }
    }

    /** Empty strings become null so optional columns stay NULL in the database. */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
