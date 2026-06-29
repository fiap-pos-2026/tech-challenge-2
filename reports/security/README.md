# Relatório de Análise de Vulnerabilidades

## Ferramenta

[Trivy](https://trivy.dev/) v0.71.2 — scanner de vulnerabilidades open source mantido pela Aqua Security. Analisa imagens de container, pacotes do SO e dependências de linguagem contra bases de CVEs conhecidos (NVD, Alpine SecDB, GitHub Advisory, etc.).

## Escopo da análise

| Item | Valor |
|---|---|
| Alvo | `tech-challenge-1_core:latest` |
| Tipo | Imagem de container Docker |
| SO da imagem | Alpine Linux 3.23.5 |
| JRE | Eclipse Temurin 25.0.3+9 (OpenJDK 25) |
| Data da análise | 2026-06-29 |
| Trivy versão | 0.71.2 |

Foram analisados:
- Pacotes do SO Alpine (via Alpine SecDB)
- Dependências Java (arquivos `.jar` na camada de aplicação)

## Resultado

**Nenhuma vulnerabilidade encontrada.** O scanner não identificou CVEs nos pacotes do SO nem nas dependências Java empacotadas na imagem.

## Como reproduzir

```bash
# Gere a imagem
docker compose build core

# Execute o scan e gere os relatórios
trivy image --format json --output reports/security/security-report.json tech-challenge-1_core:latest
trivy image --format template --template "@contrib/html.tpl" --output reports/security/security-report.html tech-challenge-1_core:latest
```

## Arquivos

| Arquivo | Descrição |
|---|---|
| `security-report.json` | Resultado completo em JSON (SchemaVersion 2) |
| `security-report.html` | Relatório visual em HTML |
