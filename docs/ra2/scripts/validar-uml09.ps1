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
        'docs/ra2/DIAGRAMA_PACOTES_ANEXO.puml',
        'docs/ra2/DIAGRAMA_COMPONENTES_ANEXO.puml',
        'docs/ra2/DIAGRAMA_IMPLANTACAO.puml',
        'docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml'
    )
    $log = [System.Collections.Generic.List[string]]::new()
    $log.Add('Baseline documental: 80a36c457ec4e46865ee456f388cb607297c3811')
    $log.Add('Data/hora: ' + [DateTimeOffset]::Now.ToString('o'))
    $log.Add('java -jar plantuml -charset UTF-8 -checkonly <seis fontes: quatro vistas e dois anexos>')
    & $Java -jar $PlantUml -charset UTF-8 -checkonly @sources *> (Join-Path $EvidenceDir 'plantuml-check.txt')
    $log.Add("checkonly exit: $LASTEXITCODE")
    if ($LASTEXITCODE -ne 0) { throw 'Erro de sintaxe PlantUML' }
    $log.Add('java -jar plantuml -charset UTF-8 -tsvg <seis fontes: quatro vistas e dois anexos>')
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

    # Elementos dos diagramas de pacotes devem existir no namespace Java indicado.
    $jdeps = Get-Content -LiteralPath 'docs/ra2/evidencias/entrega09/jdeps-pacotes.txt'
    foreach ($source in $sources | Where-Object { $_ -match 'DIAGRAMA_PACOTES' }) {
        $aliases = @{}
        $namespace = $null
        $classes = 0
        $interfaces = 0
        $lines = Get-Content -LiteralPath $source
        foreach ($line in $lines) {
            if ($line -match '^package "([^"]+)" as (\w+)') {
                $namespace = $Matches[1]
                $aliases[$Matches[2]] = $namespace
            } elseif ($line -match '^\s*(?:abstract )?(class|interface) (?:"([^"]+)"|(\w+))') {
                $kind = $Matches[1]
                $label = if ($Matches[2]) { $Matches[2] } else { $Matches[3] }
                if (-not $namespace) { throw "Elemento fora de pacote: $source -> $label" }
                $qualified = 'br.com.joinup.' + $namespace + '.' + $label
                $javaSource = 'src/main/java/' + $qualified.Replace('.', '/') + '.java'
                if (-not (Test-Path -LiteralPath $javaSource)) { throw "Classe inexistente: $qualified" }
                $expectedNamespace = $qualified.Substring(0, $qualified.LastIndexOf('.'))
                if ((Get-Content -Raw -LiteralPath $javaSource) -notmatch ([regex]::Escape('package ' + $expectedNamespace + ';'))) {
                    throw "Namespace divergente: $qualified"
                }
                if ($kind -eq 'class') { $classes++ } else { $interfaces++ }
            } elseif ($line -eq '}') { $namespace = $null }
        }
        $edges = 0
        foreach ($line in $lines) {
            if ($line -match '^(\w+) \.\.(?:\[[^\]]+\])?>\s+(\w+)') {
                $from = 'br.com.joinup.' + $aliases[$Matches[1]]
                $to = 'br.com.joinup.' + $aliases[$Matches[2]]
                $found = @($jdeps | Where-Object {
                    if ($_ -match '^(br\.com\.joinup\S*)\s+->\s+(br\.com\.joinup\S*)') {
                        ($Matches[1] -eq $from -or $Matches[1].StartsWith($from + '.')) -and
                        ($Matches[2] -eq $to -or $Matches[2].StartsWith($to + '.'))
                    }
                })
                if ($found.Count -eq 0) { throw "Dependência sem prova jdeps: $from -> $to" }
                $edges++
            }
        }
        $log.Add("$source : $classes classes + $interfaces interfaces em namespaces reais; $edges arestas confirmadas no jdeps")
    }
    # A auditoria aceitou estas relações; mudanças de título/layout não podem alterá-las.
    $cardinalPattern = '^\w+ "[0-9.*]+" -- "[0-9.*]+" \w+ : .+$'
    $original = @(git show '860492cc5b4eadee07adcb05dd9cd94c2e6cc189:docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml' | Where-Object { $_ -match $cardinalPattern })
    $current = @(Get-Content 'docs/ra2/DIAGRAMA_DOMINIO_CARDINALIDADES.puml' | Where-Object { $_ -match $cardinalPattern })
    if ($original.Count -ne 10 -or @(Compare-Object $original $current).Count -ne 0) { throw 'Cardinalidades divergentes da revisão auditada' }
    $log.Add('Dez associações/cardinalidades idênticas ao head auditado 860492c')

    # Cria o próprio log antes de conferir o link para ele; finaliza após as checagens.
    $log | Set-Content -Encoding utf8 (Join-Path $EvidenceDir 'validacao-documental.txt')
    # Verifica somente referências locais nos documentos ativos desta entrega.
    $documents = @('README.md', 'docs/ra2/RELATORIO_TECNICO_RA2.md',
        'docs/ra2/ROTEIRO_APRESENTACAO.md', 'docs/ra2/VALIDACAO_CONFORMIDADE_AULAS_UML.md',
        'docs/ra2/evidencias/entrega09/inspecao-visual.md')
    $visualRecord = Join-Path $EvidenceDir 'inspecao-visual.md'
    if ((Test-Path -LiteralPath $visualRecord) -and $visualRecord -notin $documents) { $documents += $visualRecord }
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
