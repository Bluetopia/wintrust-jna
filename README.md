# wintrust-jna
Very rough implementation of the WinVerifyTrust function using JNA.  Not for production use.

# Description
This was a very rough proof of concept to find out whether I could use the WinTrust API via Java Native Access. It is 
basically a port of the example that Microsoft provides at https://learn.microsoft.com/en-us/windows/win32/seccrypto/example-c-program--verifying-the-signature-of-a-pe-file.

I have very little experience with Windows programming and prior to this effort, no experience with Java Native Access so the 
code may be sub-optimal or my port attempt may be inaccurate.  However, the kind folks on the jna mailing list suggested
that I put this up for others to build off of, so I'm doing so in thanks for their help. 

Ultimately, I was able to confirm that a Microsoft DLL was signed and trusted, and that an unsigned DLL was not signed using this code. 