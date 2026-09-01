# Relatório de Análise de Vulnerabilidades

## Ferramenta

[Trivy](https://trivy.dev/) v0.74.0 — scanner de vulnerabilidades open source mantido pela Aqua Security. Analisa imagens de container, pacotes do SO e dependências de linguagem contra bases de CVEs conhecidos (NVD, Alpine SecDB, GitHub Advisory, etc.).

## Escopo da análise

| Item | Valor |
|---|---|
| Alvo | `tech-challenge-core:local` |
| Tipo | Imagem de container Docker |
| SO da imagem | Alpine Linux 3.24.1 |
| JRE | Eclipse Temurin 25 (OpenJDK 25) — `eclipse-temurin:25-jre-alpine` |
| Data da análise | 2026-09-01 |
| Trivy versão | 0.74.0 |

Foram analisados:
- Pacotes do SO Alpine (via Alpine SecDB)
- Dependências Java (arquivos `.jar` na camada de aplicação)

O scan roda sem filtro de severidade (`--scanners vuln`), cobrindo `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` e `UNKNOWN`.

## Resultado

**Nenhuma vulnerabilidade encontrada.** O scanner não identificou CVEs nos pacotes do SO Alpine nem nas dependências Java empacotadas na imagem.

## Correções aplicadas

O scan inicial acusava 5 CVEs do SO Alpine (severidade HIGH) e vulnerabilidades em dependências transitivas Java. As correções abaixo zeraram o relatório:

| Camada | Correção | Onde |
|---|---|---|
| SO Alpine | `apk upgrade --no-cache` no estágio de runtime — sobe todos os pacotes do sistema para a última revisão corrigida | `core/Dockerfile` |
| Driver PostgreSQL | `org.postgresql:postgresql` fixado em `42.7.12` (CVE-2026-54291) | `core/build.gradle.kts` |
| Jackson 2 | `com.fasterxml.jackson.core:jackson-databind` fixado em `2.22.1` | `core/build.gradle.kts` |
| Jackson 3 | `tools.jackson.core:jackson-core` e `jackson-databind` fixados em `3.1.5` | `core/build.gradle.kts` |
| Log4j | `log4j-api` e `log4j-to-slf4j` fixados em `2.25.5` | `core/build.gradle.kts` |
| Netty | Conjunto `io.netty:*` fixado em `4.2.16.Final` (lockstep) | `core/build.gradle.kts` |

Os overrides de versão Java são feitos no bloco `dependencyManagement` do `core/build.gradle.kts` — são bumps de patch de CVEs já corrigidos upstream, à frente do que o BOM do Spring Boot ainda traz.

## Como reproduzir

```bash
# Gere a imagem (BuildKit / buildx é necessário pelos cache mounts do Dockerfile)
docker buildx build -f core/Dockerfile -t tech-challenge-core:local --load .

# Execute o scan
trivy image --scanners vuln --format json \
  --output reports/security/security-report.json tech-challenge-core:local
```

## Arquivos

| Arquivo | Descrição |
|---|---|
| `security-report.json` | Resultado completo do scan em JSON (SchemaVersion 2) |
| `security-report.html` | Resumo visual: metadados da imagem e inventário de pacotes analisados (SO e Java) |
