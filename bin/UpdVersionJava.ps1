param (
    [string]$startDir = 'no',
    [string]$ChangePOM = $false
    )
# ---------------------------------------------------
# il file  dovrebbe essere un Hard Link 
# con     \java\photon2\stdcla\bin\UpdVersionJava.ps1 
#    <--> \java\bin\UpdVersionJava.ps1 
# Verificare con:
# 	"fsutil.exe hardlink list UpdVersionJava.ps1"
# ----------- Settaggi Iniziali --------------------    
Set-StrictMode -Version 3.0
Set-Location (Split-Path $PSCommandPath)

$script:fileProperties = "updVersionJava.properties"
$script:szStartDir = $null
$script:bChangePomFile = $false
$script:szPomFile = $null

$Script:szVersionFile = $null
$Script:szVersionVal = $null
$Script:szVersion = $null
$script:batchMode = $false
try {
if ( $null -ne $ChangePOM ) {
        # [bool]::TryParse($ChangePOM, $script:bChangePomFile)
        switch ($ChangePOM) {
            1       { $script:bChangePomFile = $true}
            '1'     { $script:bChangePomFile = $true}
            'true'  { $script:bChangePomFile = $true}
        }
    }
} catch {
    ##
}
if ( Test-Path $script:fileProperties) {
  $script:appProps = ConvertFrom-StringData(Get-Content $script:fileProperties -raw)
  $script:szStartDir = $appProps.startDir
}


function checkBottone() {
    # $szMsg = "jar:`"{0}`" modn:`"{1}`"" -f $script:szStartDir, $Script:modName
    # Write-Host $szMsg
    $bena = $null -ne $script:szStartDir -and $script:szStartDir.Length -gt 5
    $btAnalizza.Enabled = $bena
    $bena = $null -ne $Script:szVersionFile -and $Script:szVersionFile.Length -gt 5
    $btUpdVers.Enabled = $bena
}

function cercaStartDir() {
    if ( $script:szStartDir -ne $null) {
      $props = @{ InitialDirectory = $script:szStartDir } 
    } else {
      $props = @{ InitialDirectory = [Environment]::GetFolderPath('Desktop') } 
    }
    $dirBrowser = New-Object System.Windows.Forms.FolderBrowserDialog -Property $props
    $null = $dirBrowser.ShowDialog()
    $szFile = $dirBrowser.SelectedPath
    $propData = @'
startDir={0}    
'@ -f $szFile
    $propData -replace '\\',"\\" | Set-Content -Path $script:fileProperties -Force
    # Write-Host ("File=" + $szFile)
    return $szFile
}

