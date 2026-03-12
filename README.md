# FrameDrop - Video Processing API

## Visão Geral

O **FrameDrop Video Processing** é um microserviço de processamento de vídeos desenvolvido como parte do ecossistema FrameDrop. Este serviço é responsável por consumir mensagens de uma fila SQS, baixar vídeos armazenados no S3, extrair frames utilizando FFmpeg/JavaCV, compactá-los em um arquivo ZIP, fazer upload de volta ao S3 e atualizar o status do processamento via API REST.

## Arquitetura

O projeto segue os princípios da **Hexagonal Architecture** (Arquitetura Hexagonal / Ports & Adapters), promovendo separação de responsabilidades e facilitando manutenção e testes.

### Camadas da Aplicação

```
┌─────────────────────────────────────────────────────────┐
│                     Adapters In                         │
│            (SQS Listener & DTOs)                        │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   Core - Ports In                       │
│              (Input Port Interfaces)                    │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│              Core - Application Layer                   │
│                   (Use Cases)                           │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                Core - Domain Layer                      │
│          (Entities, Enums & Business Rules)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                  Core - Ports Out                       │
│            (Output Port Interfaces)                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                    Adapters Out                         │
│      (S3, REST API, FFmpeg, Tika, Zip)                  │
└─────────────────────────────────────────────────────────┘
```

### Estrutura de Pacotes

```
com.framedrop.video_processing
├── VideoProcessingApplication.java
├── adapters
│   ├── config
│   │   ├── ListenerConfig.java           # ObjectMapper e queueUrl beans
│   │   ├── ProcessVideoConfig.java       # Wiring de use cases, adapters e RestClient
│   │   ├── S3Config.java                 # S3Client bean
│   │   └── SqsConfig.java               # SqsClient bean
│   ├── in
│   │   └── listener
│   │       ├── SqsVideoProcessingListener.java  # Polling SQS a cada 5s
│   │       └── dto
│   │           └── VideoMessageDTO.java          # Record: videoId, userId, email, filePath, status
│   └── out
│       ├── DownloadS3Adapter.java                # Download de vídeo do S3
│       ├── ExtractFramesAdapter.java             # Extração de frames com FFmpeg
│       ├── UpdateVideoStatusApiAdapter.java      # PATCH status via REST API
│       ├── UploadZipS3Adapter.java               # Upload de ZIP para o S3
│       ├── ValidateVideoAdapter.java             # Validação MIME type com Apache Tika
│       ├── ZipFramesAdapter.java                 # Compactação de frames em ZIP
│       └── dto
│           └── VideoStatusDTO.java               # Record: status
└── core
    ├── application
    │   └── usecases
    │       └── ProcessVideoUseCase.java          # Orquestração do pipeline completo
    └── domain
        ├── model
        │   ├── Video.java                        # Entidade de domínio com validação
        │   └── enums
        │       └── StatusProcess.java            # PENDING, PROCESSING, COMPLETED, FAILED
        └── ports
            ├── in
            │   └── ProcessVideoInputPort.java    # Interface de entrada
            └── out
                ├── DownloadVideoOutputPort.java
                ├── ExtractFramesOutputPort.java
                ├── UpdateVideoStatusOutputPort.java
                ├── UploadZipOutputPort.java
                ├── ValidateVideoOutputPort.java
                └── ZipFramesOutputPort.java
```

## Tecnologias Utilizadas

### Backend Framework
- **Java 25**
- **Spring Boot 4.0.3**
- **Spring Scheduling** - Polling agendado da fila SQS
- **Spring Actuator** - Health checks e métricas
- **Lombok** - Redução de boilerplate

### AWS Services
- **Amazon S3** - Armazenamento de vídeos e ZIPs processados
- **Amazon SQS** - Fila de mensagens para processamento assíncrono
- **AWS SDK 2.20.21** - SDK para integração com serviços AWS

### Processamento de Vídeo
- **JavaCV 1.5.10** - Wrapper Java para FFmpeg
- **FFmpeg** - Extração de frames dos vídeos
- **Apache Tika 3.2.3** - Detecção e validação de MIME types

### Testes
- **JUnit 5** - Framework de testes
- **Mockito** - Mocks e testes unitários
- **@TempDir** - Diretórios temporários para testes de I/O

### Build & Deploy
- **Maven** - Gerenciador de dependências
- **Docker** - Containerização (multi-stage build)
- **Terraform** - Infrastructure as Code (AWS)

## Funcionalidades Principais

### 1. Pipeline de Processamento de Vídeo

O pipeline completo é orquestrado pelo `ProcessVideoUseCase` e segue os seguintes passos:

1. Cria objeto de domínio `Video` (valida ID, usuário, caminho, extensão)
2. Atualiza status para `PROCESSING` via API REST
3. Cria diretórios temporários: `{baseDir}/{videoId}/video/`, `frames/`, `zip/`
4. **Download** do vídeo a partir do bucket S3
5. **Validação** do MIME type do vídeo (Apache Tika)
6. **Extração de frames** usando FFmpegFrameGrabber → arquivos PNG (`frame_000000.png`, ...)
7. **Compactação** de todos os frames em `{baseName}_frames.zip`
8. **Upload** do ZIP para o S3 em `processed/{userId}/{videoId}_{zipFileName}`
9. Atualiza status para `COMPLETED`
10. Em caso de erro → status `FAILED`, relança exceção
11. **Finally** → limpeza de todos os diretórios temporários

