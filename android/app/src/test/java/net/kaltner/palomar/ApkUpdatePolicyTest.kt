package net.kaltner.palomar

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApkUpdatePolicyTest {
    private fun release(version: String = "1.1.0") =
        PalomarRelease(
            version = version,
            tag = "v$version",
            title = "Palomar $version",
            publishedAt = "2026-08-31T00:00:00Z",
            releaseNotesUrl = "https://github.com/kaltner-net/palomar/releases/tag/v$version",
            artifactAvailable = true,
        )

    private fun assets(version: String = "1.1.0"): List<AndroidReleaseAsset> {
        val tag = "v$version"
        fun asset(name: String, size: Long = 100) =
            AndroidReleaseAsset(name, size, "$OFFICIAL_RELEASE_DOWNLOAD_PREFIX$tag/$name")
        return listOf(
            asset("palomar-$tag.apk", 10_000),
            asset("palomar-linux-$tag.tar.gz", 20_000),
            asset("palomar-SHA256SUMS"),
            asset("palomar-SHA256SUMS.sig"),
            asset("palomar-release-cert.pem"),
        )
    }

    private fun expectCode(code: String, block: () -> Unit) {
        try {
            block()
            fail("expected $code")
        } catch (error: ApkUpdateValidationException) {
            assertEquals(code, error.code)
        }
    }

    @Test
    fun exactApkAndVerificationAssetsAreSelected() {
        val selected = selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets())
        assertEquals("palomar-v1.1.0.apk", selected.apk.name)
        assertEquals("palomar-SHA256SUMS", selected.checksumManifest.name)
    }

    @Test
    fun missingDuplicateEmptyUnexpectedAndMismatchedAssetsFailClosed() {
        expectCode("missingApk") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets().drop(1).filterNot { it.name.endsWith(".apk") })
        }
        expectCode("duplicateAssets") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets() + assets().first())
        }
        expectCode("emptyAsset") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets().map { if (it.name.endsWith(".apk")) it.copy(size = 0) else it })
        }
        expectCode("unexpectedAssets") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets().dropLast(1) + AndroidReleaseAsset("other.txt", 1, "$OFFICIAL_RELEASE_DOWNLOAD_PREFIX/v1.1.0/other.txt"))
        }
        expectCode("releaseMismatch") {
            selectAndroidReleaseAssets(release(), "v1.2.0", false, false, assets())
        }
        expectCode("untrustedAssetUrl") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets().map { if (it.name.endsWith(".apk")) it.copy(downloadUrl = "https://example.com/app.apk") else it })
        }
        expectCode("releaseNotStable") {
            selectAndroidReleaseAssets(release(), "v1.1.0", false, false, assets(), publishedAt = null)
        }
    }

    @Test
    fun checksumManifestRequiresExactPayloadEntries() {
        val manifest = fixtureManifest()
        val parsed = parseChecksumManifest(manifest, "palomar-v1.1.0.apk", "palomar-linux-v1.1.0.tar.gz")
        assertEquals("a".repeat(64), parsed["palomar-v1.1.0.apk"])
        expectCode("duplicateChecksum") {
            val duplicate = "${"a".repeat(64)}  palomar-v1.1.0.apk\n".repeat(2).toByteArray()
            parseChecksumManifest(duplicate, "palomar-v1.1.0.apk", "palomar-linux-v1.1.0.tar.gz")
        }
        expectCode("checksumAssetMismatch") {
            val unexpected =
                "${"a".repeat(64)}  palomar-v1.1.0.apk\n" +
                    "${"b".repeat(64)}  unexpected.tar.gz\n"
            parseChecksumManifest(unexpected.toByteArray(), "palomar-v1.1.0.apk", "palomar-linux-v1.1.0.tar.gz")
        }
    }

    @Test
    fun signedManifestMatchesPinnedAndInstalledCertificate() {
        val verified =
            verifySignedReleaseManifest(
                certificateBytes = decode(CERTIFICATE),
                signatureBytes = decode(SIGNATURE),
                manifestBytes = fixtureManifest(),
                expectedCertificateFingerprint = CERTIFICATE_SHA256,
                installedCertificateFingerprint = CERTIFICATE_SHA256,
                expectedApkName = "palomar-v1.1.0.apk",
                expectedArchiveName = "palomar-linux-v1.1.0.tar.gz",
            )
        assertEquals(CERTIFICATE_SHA256, verified.certificateFingerprint)
        expectCode("releaseCertificateMismatch") {
            verifySignedReleaseManifest(
                decode(CERTIFICATE), decode(SIGNATURE), fixtureManifest(), "0".repeat(64),
                CERTIFICATE_SHA256, "palomar-v1.1.0.apk", "palomar-linux-v1.1.0.tar.gz",
            )
        }
        expectCode("manifestSignatureMismatch") {
            verifySignedReleaseManifest(
                decode(CERTIFICATE), decode(SIGNATURE), fixtureManifest() + byteArrayOf(1), CERTIFICATE_SHA256,
                CERTIFICATE_SHA256, "palomar-v1.1.0.apk", "palomar-linux-v1.1.0.tar.gz",
            )
        }
    }

    @Test
    fun apkPackageSignerAndBothVersionsMustMatchAndAdvance() {
        val installed = identity("1.0.3", 10)
        validateDownloadedApkIdentity(installed, identity("1.1.0", 11), "1.1.0", CERTIFICATE_SHA256)
        expectCode("sameVersion") {
            validateDownloadedApkIdentity(installed, identity("1.0.3", 10), "1.0.3", CERTIFICATE_SHA256)
        }
        expectCode("downgrade") {
            validateDownloadedApkIdentity(installed, identity("1.0.2", 9), "1.0.2", CERTIFICATE_SHA256)
        }
        expectCode("apkVersionMismatch") {
            validateDownloadedApkIdentity(installed, identity("1.1.1", 11), "1.1.0", CERTIFICATE_SHA256)
        }
        expectCode("apkSignerMismatch") {
            validateDownloadedApkIdentity(installed, identity("1.1.0", 11, "f".repeat(64)), "1.1.0", CERTIFICATE_SHA256)
        }
        expectCode("apkSignerMismatch") {
            validateDownloadedApkIdentity(installed, identity("1.1.0", 11, ""), "1.1.0", CERTIFICATE_SHA256)
        }
        expectCode("packageMismatch") {
            validateDownloadedApkIdentity(installed, identity("1.1.0", 11).copy(packageName = "invalid.app"), "1.1.0", CERTIFICATE_SHA256)
        }
    }

    @Test
    fun downloadTransitionsSupportRetryAndCancellation() {
        assertEquals(ApkUpdatePhase.Discovering, nextApkDownloadPhase(ApkUpdatePhase.Idle, ApkDownloadEvent.Start))
        assertEquals(ApkUpdatePhase.Downloading, nextApkDownloadPhase(ApkUpdatePhase.Discovering, ApkDownloadEvent.MetadataSelected))
        assertEquals(ApkUpdatePhase.Interrupted, nextApkDownloadPhase(ApkUpdatePhase.Downloading, ApkDownloadEvent.Interrupted))
        assertEquals(ApkUpdatePhase.Discovering, nextApkDownloadPhase(ApkUpdatePhase.Interrupted, ApkDownloadEvent.Retry))
        assertEquals(ApkUpdatePhase.Canceled, nextApkDownloadPhase(ApkUpdatePhase.Downloading, ApkDownloadEvent.Cancel))
        assertNull(nextApkDownloadPhase(ApkUpdatePhase.Ready, ApkDownloadEvent.Start))
    }

    @Test
    fun unknownAppPermissionTransitionsOnlyAfterVerifiedUpdate() {
        assertEquals(ApkUpdatePhase.Failed, installerPhaseAfterRequest(false, true))
        assertEquals(ApkUpdatePhase.ExplainingPermission, installerPhaseAfterRequest(true, true))
        assertEquals(ApkUpdatePhase.AwaitingInstaller, installerPhaseAfterRequest(true, false))
        assertEquals(ApkUpdatePhase.AwaitingInstaller, installerPhaseAfterPermission(true))
        assertEquals(ApkUpdatePhase.Ready, installerPhaseAfterPermission(false))
    }

    @Test
    fun concurrentDownloadsAndInstallerLaunchesAreSuppressed() {
        val gate = ApkUpdateConcurrencyGate()
        assertTrue(gate.claimDownload())
        assertFalse(gate.claimDownload())
        gate.releaseDownload()
        assertTrue(gate.claimDownload())
        assertTrue(gate.claimInstaller())
        assertFalse(gate.claimInstaller())
        gate.releaseInstaller()
        assertTrue(gate.claimInstaller())
    }

    @Test
    fun pendingVerifiedUpdateRestoresAcrossRecreationAndReconcilesReplacement() {
        assertEquals(
            ApkUpdatePhase.Ready,
            restoredApkUpdatePhase(ApkUpdatePhase.ExplainingPermission, true, 10, 11),
        )
        assertEquals(
            ApkUpdatePhase.AwaitingInstaller,
            restoredApkUpdatePhase(ApkUpdatePhase.AwaitingInstaller, true, 10, 11),
        )
        assertEquals(
            ApkUpdatePhase.Interrupted,
            restoredApkUpdatePhase(ApkUpdatePhase.Downloading, false, 10, null),
        )
        assertEquals(
            ApkUpdatePhase.Completed,
            restoredApkUpdatePhase(ApkUpdatePhase.AwaitingInstaller, true, 11, 11),
        )
        assertEquals(
            ApkUpdatePhase.Canceled,
            restoredApkUpdatePhase(ApkUpdatePhase.Canceled, false, 10, null),
        )
    }

    private fun identity(version: String, code: Long, signer: String = CERTIFICATE_SHA256) =
        InstalledApkIdentity(PALOMAR_APPLICATION_ID, version, code, signer)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
    private fun fixtureManifest(): ByteArray = decode(MANIFEST)

    companion object {
        private const val CERTIFICATE_SHA256 = "f14057df5a434bf31f275786b34778e633290a488cabf74226b377611d3670e9"
        private const val CERTIFICATE = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURIekNDQWdlZ0F3SUJBZ0lVY0srM1JlazJPaFJiS1RwenJka3pFVXFSOUR3d0RRWUpLb1pJaHZjTkFRRUwKQlFBd0h6RWRNQnNHQTFVRUF3d1VVR0ZzYjIxaGNpQlVaWE4wSUZKbGJHVmhjMlV3SGhjTk1qWXdPVEV4TVRZMQpOakEwV2hjTk16WXdPVEE0TVRZMU5qQTBXakFmTVIwd0d3WURWUVFEREJSUVlXeHZiV0Z5SUZSbGMzUWdVbVZzClpXRnpaVENDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFEZ2dFUEFEQ0NBUW9DZ2dFQkFMZVhjWUVBSENBQnlQaE8KRDJBZVJpMDFSejIwTGFZUEFjb2cyVzFvVmpLMStHZDhwbDZ5UlBLNi9NZ1d4b3JzOEFjT0JlOGFZY0FnUGU3RwpocmRxclp1aXVTUDJBQWFjdE54enF1a09mODRQVU9Sajk5eEZTemF3VkUrbGpXYnUwRUZpSlRYN3NteHRqSThFCmZhOUU1aVM3V0R0ZDh5aWRtbkprN0hJdFFEY0xlSEs1VERSRkR1aG16b3RuajQzcFhTSjhNYm52L2xXQXJOVFYKOGxHMlJRSDV0OWFPZWx0Z254MmdRM1pNOWNXRWV0OTJXazVpTzFQbkVWTDBNdGR6eFc4UDI0cDhnb01Na3VreAovVmUzY1ZYQmk2aGhvNGs1M2VKRlJmNlcyRDhsOE9LaUd3RkIyVTl2amhteUE5bUVLRE81VWM4cHFFbXh3Vm5iCnR5YmZBMzhDQXdFQUFhTlRNRkV3SFFZRFZSME9CQllFRk5tT2ZOTUVwVHhnVXREMXZvWGRQY0owZGJVVk1COEcKQTFVZEl3UVlNQmFBRk5tT2ZOTUVwVHhnVXREMXZvWGRQY0owZGJVVk1BOEdBMVVkRXdFQi93UUZNQU1CQWY4dwpEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBS3R4Qlk3UzNLaWZYM3dlci9tQlhULytFNVFWL05LclJmZDBaRFZkCmdqME5TT05aTklLeFhXNEJZYXd1cHVhYjBwNmIwNWdoaTZmdjZOR3Q2YUxpNHJJa2tSRURIcDRmUzJLSU9nZHIKaUlVRzVNaEVVY1FJbG5TN3JnMFd5bFZRNmlhdHVlV0k5Q095b20yblZ2NmdJc3JzRVIzOUpyNVpRL3BzYzdFSwpGMnBOa2pPdnBkWkFDTUN3eW5nbStnT0psc3o1VE5vQ2h2Q2hpQkc4U051WStXY1RzdG02TEZmVDcyNjNsdUJECmE1UVQ5SGN4UWNOMkd3V0FLUS9HL0NheUdXa2YyQ2psaXFEZ21KeHdkSXNubjdhcWhpa0daVGV6M3BOdGw1NU4KZnA3Q216SzhtTzRqR0V2WEllenJOZjduSUFnMGhBQlY0dUFOVitzQWxhWmQyRmM9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"
        private const val SIGNATURE = "FKP310zihLpBIM7XCGd7g2VYh0OI0emtv5ugQK/l+fgWzuCFCz4IXYIPfrZ8Utrt3hHNY8enadEDb/kNM3ZwMUIkZ5Q+Idjwt9f/WzXDzjFglR9Z7rZa2X8kH7PpbXJlqUeAAYm1j8ykH0uSpXfM0S53Aj6GQdqsBxFEnT5YP+sIXqRowN7AJwiakms6Zjj29aZAw8An4ZONqT0kDH3YbX5BEqM+xF/bIbFYXv8lluEO0lQ58qg5pOD+VSe3PHE0xLAXhj/IsUx58gRXZd/QOFCiReL2E0BeHNW6jgAfO6j3fVLYXtDogVGInYBHGHJTClhtyGX9/VM2YQ3PSo99yw=="
        private const val MANIFEST = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYSAgcGFsb21hci12MS4xLjAuYXBrCmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmIgIHBhbG9tYXItbGludXgtdjEuMS4wLnRhci5nego="
    }
}
