/*
 * Copyright 2008 pcal.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.pcal.sqlsheet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Properties;

/**
 * SqlSheet implementation of java.sql.Driver.
 *
 * @author <a href='http://www.pcal.net'>pcal</a>
 */
public class XlsDriver implements Driver {

    // =========================================================================
    // Constants

    private final static String URL_SCHEME = "jdbc:xls:";

    // =========================================================================
    // Static initializer

    static {
        try {
            DriverManager.registerDriver(new XlsDriver());
        } catch (SQLException e) {
            System.err.println("Couldn't register " + XlsDriver.class.getName());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // Driver implementation

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    public Connection connect(String jdbcUrl, Properties info) throws SQLException {
        if (jdbcUrl == null)
            throw new IllegalArgumentException("Null url");
        if (!acceptsURL(jdbcUrl))
            return null; // why is this necessary?
        if (!jdbcUrl.startsWith(URL_SCHEME)) {
            throw new IllegalArgumentException("URL is not " + URL_SCHEME + " (" + jdbcUrl + ")");
        }
        try {
            String workbookUrl = jdbcUrl.substring(URL_SCHEME.length());

            if (workbookUrl.startsWith("file:")) {
                File saveFile = new File(workbookUrl.substring("file:".length()));
                Workbook workbook;

                if (!saveFile.exists() || (saveFile.length() == 0)) {
                    if (workbookUrl.toLowerCase().endsWith("x")) {
                        workbook = new XSSFWorkbook();
                        workbook.createSheet();
                    } else {
                        workbook = new HSSFWorkbook();
                    }

                    FileOutputStream fileOut = null;
                    try {
                        fileOut = new FileOutputStream(saveFile);
                        workbook.write(fileOut);
                        fileOut.flush();
                    } finally {
                        if (fileOut != null) {
                            try {
                                fileOut.close();
                            } catch (IOException ohwell) {
                                ohwell.printStackTrace();
                            }
                        }
                    }
                }

                FileInputStream fin = null;
                try {
                    fin = new FileInputStream(saveFile);
                    workbook = WorkbookFactory.create(fin);
                } finally {
                    if (fin != null)
                        fin.close();
                }

                return new XlsConnection(workbook, saveFile);
            } else {
                URL url = new URL(workbookUrl);
                return new XlsConnection(WorkbookFactory.create(url.openStream()));
            }
        } catch (Exception e) {
            SQLException sqe = new SQLException(e.getMessage());
            sqe.initCause(e);
            throw sqe;
        }
    }

    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.trim().startsWith(URL_SCHEME);
    }

    public boolean jdbcCompliant() { // LOLZ!
        return false;
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 0;
    }

}
