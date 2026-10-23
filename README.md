# AI Playground: LLM vs. Tool-Using Agent

This is a small educational Java 21 / Spring Boot 4 / Spring AI project to show the distinction between an LLM response and a model that can use an application tool.

## Setup

Requirements: JDK 21 and Maven 3.9+. Choose local Ollama (no paid API required) or OpenAI (paid API credits required). Both options use the same endpoints and tool code.

### Local Ollama (recommended for learning)

On Windows, install [Ollama](https://ollama.com/download/windows) and open a new terminal. Ollama runs in the background at `http://localhost:11434`; Docker is optional. On Linux, start `ollama serve` if the service is not already running.

Download the tool-capable model, then start this project with the `ollama` Spring profile:

```powershell
ollama pull qwen3:4b
mvn spring-boot:run "-Dspring-boot.run.profiles=ollama"
```

In IntelliJ, set **Active profiles** to `ollama` in the Spring Boot run configuration, or add `--spring.profiles.active=ollama` to program arguments. No OpenAI key or API credits are needed for this profile. Stop the previous application before starting another instance on port 8080.

The model download is approximately 2.5 GB; inference also needs available RAM/VRAM. CPU execution is supported but may be slow, especially on the first request. Tool selection and answer quality depend on the model; check the `TOOL EXECUTED` log for `/demo/agent`.

Optional overrides (PowerShell):

```powershell
$env:OLLAMA_MODEL = 'qwen3:4b'
$env:OLLAMA_BASE_URL = 'http://localhost:11434/v1'
```

Pull the selected model before using it. `OLLAMA_BASE_URL` must include `/v1`. The profile reuses Spring AI's existing OpenAI client through [Ollama's OpenAI-compatible API](https://docs.ollama.com/api/openai-compatibility); it sends requests to local Ollama. The `ollama` API key value is a dummy value ignored by the local server.

### Optional: run Ollama in Docker

Use either native Ollama or Docker to avoid a port conflict. With Docker Desktop running, these commands start a CPU-based Ollama server and keep downloaded models in a named volume:

```powershell
docker run -d --name ai-playground-ollama -p 127.0.0.1:11434:11434 -v ai-playground-ollama:/root/.ollama ollama/ollama
docker exec ai-playground-ollama ollama pull qwen3:4b
mvn spring-boot:run "-Dspring-boot.run.profiles=ollama"
```

The Java application still runs on the host. For a compatible NVIDIA GPU and Docker GPU setup, add `--gpus=all` to `docker run`; see [Ollama's Docker documentation](https://docs.ollama.com/docker). Native Windows Ollama is usually simpler for GPU use.

To stop and later restart the existing container:

```powershell
docker stop ai-playground-ollama
docker start ai-playground-ollama
```

If requests fail, check that Ollama is running and the configured model appears in `ollama list` (or `docker exec ai-playground-ollama ollama list`). A missing model must be pulled; a refused connection means the server or URL needs checking.

### OpenAI

Without the `ollama` profile, the project uses OpenAI. Set an API key associated with an organization/project that has available API credits.

PowerShell:

```powershell
$env:OPENAI_API_KEY = 'your_api_key'
mvn spring-boot:run
```

Linux/macOS:

```bash
export OPENAI_API_KEY='your_api_key'
mvn spring-boot:run
```

Do not commit or share your API key. ChatGPT subscriptions do not include API billing. A 429 response saying there are no credits requires adding API credits; retrying cannot restore the balance. Use the `ollama` profile to run locally instead.

## Compare the same question

Plain LLM (no business data provided):

```bash
curl 'http://localhost:8080/demo/llm?memberId=M100'
```

Expected behavior: says the spending for M100 cannot be verified. No `TOOL EXECUTED` log appears.

Tool-using mode:

```bash
curl 'http://localhost:8080/demo/agent?memberId=M100'
```

Expected behavior: a tool call fetches M100's totalSpent=12000 from the in-memory Map; the model responds that GOLD's >=10000 threshold is met. In application logs, look for `TOOL EXECUTED: getMember(M100)`.

A second tool-using example:

```bash
curl 'http://localhost:8080/demo/agent?memberId=M200'
```

Expected behavior: member M200 has totalSpent=3000, so is not eligible.

## What each component does

- `DemoController.llmOnly`: calls the LLM without granting access to application tools.
- `DemoController.withTool`: passes the same LLM prompt but enables `MemberTools`.
- `MemberTools`: simulates an authoritative business data source; the Java method performs the lookup, not the LLM.
- Spring AI: transports the model's tool-call request to the Java method, executes it, and supplies the result to the model.

This deliberately demonstrates **one tool call**, not a full autonomous planning framework. It has no vector DB, no RAG, no MCP, no persistent memory, and no model training.

**Engineering caveat:** Eligibility is intentionally described by the model to show tool use. In production, an exact spending-threshold policy should be implemented with deterministic Java rules, and any LLM explanation should be separately checked.

## Debugging questions

1. Does the LLM-only endpoint ever access the in-memory Map? It shouldn't.
2. Who executes `getMember`? Spring AI's tool executor calls your Java code; the model only requests a tool.
3. After changing M100's spending and restarting, did you retrain the LLM? No.
4. Is `/agent` guaranteed to call the tool? Model tool selection is dynamic; instructions increase reliability, and the log is the evidence of whether it actually happened.

Real-model responses require a running model server and must be checked locally; model-driven tool selection is not guaranteed.

