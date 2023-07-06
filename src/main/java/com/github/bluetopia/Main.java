package com.github.bluetopia;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sun.jna.platform.win32.WinError.*;

public class Main {

    public static void main(String[] args) {

        // Path to a file to test.
        String pathToTestFile = "path/to/file.dll";

        // Prevent invalid memory access.
        Native.setProtected(true);

        WinTrustAPILibrary lib = WinTrustAPILibrary.INSTANCE;

        Path path = Paths.get(pathToTestFile);
        if (!Files.exists(path)) {
            System.out.println("Can't find " + path.toAbsolutePath());
        }

        WinTrustAPILibrary.WINTRUST_DATA dataTest = new WinTrustAPILibrary.WINTRUST_DATA(pathToTestFile);

        NativeLong result = lib.WinVerifyTrust(null, WinTrustAPILibrary.WINTRUST_ACTION_GENERIC_VERIFY_V2, dataTest);
        System.out.println(result + " - " + getStatusCodeMessage(result));
    }

    /**
     * Helper method to translate the return code from WinVerifyTrust.
     * Found out that com.sun.jna.platform.win32.WinError actually has many of these defined already. Neat.
     * Source: https://learn.microsoft.com/en-us/windows/win32/seccrypto/certificate-and-trust-return-values
     *
     * @param value Return code from WinVerifyTrust
     * @return String containing a message indicating what the problem was.
     */
    public static String getStatusCodeMessage(NativeLong value) {
        switch (value.intValue()) {
            case TRUST_E_SYSTEM_ERROR:
                return "A system-level error occurred while verifying trust.";

            case TRUST_E_SUBJECT_NOT_TRUSTED:
                return "The signature is present, but is not trusted.";

            case TRUST_E_EXPLICIT_DISTRUST:
                return "A signature was present, but is explicitly disallowed.";

            case TRUST_E_NOSIGNATURE:
                // The C example indicates that it may be possible to get further information from the GetLastError()
                // method from the error handling API, but I did not go as far as instrumenting it for JNA.
                // Specifically, TRUST_E_SUBJECT_FORM_UNKNOWN or TRUST_E_PROVIDER_UNKNOWN might be returned.
                return "The file was not signed, or had a signature that was not valid.";

            case CRYPT_E_SECURITY_SETTINGS:
                return "The hash representing the subject or publisher isn't explicitly trusted by the admin and admin policy disables user trust.";

            default:
                return "Unknown - " + value.intValue();
        }

    }

}
