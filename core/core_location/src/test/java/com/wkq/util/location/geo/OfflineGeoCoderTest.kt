package com.wkq.util.location.geo

import com.wkq.util.location.geo.dao.GeoDao
import com.wkq.util.location.geo.entity.Admin1Entity
import com.wkq.util.location.geo.entity.CityEntity
import com.wkq.util.location.geo.entity.CountryEntity
import com.wkq.util.location.geo.util.GeoDistanceUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.sql.DriverManager
import java.util.Locale

class OfflineGeoCoderTest {

    private val mockCities = listOf(
        CityEntity(1790437, "Zhengzhou", "郑州市", 34.7466, 113.6254, "CN", "24", 4253913),
        CityEntity(1850147, "Tokyo", "东京都", 35.6762, 139.6503, "JP", "40", 8336599),
        CityEntity(5368361, "Los Angeles", "洛杉矶", 34.0522, -118.2437, "US", "CA", 3971883),
        CityEntity(2988507, "Paris", "巴黎", 48.8566, 2.3522, "FR", "A8", 2148271)
    )

    private val mockCountries = mapOf(
        "CN" to CountryEntity("CN", "China", "中国"),
        "JP" to CountryEntity("JP", "Japan", "日本"),
        "US" to CountryEntity("US", "United States", "美国"),
        "FR" to CountryEntity("FR", "France", "法国")
    )

    private val mockAdmin1s = mapOf(
        "CN.24" to Admin1Entity("CN", "24", "Henan", "河南省"),
        "JP.40" to Admin1Entity("JP", "40", "Tokyo", "東京都"),
        "US.CA" to Admin1Entity("US", "CA", "California", "加利福尼亚州"),
        "FR.A8" to Admin1Entity("FR", "A8", "Île-de-France", "法兰西岛")
    )

    private val mockAlternateNames = mapOf(
        "city.1850147.ja" to "東京",
        "admin1.JP.40.ja" to "東京都",
        "city.2988507.ja" to "パリ",
        "admin1.FR.A8.ja" to "イル＝ド＝フランス"
    )

    // 手动实现 Mock 版本的 GeoDao
    private val mockGeoDao = object : GeoDao {
        override suspend fun findNearbyCities(
            minLat: Double,
            maxLat: Double,
            minLng: Double,
            maxLng: Double
        ): List<CityEntity> {
            return mockCities.filter {
                it.latitude in minLat..maxLat && it.longitude in minLng..maxLng
            }
        }

        override suspend fun getCountry(countryCode: String): CountryEntity? {
            return mockCountries[countryCode]
        }

        override suspend fun getAdmin1(countryCode: String, admin1Code: String): Admin1Entity? {
            return mockAdmin1s["$countryCode.$admin1Code"]
        }

        override suspend fun getAlternateName(entityType: String, entityId: String, lang: String): String? {
            return mockAlternateNames["$entityType.$entityId.$lang"]
        }

        override suspend fun insertAlternateNames(alternateNames: List<com.wkq.util.location.geo.entity.AlternateNameEntity>) = Unit

        override suspend fun insertCountries(countries: List<CountryEntity>) = Unit
        override suspend fun insertAdmin1s(admin1s: List<Admin1Entity>) = Unit
        override suspend fun insertCities(cities: List<CityEntity>) = Unit
    }

    private lateinit var coder: OfflineGeoCoder

    @Before
    fun setUp() {
        // 1. 生成资产测试数据库，该文件会写入 assets 供项目打包使用
        createAssetDatabaseFile()
        // 2. 初始化反查服务
        coder = OfflineGeoCoderImpl(mockGeoDao)
    }

    @Test
    fun testHaversineDistance() {
        // 验证 Haversine 大圆算法的正确性
        // 从 郑州(34.7466, 113.6254) 到 东京(35.6762, 139.6503) 距离约为 2420 公里
        val dist = GeoDistanceUtils.haversine(34.7466, 113.6254, 35.6762, 139.6503)
        assertEquals(2359.7, dist, 5.0) // 真实距离约为 2359.7 公里，误差控制在 5 公里以内
    }

    @Test
    fun testReverseGeocodeZhengzhouInZh() = runBlocking {
        // 设置当前 Locale 为中文
        Locale.setDefault(Locale.CHINA)
        // 输入郑州附近的经纬度，反查测试
        val loc = coder.reverseGeocode(34.75, 113.62)
        assertNotNull(loc)
        assertEquals("CN", loc!!.countryCode)
        assertEquals("中国", loc.countryName)
        assertEquals("河南省", loc.stateName)
        assertEquals("郑州市", loc.cityName)
        assertTrue(loc.distanceKm < 10.0)
    }

    @Test
    fun testReverseGeocodeZhengzhouInEn() = runBlocking {
        // 设置当前 Locale 为英文
        Locale.setDefault(Locale.US)
        val loc = coder.reverseGeocode(34.75, 113.62)
        assertNotNull(loc)
        assertEquals("CN", loc!!.countryCode)
        assertEquals("China", loc.countryName)
        assertEquals("Henan", loc.stateName)
        assertEquals("Zhengzhou", loc.cityName)
    }