function cercaVersionFile() {
    # Write-Host "CercaVersionFile()"
    $sz = Get-ChildItem -Path $script:szStartDir -Filter Versione.java -Recurse
    if ( $script:bChangePomFile ) {
      $script:szPomFile = Get-ChildItem -Path $script:szStartDir -Filter pom.xml
    }
    return $sz
}
function convPomVersion {
    $sz = "_" + $(Get-Date -f "yyyyMMdd-HHmmss") + ".bak"
    $pomBak = $script:szPomFile.FullName -replace ".xml",$sz
    Copy-Item -Path $script:szPomFile -Destination $pomBak -Force 
    $bDoit=$true
    Get-Content -Path $pomBak | ForEach-Object {
        if ( $bDoit ) {
            $n = $_.IndexOf("<version>")
            if ( $n -ge 0 ) {
                $n += "<version>".Length
                $_.Substring(0,$n) + $Script:szVersion + "</version>"
                $bDoit = $false
            } else {
                $_
            }
        } else {
            $_
        }
     } | Out-File -FilePath $script:szPomFile -Encoding utf8 -Force
    Write-Host ("Cambiato pom.xml versione {0}" -f $Script:szVersion ) -ForegroundColor Green
}
function updVersInClass() {
    $szOggi   = Get-Date -f "yyyy-MM-dd_HH-mm-ss"
    $szOggiDs = Get-Date -f "dd/MM/yyyy HH:mm:ss"
    $Script:szVersionVal = "Date " + $szOggiDs
    $tempFile = "{0}\Versione_{1}_java.bak" -f $Script:szVersionFile.Directory.FullName, $szOggi
    Copy-Item -Path $Script:szVersionFile.FullName -Destination $tempFile -Force
    $Script:fileNew = ""
    Get-Content -Path $tempFile -Encoding UTF8 |
    ForEach-Object {
        $sz = $_
        if ( $sz -match "[\t ]APP_MAX_VERSION[\t ]" ) {
            $n = $sz.IndexOf("=")
            $sz1 = ($sz.Substring($n+1) -replace ";","").Trim()
            $Script:szVersionVal += " V." + $sz1
            $Script:szVersion = $sz1
        }
        if ( $sz -match "[\t ]APP_MIN_VERSION[\t ]" ) {
            $n = $sz.IndexOf("=")
            $sz1 = ($sz.Substring($n+1) -replace ";","").Trim()
            $Script:szVersionVal += "." + $sz1
            $Script:szVersion += "." + $sz1
        }
        if ( $sz -match "[\t ]APP_BUILD[\t ]" ) {
            $n = $sz.IndexOf("=")
            $sz = $sz.Substring($n+1) -replace ";",""
            $n = [int]$sz + 1
            $sz = "  public static final int    APP_BUILD = {0};" -f $n
            $Script:szVersionVal += "." + $n
            $Script:szVersion += "." + $n
            # $sz
        }
        if ( $sz -match "[\t ]CSZ_DATEDEPLOY[\t ]" ) {
            $sz = "  public static final String CSZ_DATEDEPLOY = `"{0}`";" -f $szOggiDs
            # $sz
        }
        $Script:fileNew += $sz + "`n"
    }
    $Script:fileNew | Out-File -FilePath $Script:szVersionFile.FullName -Force | Out-Null
    if ( $script:batchMode) {
        Write-Host ( "Upgrade of {0}`n`twith {1}" -f $Script:szVersionFile.FullName, $Script:szVersionVal) -ForegroundColor Green
    }
    if ( $null -ne $script:szPomFile) {
        convPomVersion 
    }
}


if ( $null -ne $startDir -and $startDir.length -gt 3) {
    $script:batchMode = $true
    $script:szStartDir = $startDir
    $Script:szVersionFile = cercaVersionFile
    if ($null -ne $Script:szVersionFile) {
        updVersInClass
    }
    exit
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO
Add-Type -AssemblyName System.XML
Add-Type -AssemblyName PresentationFramework

$DebugPreference = "continue"
$ErrorActionPreference = "stop"
$WarningPreference = "stop"



$DesignCode = @"
/// <summary>
/// Variabile di progettazione necessaria.
/// </summary>
private System.ComponentModel.IContainer components = null;

/// <summary>
/// Pulire le risorse in uso.
/// </summary>
/// <param name="disposing">ha valore true se le risorse gestite devono essere eliminate, false in caso contrario.</param>
protected override void Dispose(bool disposing)
{
    if (disposing && (components != null))
    {
        components.Dispose();
    }
    base.Dispose(disposing);
}

#region Codice generato da Progettazione Windows Form

/// <summary>
/// Metodo necessario per il supporto della finestra di progettazione. Non modificare
/// il contenuto del metodo con l'editor di codice.
/// </summary>
private void InitializeComponent()
{
    this.panel1 = new System.Windows.Forms.Panel();
    this.label1 = new System.Windows.Forms.Label();
    this.txStartDir = new System.Windows.Forms.TextBox();
    this.btCerca = new System.Windows.Forms.Button();
    this.lbFileVers = new System.Windows.Forms.Label();
    this.btAnalizza = new System.Windows.Forms.Button();
    this.lbVersione = new System.Windows.Forms.Label();
    this.btUpdVers = new System.Windows.Forms.Button();
    this.panel1.SuspendLayout();
    this.SuspendLayout();
    // 
    // panel1
    // 
    this.panel1.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
    | System.Windows.Forms.AnchorStyles.Left) 
    | System.Windows.Forms.AnchorStyles.Right)));
    this.panel1.Controls.Add(this.btUpdVers);
    this.panel1.Controls.Add(this.lbVersione);
    this.panel1.Controls.Add(this.btAnalizza);
    this.panel1.Controls.Add(this.lbFileVers);
    this.panel1.Controls.Add(this.btCerca);
    this.panel1.Controls.Add(this.txStartDir);
    this.panel1.Controls.Add(this.label1);
    this.panel1.Location = new System.Drawing.Point(1, 2);
    this.panel1.Name = "panel1";
    this.panel1.Size = new System.Drawing.Size(550, 187);
    this.panel1.TabIndex = 0;
    // 
    // label1
    // 
    this.label1.AutoSize = true;
    this.label1.Location = new System.Drawing.Point(23, 23);
    this.label1.Name = "label1";
    this.label1.Size = new System.Drawing.Size(94, 13);
    this.label1.TabIndex = 0;
    this.label1.Text = "Direttorio Partenza";
    // 
    // txStartDir
    // 
    this.txStartDir.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
    | System.Windows.Forms.AnchorStyles.Right)));
    this.txStartDir.Location = new System.Drawing.Point(123, 20);
    this.txStartDir.Name = "txStartDir";
    this.txStartDir.Size = new System.Drawing.Size(311, 20);
    this.txStartDir.TabIndex = 1;
    // 
    // btCerca
    // 
    this.btCerca.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
    this.btCerca.Location = new System.Drawing.Point(451, 17);
    this.btCerca.Name = "btCerca";
    this.btCerca.Size = new System.Drawing.Size(81, 23);
    this.btCerca.TabIndex = 2;
    this.btCerca.Text = "Cerca...";
    this.btCerca.UseVisualStyleBackColor = true;
    // this.btCerca.Click += new System.EventHandler(this.btCerca_Click);
    // 
    // lbFileVers
    // 
    this.lbFileVers.AutoSize = true;
    this.lbFileVers.Location = new System.Drawing.Point(126, 50);
    this.lbFileVers.Name = "lbFileVers";
    this.lbFileVers.Size = new System.Drawing.Size(107, 13);
    this.lbFileVers.TabIndex = 3;
    this.lbFileVers.Text = "Il file versione trovato";
    // 
    // btAnalizza
    // 
    // this.btAnalizza.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
    this.btAnalizza.Location = new System.Drawing.Point(19, 45);
    this.btAnalizza.Name = "btAnalizza";
    this.btAnalizza.Size = new System.Drawing.Size(101, 23);
    this.btAnalizza.TabIndex = 4;
    this.btAnalizza.Text = "Analizza";
    this.btAnalizza.UseVisualStyleBackColor = true;
    // 
    // lbVersione
    // 
    this.lbVersione.AutoSize = true;
    this.lbVersione.Location = new System.Drawing.Point(126, 84);
    this.lbVersione.Name = "lbVersione";
    this.lbVersione.Size = new System.Drawing.Size(98, 13);
    this.lbVersione.TabIndex = 5;
    this.lbVersione.Text = "La versione trovata";
    // 
    // btUpdVers
    // 
    this.btUpdVers.Location = new System.Drawing.Point(19, 80);
    this.btUpdVers.Name = "btUpdVers";
    this.btUpdVers.Size = new System.Drawing.Size(101, 23);
    this.btUpdVers.TabIndex = 6;
    this.btUpdVers.Text = "Upd Versione";
    this.btUpdVers.UseVisualStyleBackColor = true;
    // 
    // Form1
    // 
    this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
    this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
    this.ClientSize = new System.Drawing.Size(551, 190);
    this.Controls.Add(this.panel1);
    this.Name = "Form1";
    this.Text = "Aggiorna la versione file Java";
    this.panel1.ResumeLayout(false);
    this.panel1.PerformLayout();
    this.ResumeLayout(false);

}

