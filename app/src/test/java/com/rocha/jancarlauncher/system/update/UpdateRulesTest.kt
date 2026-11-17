package com.rocha.jancarlauncher.system.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRulesTest {
    private val apkUrl = "https://github.com/Rochafelip/JancarLauncher/releases/download/v1.8.0/app-release.apk"

    @Test fun parseLatest_picksApkAsset() {
        val json = """{"tag_name":"v1.8.0","assets":[
            {"name":"notes.txt","browser_download_url":"https://x/notes.txt"},
            {"name":"app-release.apk","browser_download_url":"$apkUrl"}]}"""
        assertEquals(Release("v1.8.0", apkUrl), UpdateRules.parseLatest(json))
    }

    @Test fun parseLatest_noApk_isNull() {
        assertNull(UpdateRules.parseLatest("""{"tag_name":"v1.8.0","assets":[{"name":"a.zip","browser_download_url":"https://x/a.zip"}]}"""))
    }

    @Test fun parseLatest_garbage_isNull() {
        assertNull(UpdateRules.parseLatest("{"))
        assertNull(UpdateRules.parseLatest(""))
        assertNull(UpdateRules.parseLatest("""{"message":"API rate limit exceeded"}"""))
    }

    @Test fun isNewer_comparesNumbers() {
        assertTrue(UpdateRules.isNewer("v1.8.0", "1.7.3"))
        assertFalse(UpdateRules.isNewer("v1.7.3", "1.7.3"))
        assertTrue(UpdateRules.isNewer("v1.7.10", "1.7.9"))
        assertFalse(UpdateRules.isNewer("vX", "1.7.3"))
    }

    private val installed = ApkInfo("com.rocha.jancarlauncher", 15, setOf("aa"))

    @Test fun verdict_reasons() {
        assertEquals(Verdict.Unreadable, UpdateRules.verdict(null, installed))
        assertEquals(Verdict.OtherPackage, UpdateRules.verdict(installed.copy(pkg = "org.videolan.vlc", versionCode = 99), installed))
        assertEquals(Verdict.OtherSignature, UpdateRules.verdict(installed.copy(versionCode = 16, certSha256 = setOf("bb")), installed))
        assertEquals(Verdict.NotNewer, UpdateRules.verdict(installed, installed))
        assertEquals(Verdict.NotNewer, UpdateRules.verdict(installed.copy(versionCode = 14), installed))
        assertEquals(Verdict.Ok, UpdateRules.verdict(installed.copy(versionCode = 16), installed))
    }
}
