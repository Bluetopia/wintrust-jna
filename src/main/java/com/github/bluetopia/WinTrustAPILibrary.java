package com.github.bluetopia;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

interface WinTrustAPILibrary extends StdCallLibrary {

    /*
     * I was unable to find a Microsoft source for the GUID for WINTRUST_ACTION_GENERIC_VERIFY_V2, but the GUID
     * from https://github.com/tpn/winsdk-10/blob/master/Include/10.0.10240.0/um/SoftPub.h appears to work.
     *
     * WINTRUST_ACTION_GENERIC_VERIFY_V2 Guid  (Authenticode)
     * ----------------------------------------------------------------------------
     *  Assigned to the pgActionID parameter of WinVerifyTrust to verify the
     *  authenticity of a file/object using the Microsoft Authenticode
     *  Policy Provider,
     *
     *          {00AAC56B-CD44-11d0-8CC2-00C04FC295EE}
     *
     * #define WINTRUST_ACTION_GENERIC_VERIFY_V2
     *      { 0xaac56b,
     *      0xcd44,
     *      0x11d0,
     *      { 0x8c, 0xc2, 0x0, 0xc0, 0x4f, 0xc2, 0x95, 0xee }
     * }
     */
    GUID WINTRUST_ACTION_GENERIC_VERIFY_V2 = new GUID(0xaac56b, 0xcd44, 0x11d0, new char[] {0x8c, 0xc2, 0x0, 0xc0, 0x4f, 0xc2, 0x95, 0xee});

    WinTrustAPILibrary INSTANCE = Native.load("wintrust", WinTrustAPILibrary.class, W32APIOptions.DEFAULT_OPTIONS);

    NativeLong WinVerifyTrust(WinDef.HWND hwnd, GUID pgActionID, WINTRUST_DATA pWVTData);

    // Referenced structures

    /**
     * Data structure representing a Windows GUID.
     *
     * Source: https://learn.microsoft.com/en-us/windows/win32/api/guiddef/ns-guiddef-guid
     *
     * Definition:
     *   typedef struct _GUID {
     *      unsigned long  Data1;
     *      unsigned short Data2;
     *      unsigned short Data3;
     *      unsigned char  Data4[8];
     *   } GUID;
     */
    @Structure.FieldOrder({"data1", "data2", "data3", "data4"})
    class GUID extends Structure {
        GUID() {
            super();
        }

        GUID(long d1, long d2, long d3, char[] d4) {
            data1 = new WinDef.ULONG(d1);
            data2 = new WinDef.USHORT(d2);
            data3 = new WinDef.USHORT(d3);
            data4 = new WinDef.UCHAR[8];

            if (d4.length == 8) {
                for (int i = 0; i < d4.length; i++) {
                    data4[i] = new WinDef.UCHAR(d4[i]);
                }
            } else {
                // TODO: Invalid GUID
            }
        }

        public WinDef.ULONG data1;
        public WinDef.USHORT data2;
        public WinDef.USHORT data3;
        public WinDef.UCHAR[] data4;
    }

    /**
     * Class representing the data structure that is passed to the WinTrust API as a parameter.
     *
     * Note: I did not go through and build the full union in the Microsoft definition as 1) this is the first time I've
     * encountered a union, 2) this is the first time I've worked with JNA and 3) I didn't need it for what was
     * a quick proof of concept.
     *
     * Source: https://learn.microsoft.com/en-us/windows/win32/api/wintrust/ns-wintrust-wintrust_data
     *
     * Definition:
     *   typedef struct _WINTRUST_DATA {
     *      DWORD                               cbStruct;
     *      LPVOID                              pPolicyCallbackData;
     *      LPVOID                              pSIPClientData;
     *      DWORD                               dwUIChoice;
     *      DWORD                               fdwRevocationChecks;
     *      DWORD                               dwUnionChoice;
     *      union {
     *          #if ...
     *              WINTRUST_FILE_INFO_                *pFile;
     *          #else
     *              struct WINTRUST_FILE_INFO_         *pFile;
     *          #endif
     *          #if ...
     *              WINTRUST_CATALOG_INFO_             *pCatalog;
     *          #else
     *              struct WINTRUST_CATALOG_INFO_      *pCatalog;
     *          #endif
     *          #if ...
     *              WINTRUST_BLOB_INFO_                *pBlob;
     *          #else
     *              struct WINTRUST_BLOB_INFO_         *pBlob;
     *          #endif
     *          #if ...
     *              WINTRUST_SGNR_INFO_                *pSgnr;
     *          #else
     *              struct WINTRUST_SGNR_INFO_         *pSgnr;
     *          #endif
     *          #if ...
     *              WINTRUST_CERT_INFO_                *pCert;
     *          #else
     *              struct WINTRUST_CERT_INFO_         *pCert;
     *          #endif
     *          #if ...
     *              WINTRUST_DETACHED_SIG_INFO_        *pDetachedSig;
     *          #else
     *              struct WINTRUST_DETACHED_SIG_INFO_ *pDetachedSig;
     *          #endif
     *      };
     *      DWORD                               dwStateAction;
     *      HANDLE                              hWVTStateData;
     *      WCHAR                               *pwszURLReference;
     *      DWORD                               dwProvFlags;
     *      DWORD                               dwUIContext;
     *      struct WINTRUST_SIGNATURE_SETTINGS_ *pSignatureSettings;
     *  } WINTRUST_DATA, *PWINTRUST_DATA;
     */
    @Structure.FieldOrder({"cbStruct", "pPolicyCallbackData", "pSIPClientData", "dwUIChoice", "fdwRevocationChecks", "dwUnionChoice", "pFile", "dwStateAction",
            "hWVTStateData", "pwszURLReference", "dwProvFlags", "dwUIContext", "pSignatureSettings"})
    class WINTRUST_DATA extends Structure {

