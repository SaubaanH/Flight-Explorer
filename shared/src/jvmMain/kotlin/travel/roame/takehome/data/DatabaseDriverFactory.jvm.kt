package travel.roame.takehome.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import travel.roame.takehome.db.FlightDatabase

actual class DatabaseDriverFactory(private val url: String = JdbcSqliteDriver.IN_MEMORY) {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(url)
        FlightDatabase.Schema.create(driver)
        return driver
    }
}
