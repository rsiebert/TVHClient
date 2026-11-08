package org.tvheadend.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.data.testutil.TestDatabase

/**
 * Guards the exported-schema contract.
 *
 * `exportSchema = true` writes `data/schemas/<version>.json`, which is the base any future migration
 * is tested against. Bumping `@Database(version = ...)` without exporting the matching schema - or
 * without writing the migration itself - would silently break that, so the live version is pinned
 * here. When the version is bumped on purpose, bump this assertion too and add a `MigrationTest`
 * covering `15 -> N`; nothing else needs wiring, `room.schemaLocation` is already set in
 * `data/build.gradle`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchemaTest {

    @Test
    fun theLiveDatabaseVersionMatchesTheExportedSchema() {
        val db = TestDatabase.create()
        try {
            assertThat(db.openHelper.readableDatabase.version).isEqualTo(15)
        } finally {
            TestDatabase.teardown(db)
        }
    }
}
