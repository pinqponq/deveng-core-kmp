package figmasync.generator

import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun testSchema(): SchemaFile {
    val schemaJson = TestFixtures::class.java.getResource("/schema.json")!!.readText()
    return json.decodeFromString(schemaJson)
}

private object TestFixtures
