---
name: relatorio-agentlens
description: Roda o AgentLens (@hugofusinato/agentlens) neste repositório e entrega um relatório em português sobre como o repo está configurado para agentes de IA e como o agente enxerga o código (arquivos de contexto, custo em tokens, referências quebradas, saúde do CLAUDE.md e lacunas). Use sempre que o usuário pedir o relatório do agentlens, uma análise de contexto/tokens do CLAUDE.md, ou perguntar "como o agente vê meu repo/código", "como meu repo está configurado para o Claude", ou quiser auditar a configuração de agentes.
---

# Relatório AgentLens

O AgentLens escaneia o repo atrás de arquivos de instrução de agentes (`CLAUDE.md`, `AGENTS.md`, `.cursorrules`, `.github/copilot-instructions.md`, `.windsurfrules`, `CONVENTIONS.md`, `DEVIN.md`...), segue os caminhos que eles citam e estima o custo em tokens (≈ caracteres / 4). O objetivo desta skill não é só rodar a ferramenta: é **interpretar** o resultado e dizer ao usuário como o agente enxerga o projeto.

## Passos

1. **Rode o scan sem sujar o repo.** Use `--stdout` e grave o JSON num arquivo temporário (por padrão a CLI cria `<repo>.html/.json` no diretório atual e abre o browser). Escolha o executável nesta ordem de preferência, na raiz do repo, mesmo que o usuário tenha o pacote instalado globalmente:

   ```bash
   OUT="$(mktemp -t agentlens).json"
   CLI=node_modules/@hugofusinato/agentlens/cli.js
   if [ -f "$CLI" ]; then
     node "$CLI" --path . --stdout > "$OUT"                    # 1º: node direto no CLI local
   elif command -v agentlens >/dev/null; then
     agentlens --path . --stdout > "$OUT"                      # 2º: instalação global
   else
     npx --yes @hugofusinato/agentlens --path . --stdout > "$OUT"   # 3º: npx
   fi
   ```

   - O node direto vem primeiro porque é rápido, não depende de rede e usa a versão fixada no `package.json`.
   - Se falhar (Node < 18, rede no `npx`), avise o usuário e diga qual executável foi tentado.
   - Se o usuário pedir também o relatório visual, rode de novo com `--out <nome> --no-open` e informe os arquivos gerados. Eles são artefatos: não os adicione ao git sem pedir.
2. **Leia o JSON** (`repos[0]`). Campos úteis: `foundByTool` (arquivo de instrução por ferramenta), `uniqueRefs` (arquivos citados: `path`, `type`, `found`, `tokens`, `fromTools`), `agentTok` (tokens dos arquivos de instrução), `refTok` (tokens das referências), `totalContextTokens`, `noFiles`. Resuma com `node -e` ou `jq` em vez de despejar `content` no terminal.
3. **Complete com o que o AgentLens não vê.** Ele só olha os arquivos de instrução e suas referências. Confira o resto da configuração do agente no repo (em paralelo): `.claude/` (skills, agents, commands, `settings*.json`), `.mcp.json`, `.sdkmanrc`/`Dockerfile`/`gradlew` que o CLAUDE.md cita, e `git status` para arquivos soltos.
4. **Valide o CLAUDE.md contra o código.** Abra o `CLAUDE.md` e verifique por amostragem se o que ele afirma bate com o repo (comandos existem, pastas/pacotes citados existem, versões batem com `build.gradle`). Contexto desatualizado é pior que contexto ausente, pois o agente age com confiança no que leu.
5. **Escreva o relatório** no formato abaixo, no chat. Não crie arquivo de relatório a menos que o usuário peça.

## Formato do relatório (em português)

Profundidade: objetiva. Recomendações sim, mas curtas: frases de uma linha, no máximo 5 itens por lista, sem parágrafos longos. Se um ponto não rende ação, não o inclua.

### 1. Resumo de uso (sempre primeiro)
Bloco curto, para quem lê só isso:
- **Uso:** total de tokens de contexto (`agentTok` + `refTok`), a faixa de saúde (ideal ≤ 2.000, atenção até 5.000, alerta acima, máximo 20.000 para `CLAUDE.md`; Codex/`AGENTS.md`: 3.000 / 6.000 / 8.000) e quem pesa mais.
- **Como melhorar:** até 3 ações de maior impacto.
- **O que remover do contexto:** o que sai do `CLAUDE.md` ou das referências, cada item com o motivo e a economia estimada em tokens (ex.: segredos, conteúdo que o agente descobre lendo o código, referências caras lidas só sob demanda como `schema.sql`, trechos desatualizados, duplicações).

### 2. Configuração do repositório
Tabela enxuta: arquivo, ferramenta, tokens, estado. Inclua os arquivos detectados pelo AgentLens **e** os que ele não cobre (`.claude/skills`, `.mcp.json`...).

### 3. Como o agente vê o código
3 a 6 bullets com o que ele aprende do `CLAUDE.md` (stack, comandos, convenções, armadilhas) e os **pontos cegos**: o que existe no código e não está no contexto, e o que o contexto promete mas o código não tem. Fluxograma mermaid só se ajudar de verdade.

### 4. Problemas encontrados
Lista curta e priorizada (alto / médio / baixo): referências com `found: false` (confirme no disco se é falso positivo, pois a ferramenta resolve caminhos a partir da raiz), instruções desatualizadas, credenciais expostas, arquivos soltos que poluem o repo.

### 5. Próximos passos
Até 5 ações concretas, em ordem de impacto. Não aplique nenhuma sem o usuário pedir.

## Cuidados

- Os tokens são estimativa (`chars / 4`), não contagem real de tokenizer. Diga isso uma vez.
- Se `noFiles` for `true`, não há arquivo de instrução: relate isso e recomende criar um (`/init`), sem inventar números.
- Baseie tudo no JSON e no código reais; não suponha o que o agente "sente". Se uma conclusão for inferência, diga.
- Cite credenciais encontradas pelo nome do arquivo/linha, sem repetir o valor no relatório.
