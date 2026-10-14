; Inno Setup script for the Specman Windows installer. Built via "mvn -P windows-installer clean package",
; which passes the following defines (see the windows-installer profile in pom.xml):
;   AppVersion, AppId, AppPublisher, SourceDir (jpackage app image), ExtraDir (grammar and reference model), OutDir, ProjectDir
#define AppName "Specman"
#define AppExe "Specman.exe"

[Setup]
; The doubled opening brace is Inno's escape for a literal "{" in front of the GUID
AppId={{{#AppId}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
; System-wide by default (needs admin rights); the "install for all users / only for me" dialog lets
; the user switch to a per-user install. HKA, {autodesktop} and {group} follow that choice.
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
DefaultDirName={code:DefaultDir}
; Always show the directory page; by default Inno skips it when it finds an existing installation
; (then it silently reuses that directory). On upgrades the page is prefilled with the existing directory.
DisableDirPage=no
DefaultGroupName={#AppName}
SetupIconFile={#ProjectDir}\src\main\resources\images\specman.ico
; Logo images for 100%..250% display scaling (sizes: see howto/howto-windows-installer.adoc, "Branding")
WizardImageFile={#ProjectDir}\src\main\inno\wizard-large-100.bmp,{#ProjectDir}\src\main\inno\wizard-large-125.bmp,{#ProjectDir}\src\main\inno\wizard-large-150.bmp,{#ProjectDir}\src\main\inno\wizard-large-175.bmp,{#ProjectDir}\src\main\inno\wizard-large-200.bmp,{#ProjectDir}\src\main\inno\wizard-large-225.bmp,{#ProjectDir}\src\main\inno\wizard-large-250.bmp
WizardSmallImageFile={#ProjectDir}\src\main\inno\wizard-small-100.bmp,{#ProjectDir}\src\main\inno\wizard-small-125.bmp,{#ProjectDir}\src\main\inno\wizard-small-150.bmp,{#ProjectDir}\src\main\inno\wizard-small-175.bmp,{#ProjectDir}\src\main\inno\wizard-small-200.bmp,{#ProjectDir}\src\main\inno\wizard-small-225.bmp,{#ProjectDir}\src\main\inno\wizard-small-250.bmp
UninstallDisplayIcon={app}\{#AppExe}
OutputDir={#OutDir}
OutputBaseFilename=specman-{#AppVersion}-windows-setup
Compression=lzma2
SolidCompression=yes
ChangesEnvironment=yes
ChangesAssociations=yes
ArchitecturesInstallIn64BitMode=x64compatible
; Replaces a running Specman instead of failing on locked files
CloseApplications=yes

[Languages]
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

; This file is UTF-8 with BOM (Inno Setup reads it as ANSI otherwise), so umlauts are fine
[CustomMessages]
english.AppRunning=Specman is still running. Please close it and click Retry, or click Cancel to abort the uninstallation.
german.AppRunning=Specman wird noch ausgeführt. Bitte beenden und "Wiederholen" wählen, oder "Abbrechen", um die Deinstallation abzubrechen.

; app\ and runtime\ are generated completely by jpackage. The jar name contains the version, so without
; this an upgrade would leave the previous version's jar behind (and stale runtime files on a JDK change).
; Same for the reference model, whose name contains the model version (e.g. specman-reference-v1.3-model-v2.nsd).
[InstallDelete]
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"
Type: files; Name: "{app}\specman-reference-v*.nsd"
Type: files; Name: "{app}\SpecmanModel_V*.g4"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion
; Grammar and reference model (needed for AI-assisted spec authoring) directly in the installation
; directory, at the same place as in the portable release bundle
Source: "{#ExtraDir}\*"; DestDir: "{app}"; Flags: ignoreversion
; Forwarder that puts "specman" on the PATH; lives in its own bin directory so it can't clash with
; Specman.exe (Windows filenames are case-insensitive)
Source: "{#ProjectDir}\src\main\inno\specman.cmd"; DestDir: "{app}\bin"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Registry]
Root: HKA; Subkey: "Software\Classes\.nsd"; ValueType: string; ValueData: "Specman.nsd"; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\.nsd"; ValueName: "Content Type"; ValueType: string; ValueData: "application/x-specman-nsd"; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\Specman.nsd"; ValueType: string; ValueData: "Specman Diagramm"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\Specman.nsd\DefaultIcon"; ValueType: string; ValueData: "{app}\{#AppExe},0"
Root: HKA; Subkey: "Software\Classes\Specman.nsd\shell\open\command"; ValueType: string; ValueData: """{app}\{#AppExe}"" ""%1"""
; PATH entry: user environment (HKCU) for a per-user install, machine environment (HKLM) for a system-wide one
Root: HKA; Subkey: "{code:EnvSubkey}"; ValueName: "Path"; ValueType: expandsz; ValueData: "{olddata};{app}\bin"; Check: NeedsPath(ExpandConstant('{app}\bin'))

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[Code]
// %LOCALAPPDATA%\Specman for a per-user install, %ProgramFiles%\Specman for a system-wide one
function DefaultDir(Param: string): string;
begin
  if IsAdminInstallMode then
    Result := ExpandConstant('{autopf}\{#AppName}')
  else
    Result := ExpandConstant('{localappdata}\{#AppName}');
end;

function EnvSubkey(Param: string): string;
begin
  if IsAdminInstallMode then
    Result := 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
  else
    Result := 'Environment';
end;

function NeedsPath(Dir: string): Boolean;
var
  OldPath: string;
begin
  if not RegQueryStringValue(HKA, EnvSubkey(''), 'Path', OldPath) then
  begin
    Result := True;
    exit;
  end;
  Result := Pos(';' + Uppercase(Dir) + ';', ';' + Uppercase(OldPath) + ';') = 0;
end;

procedure RemoveFromPath(Dir: string);
var
  OldPath, NewPath: string;
  P: Integer;
begin
  if not RegQueryStringValue(HKA, EnvSubkey(''), 'Path', OldPath) then
    exit;
  NewPath := ';' + OldPath + ';';
  P := Pos(';' + Uppercase(Dir) + ';', Uppercase(NewPath));
  if P = 0 then
    exit;
  Delete(NewPath, P, Length(Dir) + 1);
  NewPath := Copy(NewPath, 2, Length(NewPath) - 2);
  RegWriteExpandStringValue(HKA, EnvSubkey(''), 'Path', NewPath);
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
    RemoveFromPath(ExpandConstant('{app}\bin'));
end;

// CloseApplications only covers the installer. Without this check the uninstaller silently skips the
// locked Specman.exe, removes everything else and leaves a half-installed directory behind.
function IsProcessRunning(ImageName: string): Boolean;
var
  ResultCode: Integer;
begin
  // Full paths: a "find" from e.g. Git for Windows could otherwise shadow the Windows one on the PATH.
  // The extra outer quotes are cmd's rule for /C command lines that contain several quoted parts.
  Result := Exec(ExpandConstant('{cmd}'),
    '/C ""' + ExpandConstant('{sys}\tasklist.exe') + '" /NH /FI "IMAGENAME eq ' + ImageName + '" | "' +
    ExpandConstant('{sys}\find.exe') + '" /I "' + ImageName + '""',
    '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
end;

function InitializeUninstall(): Boolean;
begin
  Result := True;
  while IsProcessRunning('{#AppExe}') or IsProcessRunning('specman-cli.exe') do
  begin
    // Silent uninstalls can't ask: SuppressibleMsgBox answers Cancel there instead of looping forever
    if SuppressibleMsgBox(ExpandConstant('{cm:AppRunning}'), mbError, MB_RETRYCANCEL, IDCANCEL) = IDCANCEL then
    begin
      Result := False;
      exit;
    end;
  end;
end;