        final int WTD_UI_NONE = 2;
        final int WTD_REVOKE_NONE = 0;
        final int WTD_CHOICE_FILE = 1;
        final int WTD_STATEACTION_VERIFY = 0x00000001;

        public WINTRUST_DATA() {
            super();
        }

        /**
         * Constructs a WINTRUST_DATA "struct" with the specified filename.
         *
         * Other initialization parameters in WINTRUST_DATA are copied from the example program linked in comment below.
         *
         * @param pathToFile File to reference in the data structure.
         */
        public WINTRUST_DATA(String pathToFile) {
            this();

            // Settings from the example (https://learn.microsoft.com/en-us/windows/win32/seccrypto/example-c-program--verifying-the-signature-of-a-pe-file)
            // Use default code signing EKU
            pPolicyCallbackData = Pointer.NULL;
            // No data to pass to SIP
            pSIPClientData = Pointer.NULL;
            // Disable WVT UI
            dwUIChoice = WTD_UI_NONE;
            // No revocation checking
            fdwRevocationChecks = WTD_REVOKE_NONE;
            // Verify embedded signature on a file
            dwUnionChoice = WTD_CHOICE_FILE;
            // Verify action
            dwStateAction = WTD_STATEACTION_VERIFY;
            // Verification sets hWVTStateData
            hWVTStateData = Pointer.NULL;
            // Not used
            pwszURLReference = Pointer.NULL;
            // Not applicable if no UI
            dwUIContext = 0;

            // Setting up the WINTRUST_FILE_INFO
            pFile = new WINTRUST_FILE_INFO.ByReference();
            pFile.init(new WString(pathToFile));
            cbStruct = size();
        }

        public int cbStruct;
        public Pointer pPolicyCallbackData = null;
        public Pointer pSIPClientData = null;
        public int dwUIChoice = 2;      // WTD_NONE = 2
        public int fdwRevocationChecks = 0; // WTD_REVOKE_NONE = 0
        public int dwUnionChoice = 1;  // WTD_CHOICE_FILE = 1
        public WINTRUST_FILE_INFO.ByReference pFile;
        public int dwStateAction;
        public Pointer hWVTStateData;
        public Pointer pwszURLReference;
        public int dwProvFlags;
        public int dwUIContext;
        public Pointer pSignatureSettings = Pointer.NULL;

    }

    /* Source:
    */

    /**
     * WINTRUST_FILE_INFO sub-structure used for storing file information in the WINTRUST_DATA structure.
     *
     * Only really needed the pcwszFilePath property here; everything else is nulled/default.
     *
     * Source: https://learn.microsoft.com/en-us/windows/win32/api/wintrust/ns-wintrust-wintrust_file_info
     *
     * Definition:
     *   typedef struct WINTRUST_FILE_INFO_ {
     *      DWORD   cbStruct;
     *      LPCWSTR pcwszFilePath;
     *      HANDLE  hFile;
     *      GUID    *pgKnownSubject;
     *   } WINTRUST_FILE_INFO, *PWINTRUST_FILE_INFO;
     */
    @Structure.FieldOrder({"cbStruct", "pcwszFilePath", "hFile", "pgKnownSubject"})
    class WINTRUST_FILE_INFO extends Structure {
        public static class ByReference extends WINTRUST_FILE_INFO implements Structure.ByReference { }

        public WINTRUST_FILE_INFO() {
            super();
        }

        public void init(WString file) {
            pcwszFilePath = file;
            hFile = Pointer.NULL;
            pgKnownSubject = Pointer.NULL;
            cbStruct = size();
        }

        public int cbStruct;
        public WString pcwszFilePath;
        public Pointer hFile = Pointer.NULL;
        public Pointer pgKnownSubject;
    }
}
