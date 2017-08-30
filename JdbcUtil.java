import java.sql.*;
import java.util.*;
import java.io.*;
import java.lang.*;

public class JdbcUtil {

    static final List<String> DRIVERS;

    static {
        DRIVERS = new ArrayList<>();
        DRIVERS.add("oracle.jdbc.driver.OracleDriver");
        DRIVERS.add("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVERS.add("com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource");
        DRIVERS.add("com.mysql.jdbc.Driver");
        DRIVERS.add("com.ibm.db2.jcc.DB2Driver");
        DRIVERS.add("org.h2.Driver");

    }

    static String findDriver() {

        for (String driver : DRIVERS) {
            try {
                System.err.println("Trying driver: " + driver);
                Class.forName(driver, false, JdbcUtil.class.getClassLoader());
                System.err.println("Loaded JDBC driver: [" + driver + "]");
                return driver;
            } catch (Exception e) {
            }
        }
        return null;
    }

    static void addDriver(String driverClassName) {
        System.err.println("Adding driver to the list: " + driverClassName);
        DRIVERS.add(driverClassName);
    }

    //private final static String DB_URL = "jdbc:oracle:thin:@localhost:1521:mydatabase";
    //private final static String USER = "UsernAme";
    //private final static String PASS = "password123";

    private static String url_base = "jdbc:oracle:thin:";

    private static String username = "";
    private static String password = "";
    private static String url = "";

    private static String _qry = null;
    private static String _qryFile = null;
    private static String _delim = "|";

    private static long pagesize = -1;
    private static long pageStart = 0;
    private static long pageEnd = 0;

    private static Connection conn = null;

    public static void main(String[] args) {
        parseParams(args);

        if (!url.toLowerCase().startsWith("jdbc")) {
            url = url_base + "@" + url;
        }

        try {
            String driverName = findDriver();
            if (driverName == null) {
                System.err.println("Cannot find any known JDBC driver in the classpath.");
                System.exit(1);
            }
            Class.forName(driverName);

            readQuery();

            System.err.println("Connecting to database: " + url + " using credentials: " + username + ":" + "******" + "...");
            //conn = DriverManager.getConnection(DB_URL,USER,PASS);
            conn = DriverManager.getConnection(url, username, password);
            System.err.println("Connected");
            if (_qry != null) {
                for (String singleQuery : _qry.split(";")) {
                    sendQuery(conn, singleQuery.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    // ignore
                }
            }
        }
    }

    private static void parseParams(String[] params) {
        if (params.length < 3) {
            System.err.println("At least 3 arguments must be provided.");
            System.out.println(USAGE);
            System.exit(1);
        }

        for (int i = 0; i < params.length; i++) {
            String arg = params[i];

        /* java6 is unable to switch strings */
            // @formatter:off
            if ("-u".equals(arg))   { username = params[++i];  } else
            if ("-p".equals(arg))   { password = params[++i];  } else
            if ("-U".equals(arg))   { url      = params[++i];  } else
            if ("-q".equals(arg))   { _qry     = params[++i];  } else
            if ("-d".equals(arg))   { _delim   = params[++i];  } else
            if ("-dr".equals(arg))  { addDriver(params[++i]);  } else
            if ("-if".equals(arg))  { _qryFile = params[++i];  } else
            if ("-pg".equals(arg))  { parsePages(params[++i]); } else
            {
                System.err.println("Unknown argument: " + arg);
                System.err.println(USAGE);
                System.exit(1);
            }
            // @formatter:on
        }

    }

    private static void parsePages(String expr) {
        if (expr == null || expr.isEmpty()) {
            return;
        }

        String pagesizeStr = "0";
        String pageStartStr = "1";
        String pageEndStr = "1";

        long _pagesize = 0;
        long _pageStart = 0;
        long _pageEnd = 0;

        String[] tokens = expr.split(":");
        if (tokens != null && tokens.length > 0) {
            if (tokens.length > 1) {
                pagesizeStr = tokens[1];
                if (pagesizeStr != null && !pagesizeStr.isEmpty()) {
                    _pagesize = Long.parseLong(pagesizeStr);
                }
            }

            tokens = tokens[0].split("-");
            if (tokens != null && tokens.length > 0) {
                pageStartStr = tokens[0];
                pageEndStr = pageStartStr;
                if (tokens.length > 1) {
                    pageEndStr = tokens[1];
                }


                if (pageStartStr != null && ! pageStartStr.isEmpty()) {
                    _pageStart = Long.parseLong(pageStartStr);
                } else {
                    _pageStart = 0;
                }

                if (pageEndStr != null && ! pageEndStr.isEmpty()) {
                    _pageEnd = Long.parseLong(pageEndStr);
                } else {
                    _pageEnd = pageStart;
                }

            }
        }

        if (_pageStart > _pageEnd) {
            throw new IllegalStateException("Starting page number cannot be lower than ending page");
        }

        System.err.println(String.format("Start page: %s, end page: %s, page size: %s", _pageStart, _pageEnd, _pagesize));

        pagesize = _pagesize;
        pageStart = _pageStart;
        pageEnd = _pageEnd;
    }

    private static String readQuery() {

        InputStream is = null;

        if (_qryFile == null || _qryFile.isEmpty()) {
            return _qry;
        }

        if ("--".equals(_qryFile)) {
            System.err.println("Reading query from stdin");
            try {
                _qry = inputStreamToString(System.in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return _qry;
        }

        if (_qryFile.startsWith("file://")) {
            System.err.println("Reading query from file: " + _qryFile);
            _qryFile = _qryFile.split("file://")[1];
            try {
                is = new FileInputStream(_qryFile);
                _qry = inputStreamToString(is);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(is);
            }
            return _qry;
        }

        return null;
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        try {
            return result.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void sendQuery(Connection connection, String qry) {
        String qry_low = qry.toLowerCase();
        //    qry = qry.toLowerCase();
        if (qry_low.startsWith("select")) {
            System.err.println("Sending query: [" + qry + "]");
            sendSelectQuery(connection, qry);
        } else if (qry_low.startsWith("insert")) {
            sendInsertQuery(connection, qry);
        } else if (qry_low.startsWith("show")) {
            sendSelectQuery(connection, qry);
        } else if (qry_low.startsWith("update")) {
            if (!qry_low.contains(" where ")) {
                System.err.println("WHERE clause is missing.");
                System.exit(4);
            }

            sendUpdateQuery(connection, qry);
        } else if (qry_low.startsWith("delete")) {
            if (!qry_low.contains(" where ")) {
                System.err.println("WHERE clause is missing.");
                System.exit(4);
            }

            sendDeleteQuery(connection, qry);
        } else {
            System.err.println("Unrecognized query: [" + qry + "]");
        }
    }

    private static void sendSelectQuery(Connection connection, String query) {
        ResultSet rs = null;
        try {
            Statement stmt = connection.createStatement();
            rs = stmt.executeQuery(query);
            ResultSetMetaData meta = rs.getMetaData();
            int colsCnt = meta.getColumnCount();
            int rownum = 0;
            String data;

            long currentPage = 0;
            long currentPageRow = -1;

            System.out.print("Row#");
            for (int i = 1; i <= colsCnt; i++) {
                System.out.print(_delim + meta.getColumnName(i));
            }
            System.out.println();

            while (rs.next()) {
                rownum++;
                currentPageRow++;
                if (currentPageRow == pagesize) {
                    currentPage++;
                    currentPageRow = 0;
                    if (currentPage >= pageStart && currentPage <= pageEnd) {
                        System.err.println("Page #"+currentPage);
                    }
                }
                if (currentPage >= pageStart) {
                    if (currentPage > pageEnd) {
                        System.err.println("End of last page: #"+pageEnd);
                        break;
                    }
                } else {
                    continue;
                }

                System.out.print(rownum);

                for (int i = 1; i <= colsCnt; i++) {
                    System.out.print(_delim + rs.getObject(i));
                }

                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        } finally {
            try {
                rs.close();
            } catch (Exception ex) {
            }
        }

    }

    private static void sendUpdateQuery(Connection connection, String query) {
        try {
            Statement stmt = connection.createStatement();
            int updatedCount = stmt.executeUpdate(query);

            System.out.println("Updated rows: " + updatedCount);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }

    }

    private static void sendInsertQuery(Connection connection, String query) {
        try {
            Statement stmt = connection.createStatement();
            int updatedCount = stmt.executeUpdate(query);

            System.out.println("Inserted rows: " + updatedCount);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }

    }

    private static void sendDeleteQuery(Connection connection, String query) {
        try {
            Statement stmt = connection.createStatement();
            int updatedCount = stmt.executeUpdate(query);

            System.out.println("Deleted rows: " + updatedCount);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }

    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final String USAGE = ""
            + "USAGE:\n"
            + "  * -u   <username>\n"
            + "  * -p   <password>\n"
            + "  * -U   <url>       : e.g.: localhost:1521:mydatabase; jdbc:oracle:thin:@localhost:1521:mydatabase \n"
            + "    -q   <query>\n"
            + "    -d   <delimiter> : delimiter to separate columns in SELECT output\n"
            + "    -dr  <driver>    : custom driver class name, e.g.: -dr 'org.h2.Driver'\n"
            + "    -pg  <page>      : pages to select. e.g. 3-4:20 will print pages 3 and 4 of size 20. 3:20 will only print pg #3\n"
            + "    -if  <file>      : query input file. Either 'file:///some/path/to/file.sql' or '--' for stdin\n";

}
