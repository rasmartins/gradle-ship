##################################################################################################
# Configuration                                                                                  #
##################################################################################################

# Product name.
!define PRODUCT                "@PRODUCT_ID@"
# Product version.
!define VERSION                "@PRODUCT_VERSION@"
# Architecture.
!define ARCH                   "@ARCH@"
# License file.
!define LICENSE                "@LICENSE@"
# Executable name.
!define EXECUTABLE             "@EXE@"
# Distribution folder.
!define DISTRIBUTION           "@DISTRIBUTION@"
# Installer icon.
!define MUI_ICON               "@ICON@"
# Uninstaller icon.
!define MUI_UNICON             "@ICON@"
# Enable header image.
!define MUI_HEADERIMAGE
# Header image.
!define MUI_HEADERIMAGE_BITMAP "@HEADER@"
# Windows register folder
!define HKLM_FOLDER            "Software\Microsoft\Windows\CurrentVersion\Uninstall"
# Output file.
OutFile                        "@OUTFILE@"
# License file.
LicenseData                    "${LICENSE}"
# Canonical product name.
Name                           "${PRODUCT} ${VERSION}"
# Install folder.
InstallDir                     "$PROGRAMFILES\${PRODUCT}"
# Compression method.
SetCompressor                  /SOLID bzip2
# Perform a self CRC.
CRCCheck                       On

###################################################################################################
## Pages                                                                                          #
###################################################################################################

; Modern UI.
!include "MUI2.nsh"

# Installer
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE   "${LICENSE}"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Uninstaller
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

# Installer languages.
!insertmacro MUI_LANGUAGE       "English"

###################################################################################################
## Installer                                                                                      #
###################################################################################################

Section "Install"
  SetOutPath "$INSTDIR"

  # Copy files.
  File /r "${DISTRIBUTION}/*"

  # Desktop shortcut.
  CreateShortCut "$DESKTOP\${PRODUCT}.lnk" "$INSTDIR\${EXECUTABLE}" ""

  # Start menu folder.
  CreateDirectory "$SMPROGRAMS\${PRODUCT}"
  # Application shortcut.
  CreateShortCut "$SMPROGRAMS\${PRODUCT}\${PRODUCT}.lnk" "$INSTDIR\${EXECUTABLE}" "" "$INSTDIR\${EXECUTABLE}" 0
  # Uninstall shortcut.
  CreateShortCut "$SMPROGRAMS\${PRODUCT}\Uninstall.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0

  WriteRegStr HKLM "${HKLM_FOLDER}\${PRODUCT}" "DisplayName" "${PRODUCT} (remove only)"
  WriteRegStr HKLM "${HKLM_FOLDER}\${PRODUCT}" "UninstallString" "$INSTDIR\Uninstall.exe"

  WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

###################################################################################################
## Uninstaller                                                                                    #
###################################################################################################

Section un."Uninstall"
  ; Delete Files
  RMDir /r "$INSTDIR\*.*"

  ; Remove the installation directory
  RMDir "$INSTDIR"

  ; Delete Start Menu Shortcuts
  Delete "$DESKTOP\${PRODUCT}.lnk"
  Delete "$SMPROGRAMS\${PRODUCT}\*.*"
  RmDir  "$SMPROGRAMS\${PRODUCT}"

  ; Delete Uninstaller And Unistall Registry Entries
  DeleteRegKey HKLM "Software\${PRODUCT}"
  DeleteRegKey HKLM "${HKLM_FOLDER}\${PRODUCT}"
SectionEnd
