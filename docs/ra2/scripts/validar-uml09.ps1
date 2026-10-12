param(
    [Parameter(Mandatory = $true)][string]$Java,
    [Parameter(Mandatory = $true)][string]$PlantUml,
    [string]$EvidenceDir = 'docs/ra2/evidencias/entrega09'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Push-Location $repo
try {
    New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
    $sources = @(
        'docs/ra2/DIAGRAMA_PACOTES.puml',
        'docs/ra2/DIAGRAMA_COMPONENTES.puml',
        'docs/ra2/DIAGRAMA_IMPLANTACAO.puml',
        'docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml'
    )
    $log = [System.Collections.Generic.List[string]]::new()
    $log.Add('Baseline documental: 80a36c457ec4e46865ee456f388cb607297c3811')
    $log.Add('Data/hora: ' + [DateTimeOffset]::Now.ToString('o'))
    $log.Add('java -jar plantuml -charset UTF-8 -checkonly <quatro fontes>')
    & $Java -jar $PlantUml -charset UTF-8 -checkonly @sources *> (Join-Path $EvidenceDir 'plantuml-check.txt')
    $log.Add("checkonly exit: $LASTEXITCODE")
    if ($LASTEXITCODE -ne 0) { throw 'Erro de sintaxe PlantUML' }
    $log.Add('java -jar plantuml -charset UTF-8 -tsvg <quatro fontes>')
    & $Java -jar $PlantUml -charset UTF-8 -tsvg @sources *> (Join-Path $EvidenceDir 'plantuml-render.txt')
    $log.Add("tsvg exit: $LASTEXITCODE")
    if ($LASTEXITCODE -ne 0) { throw 'Erro de renderização PlantUML' }

    foreach ($source in $sources) {
        $svg = [IO.Path]::ChangeExtension($source, '.svg')
        $content = Get-Content -Raw -LiteralPath $svg
        [xml]$xml = $content
        if ($xml.DocumentElement.LocalName -ne 'svg' -or
            $content -match '(?i)Syntax Error|An error has occurred|UnsupportedOperationException|java\.lang\.[A-Za-z]+Exception') {
            throw "SVG inválido/tela de erro: $svg"
        }
        $log.Add("$svg : XML SVG válido, $($xml.svg.width) x $($xml.svg.height), sem marcador de erro")
    }

    # Cria o próprio log antes de conferir o link para ele; finaliza após as checagens.
    $log | Set-Content -Encoding utf8 (Join-Path $EvidenceDir 'validacao-documental.txt')
    # Verifica somente referências locais nos documentos ativos desta entrega.
    $documents = @('README.md', 'docs/ra2/RELATORIO_TECNICO_RA2.md',
        'docs/ra2/ROTEIRO_APRESENTACAO.md', 'docs/ra2/VALIDACAO_CONFORMIDADE_AULAS_UML.md')
    $links = 0
    foreach ($document in $documents) {
        $parent = Split-Path $document
        foreach ($match in [regex]::Matches((Get-Content -Raw -LiteralPath $document), '\[[^\]]+\]\(([^)]+)\)')) {
            $target = $match.Groups[1].Value
            if ($target -match '^[a-zA-Z]+:|^#') { continue }
            $target = ($target -split '#')[0]
            $resolved = if ($parent) { Join-Path $parent $target } else { $target }
            if (-not (Test-Path -LiteralPath $resolved)) { throw "Link inexistente: $document -> $target" }
            $links++
        }
    }
    $log.Add("Links locais existentes: $links")
    $protected = @(git diff --name-only 80a36c457ec4e46865ee456f388cb607297c3811 -- src pom.xml .github)
    if ($LASTEXITCODE -ne 0 -or $protected.Count -ne 0) { throw 'Diff fora do escopo documental' }
    $log.Add('git diff baseline -- src pom.xml .github : vazio')
    $log.Add('README, matriz e evidências 08 preservados; conferir com git diff --name-only.')
    $log.Add('Inspeção visual humana/assistida é etapa separada: este script não prova legibilidade.')
    $log | Set-Content -Encoding utf8 (Join-Path $EvidenceDir 'validacao-documental.txt')
    $log
} finally {
    Pop-Location
}