    @Test
    fun testReverseGeocodeTokyoInZh() = runBlocking {
        Locale.setDefault(Locale.CHINA)
        val loc = coder.reverseGeocode(35.68, 139.66)
        assertNotNull(loc)
        assertEquals("JP", loc!!.countryCode)
        assertEquals("日本", loc.countryName)
        assertEquals("東京都", loc.stateName)
        assertEquals("东京都", loc.cityName)
    }

    @Test
    fun testReverseGeocodeTokyoInJa() = runBlocking {
        Locale.setDefault(Locale.JAPAN)
        val loc = coder.reverseGeocode(35.68, 139.66)
        assertNotNull(loc)
        assertEquals("JP", loc!!.countryCode)
        assertEquals("日本", loc.countryName)
        assertEquals("東京都", loc.stateName)
        assertEquals("東京", loc.cityName)
    }

    @Test
    fun testReverseGeocodeParisInJa() = runBlocking {
        Locale.setDefault(Locale.JAPAN)
        val loc = coder.reverseGeocode(48.85, 2.35)
        assertNotNull(loc)
        assertEquals("FR", loc!!.countryCode)
        assertEquals("フランス", loc.countryName)
        assertEquals("イル＝ド＝フランス", loc.stateName)
        assertEquals("パリ", loc.cityName)
    }

    @Test
    fun testIllegalCoordinates() = runBlocking {
        // 验证非法输入
        val loc = coder.reverseGeocode(100.0, 200.0)
        assertNull(loc)
    }

    private fun createAssetDatabaseFile() {
        val assetsDir = File("src/main/assets")
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }
        val dbFile = File(assetsDir, "geonames.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS geonames_countries (
                            country_code TEXT PRIMARY KEY,
                            name TEXT,
                            name_zh TEXT
                        )
                    """
                    )
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS geonames_admin1 (
                            country_code TEXT,
                            admin1_code TEXT,
                            name TEXT,
                            name_zh TEXT,
                            PRIMARY KEY(country_code, admin1_code)
                        )
                    """
                    )
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS geonames_cities (
                            id INTEGER PRIMARY KEY,
                            name TEXT,
                            name_zh TEXT,
                            latitude REAL,
                            longitude REAL,
                            country_code TEXT,
                            admin1_code TEXT,
                            population INTEGER
                        )
                    """
                    )
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS geonames_alternate_names (
                            entity_type TEXT,
                            entity_id TEXT,
                            lang TEXT,
                            name TEXT,
                            PRIMARY KEY(entity_type, entity_id, lang)
                        )
                    """
                    )
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_coords ON geonames_cities(latitude, longitude)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_cities_codes ON geonames_cities(country_code, admin1_code)")

                    stmt.execute("INSERT OR REPLACE INTO geonames_countries VALUES ('CN', 'China', '中国')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_countries VALUES ('JP', 'Japan', '日本')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_countries VALUES ('US', 'United States', '美国')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_countries VALUES ('FR', 'France', '法国')")

                    stmt.execute("INSERT OR REPLACE INTO geonames_admin1 VALUES ('CN', '24', 'Henan', '河南省')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_admin1 VALUES ('JP', '40', 'Tokyo', '東京都')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_admin1 VALUES ('US', 'CA', 'California', '加利福尼亚州')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_admin1 VALUES ('FR', 'A8', 'Île-de-France', '法兰西岛')")

                    stmt.execute("INSERT OR REPLACE INTO geonames_cities VALUES (1790437, 'Zhengzhou', '郑州市', 34.7466, 113.6254, 'CN', '24', 4253913)")
                    stmt.execute("INSERT OR REPLACE INTO geonames_cities VALUES (1850147, 'Tokyo', '东京都', 35.6762, 139.6503, 'JP', '40', 8336599)")
                    stmt.execute("INSERT OR REPLACE INTO geonames_cities VALUES (5368361, 'Los Angeles', '洛杉矶', 34.0522, -118.2437, 'US', 'CA', 3971883)")
                    stmt.execute("INSERT OR REPLACE INTO geonames_cities VALUES (2988507, 'Paris', '巴黎', 48.8566, 2.3522, 'FR', 'A8', 2148271)")

                    stmt.execute("INSERT OR REPLACE INTO geonames_alternate_names VALUES ('city', '1850147', 'ja', '東京')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_alternate_names VALUES ('admin1', 'JP.40', 'ja', '東京都')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_alternate_names VALUES ('city', '2988507', 'ja', 'パリ')")
                    stmt.execute("INSERT OR REPLACE INTO geonames_alternate_names VALUES ('admin1', 'FR.A8', 'ja', 'イル＝ド＝フランス')")
                }
            }
            println("geonames.db generated successfully at: ${dbFile.absolutePath}")
        } catch (e: Exception) {
            System.err.println("Failed to write geonames.db to assets: ${e.message}")
        }
    }
}
