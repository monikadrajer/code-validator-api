package org.sitenv.vocabularies.loader.code;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.log4j.Logger;
import org.sitenv.vocabularies.loader.BaseCodeLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Brian on 2/7/2016.
 */
@Component(value = "CPT")
public class CptLoader extends BaseCodeLoader {
    private static Logger logger = Logger.getLogger(CptLoader.class);
    private String oid;
    @Value("${CPT_FILE_START_READ_LINE_NUMBER:34}")
    private int CODES_START_AT_THIS_LINE_NUMBER;

    public CptLoader() {
        this.oid = CodeSystemOIDs.CPT4.codesystemOID();
    }

    @Override
    public void load(List<File> filesToLoad, Connection connection) {
        BufferedReader br = null;
        FileReader fileReader = null;
        try {
            StrBuilder insertQueryBuilder = new StrBuilder(codeTableInsertSQLPrefix);
            int totalCount = 0, pendingCount = 0;

            for (File file : filesToLoad) {
                if (file.isFile() && !file.isHidden()) {
                    logger.debug("Loading CPT File: " + file.getName());
                    String codeSystem = file.getParentFile().getName();
                    fileReader = new FileReader(file);
                    br = new BufferedReader(fileReader);
                    String line;
                    int currentLineNumberCounter = 0;
                    while ((line = br.readLine()) != null) {
                        if (!line.isEmpty() && currentLineNumberCounter >= CODES_START_AT_THIS_LINE_NUMBER-1) {
                            String code = line.substring(0, 5);
                            String displayName = line.substring(line.indexOf(" "));
                            buildCodeInsertQueryString(insertQueryBuilder, code, displayName, codeSystem, oid, CODES_IN_THIS_SYSTEM_ARE_ALWAYS_ACTIVE);

                            if ((++totalCount % BATCH_SIZE) == 0) {
                                insertCode(insertQueryBuilder.toString(), connection);
                                insertQueryBuilder.clear();
                                insertQueryBuilder.append(codeTableInsertSQLPrefix);
                                pendingCount = 0;
                            }
                        }
                        currentLineNumberCounter++;
                    }
                }
            }
            if (pendingCount > 0) {
                insertCode(insertQueryBuilder.toString(), connection);
            }
        } catch (IOException e) {
            logger.error(e);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    fileReader.close();
                    br.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }
}
