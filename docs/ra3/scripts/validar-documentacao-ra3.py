"""Conferência documental estática; não substitui PlantUML nem testes MySQL.

Executar da raiz: python docs/ra3/scripts/validar-documentacao-ra3.py
Usa somente a biblioteca padrão e não conecta ao banco.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
DOC = ROOT / 'docs/ra3'
MODEL = ROOT / 'src/main/java/br/com/joinup/ra3/modelo'
CLASSES = {'GeneroMusical', 'Organizador', 'Participante', 'Evento',
           'TipoIngresso', 'Comunidade', 'Postagem'}
RELATIONS = [
    ('Organizador', 'Evento', 'organizador', 'id_organizador', 'ManyToOne', '0..*'),
    ('GeneroMusical', 'Evento', 'generoMusical', 'id_genero_musical', 'ManyToOne', '0..*'),
    ('Evento', 'TipoIngresso', 'evento', 'id_evento', 'ManyToOne', '0..*'),
    ('Evento', 'Comunidade', 'evento', 'id_evento', 'OneToOne', '0..1'),
    ('Comunidade', 'Postagem', 'comunidade', 'id_comunidade', 'ManyToOne', '0..*'),
    ('Participante', 'Postagem', 'participante', 'id_participante', 'ManyToOne', '0..*'),
]

def require(condition, message):
    if not condition:
        raise SystemExit('FALHA: ' + message)

def read(path):
    return path.read_text(encoding='utf-8-sig')

puml = read(DOC / 'DIAGRAMA_CLASSES_RA3.puml')
blocks = dict(re.findall(r'class (\w+) <<Entity>> \{\n(.*?)^  \}', puml, re.M | re.S))
require(set(blocks) == CLASSES, 'classes UML diferem das sete entidades')
sources = {name: read(MODEL / (name + '.java')) for name in CLASSES}
actual_entities = {f.stem for f in MODEL.glob('*.java') if re.search(r'^@Entity\b', read(f), re.M)}
require(actual_entities == CLASSES, '@Entity fora do conjunto esperado')
tree = ET.parse(ROOT / 'src/main/resources/META-INF/persistence.xml')
ns = {'j': 'https://jakarta.ee/xml/ns/persistence'}
listed = [x.text for x in tree.findall('.//j:class', ns)]
require(len(listed) == 7 and set(listed) == {'br.com.joinup.ra3.modelo.' + n for n in CLASSES},
        'persistence.xml deve listar exatamente as sete entidades')
attrs = methods = 0
for name, body in blocks.items():
    source = sources[name]
    fields = {field: kind for kind, field in re.findall(r'private ([\w<>]+) (\w+)\s*(?:[;=])', source)}
    shown = dict(re.findall(r'^\s*- (\w+): ([\w<>]+)', body, re.M))
    require(shown == fields, f'atributos/tipos divergentes em {name}: {shown} / {fields}')
    require('- id: Integer {PK}' in body and '@GeneratedValue(strategy = GenerationType.IDENTITY)' in source,
            'PK/IDENTITY divergente em ' + name)
    for method, args, result in re.findall(r'^\s*\+ (\w+)\(([^)]*)\): (\w+)', body, re.M):
        argtypes = [v.strip() for v in args.split(',') if v.strip()]
        candidates = re.findall(r'public ' + result + r' ' + method + r'\(([^)]*)\)', source)
        require(any([v.strip().split()[0] for v in c.split(',') if v.strip()] == argtypes for c in candidates),
                f'método inexistente: {name}.{method}({args})')
        methods += 1
    attrs += len(shown)
print(f'ENTIDADES_OK classes=7 atributos={attrs} mutadores={methods} PK=IDENTITY XML=7')

sql = read(ROOT / 'src/main/resources/db/ra3/schema_ra3.sql')
table_blocks = dict(re.findall(r'CREATE TABLE IF NOT EXISTS `(\w+)` \((.*?)\) ENGINE = InnoDB;', sql, re.S))
tables = {n: re.search(r'@Table\(name = "(\w+)"', s).group(1) for n, s in sources.items()}
require(len(table_blocks) == 7 and set(table_blocks) == set(tables.values()), 'tabelas SQL divergentes')
uml_relations = re.findall(r'^\s*(\w+) "1" -- "([^"]+)" (\w+) :', puml, re.M)
require(set(uml_relations) == {(p, mult, child) for p, child, _, _, _, mult in RELATIONS}
        and len(uml_relations) == 6, 'associações/multiplicidades UML divergentes')
for parent, child, field, fk, annotation, multiplicity in RELATIONS:
    pattern = (r'@' + annotation + r'\(([^)]*)\)\s*@JoinColumn\(([^)]*)\)\s*private '
               + parent + ' ' + field + r';')
    match = re.search(pattern, sources[child])
    require(match is not None, f'associação ausente: {child}.{field}')
    require('optional = false' in match[1] and 'nullable = false' in match[2]
            and f'name = "{fk}"' in match[2], f'obrigatoriedade/FK divergente: {child}.{field}')
    parent_pk = re.search(r'@Column\(name = "(\w+)", nullable = false\)\s*private Integer id;', sources[parent])[1]
    require(re.search(r'FOREIGN KEY \(`' + fk + r'`\)\s*REFERENCES `' + tables[parent]
                      + r'` \(`' + parent_pk + r'`\)', table_blocks[tables[child]]), 'FK SQL divergente')
    if annotation == 'OneToOne':
        require('unique = true' in match[2] and re.search(r'UNIQUE INDEX `uk_comunidade_evento` \(`id_evento`',
                table_blocks[tables[child]]), 'UNIQUE comunidade/evento ausente')
require('@OneToMany(mappedBy = "evento", fetch = FetchType.EAGER)' in sources['Evento'], 'inverso mappedBy ausente')
require(not re.search(r'\*--|o--|<\|--|--\|>', puml), 'composição/agregação/herança inesperada')
print('RELACOES_OK associacoes=6 ManyToOne=5 OneToOne=1 FKs=6 UNIQUE_comunidade=sim mappedBy=evento')

svgfile = DOC / 'DIAGRAMA_CLASSES_RA3.svg'
svg = ET.parse(svgfile).getroot()
require(svg.tag == '{http://www.w3.org/2000/svg}svg', 'raiz SVG inválida')
texts = ' '.join(svg.itertext())
require(not re.search(r'Syntax Error|An error has occurred', texts), 'SVG contém erro PlantUML')
require(all(n in texts for n in CLASSES), 'SVG não contém todas as entidades')
print(f'SVG_XML_OK largura={svg.get("width")} altura={svg.get("height")} classes=7')

files = ['RELATORIO_ACADEMICO_RA3.md', 'ROTEIRO_APRESENTACAO_RA3.md',
         'CHECKLIST_ENTREGA_RA3.md', 'VALIDACAO_ENTREGA_04.md', 'EXECUCAO_RA3.md']
links = 0
for name in files:
    text = read(DOC / name)
    for target in re.findall(r'\]\(([^)]+)\)', text):
        if re.match(r'https?://|#', target):
            continue
        path = (DOC / target.split('#')[0]).resolve()
        require(path.is_file(), f'link local ausente: {name} -> {target}')
        links += 1
report = read(DOC / files[0])
require(re.findall(r'^## (.+)$', report, re.M) == ['1. Escopo e propósito do sistema',
        '2. Reflexões da equipe sobre o desenvolvimento'], 'relatório deve ter somente dois tópicos')
print(f'DOCUMENTOS_OK links_locais={links} topicos_relatorio=2')
