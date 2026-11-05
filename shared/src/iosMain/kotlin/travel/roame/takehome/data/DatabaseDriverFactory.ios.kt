package travel.roame.takehome.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import travel.roame.takehome.db.FlightDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(FlightDatabase.Schema, "flights.db")
}
