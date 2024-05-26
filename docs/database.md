# Database configuration

Database configuration is stored in the [persistence.xml](/src/main/resources/META-INF/persistence.xml) file.

## Common options

`hibernate.hikari.username`: `<username>`  
`hibernate.hikari.password`: `<user password>`

`hibernate.hbm2ddl.auto`:

* `create-drop`: Create the database schema on startup and drop it on shutdown. All data is lost after shutdown. Useful for development.
* `update`: Update the database schema on startup if changed. This is the recommended option.
* `validate`: Validate the database schema on startup and throw an exception if it is invalid. This is the safest option.

## Database drivers

[H2](https://h2database.com/) and [MariaDB](https://mariadb.org/) drivers are supported out of the box.

### H2

`hibernate.dialect`: `org.hibernate.dialect.H2Dialect`  
`hibernate.hikari.dataSourceClassName`: `org.h2.jdbcx.JdbcDataSource`

#### In-memory

Data is stored in memory, everything is lost when the server is restarted. Useful for testing.

`hibernate.hikari.dataSource.url`: `jdbc:h2:mem:<database>` (e.g `jdbc:h2:mem:protanki-server`)

#### File

Data is stored in a file, persists across server restarts. This is default option.

`hibernate.hikari.dataSource.url`: `jdbc:h2:file:<filename>` (e.g. `jdbc:h2:file:./protanki-server`)

### MariaDB

Data is stored on a MariaDB server. This is the recommended option.

`hibernate.dialect`: `org.hibernate.dialect.MariaDBDialect`  
`hibernate.hikari.dataSourceClassName`: `org.mariadb.jdbc.MariaDbDataSource`  
`hibernate.hikari.dataSource.url`: `jdbc:mariadb://<host>:<port>/<database>` (e.g. `jdbc:mariadb://localhost:3306/protanki-server`)
