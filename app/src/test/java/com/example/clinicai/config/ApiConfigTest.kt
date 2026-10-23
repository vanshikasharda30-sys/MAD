package com.example.clinicai.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiConfigTest {
    private val dev = "http://10.0.2.2:3000/"
    private val prod = "https://clinicai-555888.vercel.app/"

    @Test fun developmentUsesEmulatorHostAddress() {
        assertEquals(dev, ApiConfig.resolveBaseUrl(ApiConfig.Environment.DEVELOPMENT, dev, prod))
    }

    @Test fun productionUsesDeployedHttpsUrl() {
        assertEquals(prod, ApiConfig.resolveBaseUrl(ApiConfig.Environment.PRODUCTION, dev, prod))
    }

    @Test fun productionRejectsPlainHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig.resolveBaseUrl(ApiConfig.Environment.PRODUCTION, dev, "http://example.com/")
        }
    }

    @Test fun productionRejectsEmulatorUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig.resolveBaseUrl(ApiConfig.Environment.PRODUCTION, dev, "https://10.0.2.2/")
        }
    }

    @Test fun baseUrlMustEndWithSlashForRetrofit() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiConfig.resolveBaseUrl(ApiConfig.Environment.PRODUCTION, dev, "https://example.com")
        }
    }

    @Test fun unknownEnvironmentKeyFallsBackToProduction() {
        assertEquals(ApiConfig.Environment.PRODUCTION, ApiConfig.Environment.fromKey("staging"))
        assertEquals(ApiConfig.Environment.DEVELOPMENT, ApiConfig.Environment.fromKey("dev"))
    }

    @Test fun builtConfigurationIsConsistent() {
        // Whatever the build selected must itself pass the production safety rules when it is production.
        if (ApiConfig.isProduction) assert(ApiConfig.baseUrl.startsWith("https://"))
    }
}
