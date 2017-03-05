

Created this tiny util when I needed to test some ORA drivers and see how they behave in my infra. Added a few bells and whistles since then. Still it is a very simple though sometimes very useful util.


EXAMPLE:

    netikras@netikras-xps /tmp/playground/JdbcUtil $ java -cp .:/home/netikras/received/h2/h2-1.4.192.jar JdbcUtil -u root -p test123 -U jdbc:h2:mem:test1 -q "show databases"  | column -s '|' -t
    Trying driver: oracle.jdbc.driver.OracleDriver
    Trying driver: com.microsoft.sqlserver.jdbc.SQLServerDriver
    Trying driver: com.mysql.jdbc.Driver
    Trying driver: com.ibm.db2.jcc.DB2Driver
    Trying driver: org.h2.Driver
    Loaded JDBC driver: [org.h2.Driver]
    Connecting to database: jdbc:h2:mem:test1 using credentials: root:******...
    Connected
    Row#  SCHEMA_NAME
    1     INFORMATION_SCHEMA
    2     PUBLIC
    netikras@netikras-xps /tmp/playground/JdbcUtil $ 


    netikras@netikras-xps /tmp/playground/JdbcUtil $ java -cp .:/home/netikras/received/h2/h2-1.4.192.jar JdbcUtil -u root -p test123 -U jdbc:h2:mem:test1 -q "show databases"  -dr org.h2.Driver 2>/dev/null | column -s '|' -t
    Row#  SCHEMA_NAME
    1     INFORMATION_SCHEMA
    2     PUBLIC
    netikras@netikras-xps /tmp/playground/JdbcUtil $ 


