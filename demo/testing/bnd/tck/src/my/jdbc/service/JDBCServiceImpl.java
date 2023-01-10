package my.jdbc.service;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jdbc.DataSourceFactory;

@Component
public class JDBCServiceImpl implements DataSourceFactory {

	@Override
	public DataSource createDataSource(Properties props) throws SQLException {
		return null;
	}

	@Override
	public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props) throws SQLException {
		return null;
	}

	@Override
	public XADataSource createXADataSource(Properties props) throws SQLException {
		return null;
	}

	@Override
	public Driver createDriver(Properties props) throws SQLException {
		return new Driver() {

			@Override
			public boolean jdbcCompliant() {
				return false;
			}

			@Override
			public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
				throw new SQLException();
			}

			@Override
			public Logger getParentLogger() throws SQLFeatureNotSupportedException {
				throw new SQLFeatureNotSupportedException();
			}

			@Override
			public int getMinorVersion() {
				return 0;
			}

			@Override
			public int getMajorVersion() {
				return 1;
			}

			@Override
			public Connection connect(String url, Properties info) throws SQLException {
				throw new SQLException();
			}

			@Override
			public boolean acceptsURL(String url) throws SQLException {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}

}