### 2. Consumo de Mensagens SQS

#### SQS Listener
- **Polling**: `@Scheduled(fixedDelay = 5000)` — a cada 5 segundos
- **Batch size**: até 5 mensagens por polling, 10s de long-poll wait
- **Formato da mensagem (DTO)**:
```json
{
  "videoId": "string",
  "userId": "string",
  "email": "string",
  "filePath": "string",
  "status": "string"
}
```
- Em caso de sucesso: deleta a mensagem da fila
- Em caso de falha: loga o erro, a mensagem permanece na fila (retry/DLQ)

### 3. Atualização de Status via API

#### Atualizar Status do Vídeo
- **Endpoint chamado**: `PATCH /api/videos/{videoId}`
- **Descrição**: Atualiza o status do processamento no serviço de upload
- **Body**: `{ "status": "PROCESSING" | "COMPLETED" | "FAILED" }`

### 4. Integrações Externas

#### Amazon S3
- Download de vídeos originais do bucket de upload
- Upload de ZIPs processados para o mesmo bucket

#### Amazon SQS
- Consumo de mensagens com informações de vídeos a processar
- Deleção de mensagens após processamento bem-sucedido

#### FrameDrop Upload API
- Atualização de status do vídeo via REST (RestClient)

## Casos de Uso Implementados

| Use Case | Descrição |
|----------|-----------|
| `ProcessVideoUseCase` | Orquestração completa do pipeline de processamento de vídeo |

## Ports (Interfaces)

### Input Port

| Port | Método |
|------|--------|
| `ProcessVideoInputPort` | `void processVideo(String videoId, String userId, String videoPath, String status)` |

### Output Ports

| Port | Método |
|------|--------|
| `DownloadVideoOutputPort` | `File downloadVideo(String videoPath, Path destinationDir)` |
| `ValidateVideoOutputPort` | `boolean isValidFormatVideo(File file)` |
| `ExtractFramesOutputPort` | `List<File> extractFrames(File videoFile, Path framesDir)` |
| `ZipFramesOutputPort` | `File zipFrames(List<File> frames, Path zipDir, String zipFileName)` |
| `UploadZipOutputPort` | `void uploadZip(String destinationPath, File zipFile)` |
| `UpdateVideoStatusOutputPort` | `void updateStatus(String videoId, String status)` |

## Modelo de Dados

### Entidade: Video

```java
{
  "videoId": "String",
  "userId": "String",
  "videoPath": "String",
  "fileName": "String",
  "fileExtension": "String",
  "statusProcess": "StatusProcess"
}
```

### Extensões Permitidas
`.mp4`, `.mkv`, `.webm`, `.mov`, `.avi`

### Enum: StatusProcess

| Status | Descrição |
|--------|-----------|
| `PENDING` | Aguardando processamento |
| `PROCESSING` | Em processamento |
| `COMPLETED` | Processamento concluído |
| `FAILED` | Falha no processamento |

## Padrões de Status de Processamento

```
PENDING → PROCESSING → COMPLETED
                    └→ FAILED
```

## Configuração

### Variáveis de Ambiente

```properties
# AWS S3
S3_BUCKET_NAME=<bucket_name>

# AWS SQS
SQS_VIDEO_PROCESSING_QUEUE_URL=<queue_url>

# Diretório de processamento
VIDEO_PROCESSING_BASE_DIR=<base_dir>   # default: /tmp/framedrop-video-processing

# API de Upload
UPLOAD_API_BASE_URL=<url>              # URL do serviço framedrop-upload
```

### application.properties

```properties
spring.application.name=video-processing
server.port=8081
aws.s3.bucket-name=${S3_BUCKET_NAME:framedrop-upload-tst}
aws.sqs.video-processing-queue-url=${SQS_VIDEO_PROCESSING_QUEUE_URL:...}
aws.region=us-east-1
video.processing.base-dir=${VIDEO_PROCESSING_BASE_DIR:/tmp/framedrop-video-processing}
framedrop.upload-api.base-url=${UPLOAD_API_BASE_URL:http://...}
```

## Docker

O projeto inclui um `Dockerfile` multi-stage para containerização da aplicação.

### Build da Imagem
```bash
cd video-processing
docker build -t framedrop-video-processing:latest .
```

### Executar Container
```bash
docker run -p 8081:8081 \
  -e S3_BUCKET_NAME=<bucket> \
  -e SQS_VIDEO_PROCESSING_QUEUE_URL=<queue_url> \
  -e UPLOAD_API_BASE_URL=<url> \
  -e AWS_ACCESS_KEY_ID=<key> \
  -e AWS_SECRET_ACCESS_KEY=<secret> \
  framedrop-video-processing:latest
```