#endregion

private System.Windows.Forms.Panel panel1;
private System.Windows.Forms.Button btCerca;
private System.Windows.Forms.TextBox txStartDir;
private System.Windows.Forms.Label label1;
private System.Windows.Forms.Label lbVersione;
private System.Windows.Forms.Button btAnalizza;
private System.Windows.Forms.Label lbFileVers;
private System.Windows.Forms.Button btUpdVers;
"@

$InitCode = @"
	public frmUpdVersJavaClass()
	{
		InitializeComponent();
	}
"@

if ( -not ([System.Management.Automation.PSTypeName]'frmUpdVersJavaClass').Type) {
    $FormCode="public class frmUpdVersJavaClass : System.Windows.Forms.Form { "+$DesignCode+$InitCode+"}";	
    $Assembly = ( 
        "System.Windows.Forms",
        "System.Drawing",
        "System.Drawing.Primitives",
        "System.ComponentModel.Primitives"
    )
    # ### Form frmUpdVersJavaClass
    Add-Type -ReferencedAssemblies $Assembly -TypeDefinition $FormCode -Language CSharp  
}
$form = New-Object frmUpdVersJavaClass
$form.ClientSize = New-Object System.Drawing.Size(553, 164)
$form.Text = "Aumenta il valore di versione di un file JAVA"
# ### My data

$pan1 = $form.Controls[0]
$txStartDir = $pan1.Controls['txStartDir']
$lbFileVers = $pan1.Controls['lbFileVers']
$lbVersione  = $pan1.Controls['lbVersione']
$btCerca    = $pan1.Controls['btCerca']
$btAnalizza = $pan1.Controls['btAnalizza']
$btUpdVers  = $pan1.Controls['btUpdVers']
if ( $null -ne $script:szStartDir) {
    $txStartDir.Text = $script:szStartDir
    $btAnalizza.Enabled = $true
} else {
    $btAnalizza.Enabled = $false
}
$btUpdVers.Enabled  = $false 

$txStartDir.add_TextChanged({
    $sz = $txStartDir.Text
    $script:szStartDir = $sz
    checkBottone
})
$btCerca.Add_click({
    $sz = cercaStartDir
    $txStartDir.Text = $sz
    $script:szStartDir = $sz
    checkBottone
})
$btAnalizza.Add_click({
    $sz = cercaVersionFile
    $lbFileVers.Text = $sz
    $Script:szVersionFile = $sz
    checkBottone
})
$btUpdVers.Add_click({
    updVersInClass
    $lbVersione.Text = $Script:szVersionVal
})

# ### trasporto
$form.showDialog() | Out-Null

if ( $null -ne $form )  {
    $form.Close()
    $form.Dispose()
    $form = $null
}
