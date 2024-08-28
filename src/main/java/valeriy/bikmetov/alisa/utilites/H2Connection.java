package valeriy.bikmetov.alisa.utilites;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Валерий Бикметов
 */
public final class H2Connection {
    private Connection connection;
    private static int count = 0;

    public H2Connection(String url) throws ClassNotFoundException, SQLException  {
        makeConnection(url);
    }

    private void makeConnection(String url) throws ClassNotFoundException, SQLException {
        if(count == 0) {
            Class.forName(Constants.NAME_DRIVER_DB);
            if(url.startsWith(Constants.PREFIX_URL_DB))
                url += Constants.AUTO_SERVER_DB;
            else
                url = Constants.PREFIX_URL_DB + url + Constants.AUTO_SERVER_DB;
        }
        else {
            ++count;
        }
        connection = DriverManager.getConnection(url,  Constants.ADMIN_DB, Constants.PASSWORD_DB);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        this.connection.close();
    }

    public static int getCount() {
        return count;
    }
}