### Detalhes do Dockerfile
- **Build stage**: `maven:3.9-eclipse-temurin-25` — cache de dependências + `mvn clean package -DskipTests`
- **Runtime stage**: `eclipse-temurin:25-jre`
  - Instala `ffmpeg` (necessário para JavaCV) e `wget` (para healthcheck)
  - Usuário não-root `spring:spring`
  - Diretório de processamento: `/tmp/framedrop-video-processing`
  - Porta: `8081`
  - Healthcheck: `wget http://localhost:8081/actuator/health` (intervalo 30s, start period 40s)
  - JVM flags: `UseContainerSupport`, `MaxRAMPercentage=75%`

## Infraestrutura (Terraform)

O diretório `/terraform` contém a infraestrutura como código para provisionar recursos na AWS.

### Recursos Criados
- **Security Group** (`framedrop-video-processing-ecs-sg`) — ingress 8081 do ALB SG, todo egress permitido
- **ECS Task Definition** (Fargate) — 1024 CPU, 2048 MB memória, logs no CloudWatch
- **ECS Service** — 1 task desejada, Fargate, IP público, integração com ALB target group, range de deploy 50-200%
- **CloudWatch Log Group** — retenção de 1 dia

### Data Sources Referenciados
- ECS Cluster: `framedrop-ecs-cluster`
- ECR Repository: `framedrop-video-processing-app`
- VPC e Subnets padrão
- ALB Security Group: `framedrop-alb-sg`
- Target Group: `framedrop-video-processing-tg`
- SSM Parameters: `/framedrop/infra/bucket_name`, `/framedrop/infra/sqs_video_processing_queue_url`, `/framedrop/infra/upload_api_base_url`

### Secrets Injetados via SSM
- `S3_BUCKET_NAME`
- `SQS_VIDEO_PROCESSING_QUEUE_URL`
- `UPLOAD_API_BASE_URL`

### Deploy da Infraestrutura
```bash
cd terraform
terraform init
terraform plan
terraform apply
```

## Testes

### Executar Testes Unitários
```bash
cd video-processing
mvn test
```

### Estrutura de Testes

- **Unit Tests**: Testes unitários de todos os adapters, use cases e modelos de domínio
- **Mocking**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- **I/O Tests**: `@TempDir` para testes baseados em filesystem
- **Parameterized Tests**: `@ParameterizedTest` + `@ValueSource` para validação de extensões

### Classes de Teste (12 classes)

| Classe | Descrição |
|--------|-----------|
| `VideoProcessingApplicationTests` | Teste de contexto da aplicação |
| `SqsVideoProcessingListenerTest` | Processamento de mensagens, deleção, tratamento de erros |
| `VideoMessageDTOTest` | Serialização/deserialização do DTO |
| `DownloadS3AdapterTest` | Download de vídeos do S3 |
| `UpdateVideoStatusApiAdapterTest` | Atualização de status via REST |
| `UploadZipS3AdapterTest` | Upload de ZIP para o S3 |
| `ValidateVideoAdapterTest` | Validação de formato de vídeo |
| `ZipFramesAdapterTest` | Compactação de frames em ZIP |
| `VideoStatusDTOTest` | Serialização/deserialização do DTO |
| `ProcessVideoUseCaseTest` | Cenário happy path + 5 cenários de falha |
| `VideoTest` | Validação de domínio (12+ edge cases) |
| `StatusProcessTest` | Testes do enum de status |

## Monitoramento

### Spring Actuator

A aplicação expõe endpoints de monitoramento:

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`

## Padrões de Código

- **Hexagonal Architecture (Ports & Adapters)**: Separação clara entre core e infraestrutura
- **SOLID Principles**: Código modular e testável
- **Dependency Injection**: Gerenciado pelo Spring
- **DTOs**: Separação entre camadas de entrada/saída
- **Port Pattern**: Abstrações para integrações externas (S3, SQS, API REST)
- **Domain Validation**: Validações de negócio encapsuladas na entidade de domínio

## Como Executar Localmente

### Pré-requisitos
- Java 25
- Maven 3.x
- Docker (opcional)
- FFmpeg instalado localmente
- AWS CLI configurado (para S3 e SQS)

### Passos

1. **Clone o repositório**
```bash
git clone https://github.com/daniel-dev-vs/framedrop-video-processing.git
cd framedrop-video-processing
```

2. **Configure as variáveis de ambiente**
```bash
export S3_BUCKET_NAME=your_bucket
export SQS_VIDEO_PROCESSING_QUEUE_URL=your_queue_url
export UPLOAD_API_BASE_URL=http://localhost:8080
export VIDEO_PROCESSING_BASE_DIR=/tmp/framedrop-video-processing
```

3. **Build do projeto**
```bash
cd video-processing
mvn clean install
```

4. **Execute a aplicação**
```bash
mvn spring-boot:run
```

5. **Verifique o health check**
```
http://localhost:8081/actuator/health
```

## Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## Licença

Este projeto faz parte do ecossistema FrameDrop.

## Autores

Desenvolvido por **Equipe Framedrop**

---

**Versão**: 0.0.1-SNAPSHOT  
**Última atualização**: Março 2026
